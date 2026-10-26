package dev.twitchpatches.patches.twitch.chat

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import dev.twitchpatches.patches.twitch.settings.settingsPatch
import dev.twitchpatches.patches.twitch.shared.*

// Adapted from arandomhooman/hoomans-morphe-patches (GPL-3.0), Show deleted messages.
private const val POLICY = "Ldev/twitchpatches/extension/chat/DeletedMessageSettings;"
private const val DISPATCHER = "Ltv/twitch/android/core/mvp/viewdelegate/EventDispatcher;"

@Suppress("unused")
val showDeletedMessagesPatch = bytecodePatch(
    name = "Show deleted messages",
    description = "Turns deleted or moderated chat messages into a tappable spoiler that reveals the original text.",
    default = true,
) {
    compatibleWith(TwitchTarget.compatibility)
    dependsOn(settingsPatch)
    execute {
        // The ClickableSpan on a deleted message; its boolean field gates both the reveal and the clickable style.
        val spans = mutableListOf<Method>()
        classDefForEach { type ->
            if (type.superclass == "Landroid/text/style/ClickableSpan;") spans.addAll(type.methods.filter {
                it.name == "<init>" && it.parameterTypes.map { p -> p.toString() } ==
                    listOf("Ljava/lang/String;", "Landroid/text/SpannedString;", "Z", DISPATCHER)
            })
        }
        val constructor = spans.uniqueHook("deleted message span constructor")
        val span = mutableClassDefBy(constructor.definingClass)
        val access = span.fields.filter { it.type == "Z" }.uniqueHook("deleted message access flag")
        val original = span.fields.filter { it.type == "Landroid/text/SpannedString;" }.uniqueHook("deleted message text")

        val init = span.methods.single { it.reference == constructor.reference }
        val end = init.code().lastIndex
        if (init.getInstruction(end).opcode != Opcode.RETURN_VOID)
            throw PatchException("Twitch deleted messages: span constructor no longer ends with return-void.")
        init.addInstructionsWithLabels(end, """
            invoke-static {}, $POLICY->enabled()Z
            move-result p3
            if-eqz p3, :stock_return
            iput-boolean p3, p0, $access
        """, ExternalLabel("stock_return", init.getInstruction(end)))

        // The formatter rebuilds the span with the row's live dispatcher only when the input has none. Twitch now
        // passes a placeholder whose span has a null dispatcher, so recover the original text and take the rebuild path.
        val formatters = mutableListOf<Method>()
        classDefForEach { type -> formatters.addAll(type.methods.filter {
            AccessFlags.STATIC.isSet(it.accessFlags) && it.returnType == "Landroid/text/Spanned;" &&
                it.parameterTypes.map { p -> p.toString() } == listOf("Ljava/lang/String;",
                    "Landroid/text/SpannedString;", "Landroid/content/Context;", DISPATCHER, "Z")
        }) }
        val formatterHook = formatters.uniqueHook("deleted message formatter")
        val formatter = mutableClassDefBy(formatterHook.definingClass).methods.single { it.reference == formatterHook.reference }
        val code = formatter.code()
        val getSpans = code.indexOfFirst {
            val method = (it as? ReferenceInstruction)?.reference as? MethodReference
            method?.definingClass == "Landroid/text/SpannedString;" && method.name == "getSpans"
        }
        val length = code.indices.firstOrNull { it > getSpans && getSpans >= 0 && code[it].opcode == Opcode.ARRAY_LENGTH }
            ?: throw PatchException("Twitch deleted messages: existing-span array check was not found.")
        val scratch = (code.take(length).lastOrNull {
            it.opcode == Opcode.CONST_CLASS && ((it as ReferenceInstruction).reference as TypeReference).type == span.type
        } as? OneRegisterInstruction)?.registerA
            ?: throw PatchException("Twitch deleted messages: span class register was not found.")
        val array = formatter.getInstruction<TwoRegisterInstruction>(length).registerB
        if (array == scratch) throw PatchException("Twitch deleted messages: formatter registers now overlap.")
        formatter.addInstructionsWithLabels(length, """
            invoke-static {}, $POLICY->enabled()Z
            move-result v$scratch
            if-eqz v$scratch, :use_stock_array
            array-length v$scratch, v$array
            if-eqz v$scratch, :use_stock_array
            const/4 v$scratch, 0x0
            aget-object v$scratch, v$array, v$scratch
            iget-object p1, v$scratch, $original
            const/4 p4, 0x1
            const/4 v$scratch, 0x0
            new-array v$array, v$scratch, [${span.type}
        """, ExternalLabel("use_stock_array", formatter.getInstruction(length)))

        // Row state can later write false back to the span; keep onClick and updateDrawState active.
        val reads = span.methods.flatMap { method -> method.code().withIndex().filter { (_, ins) ->
            ins.opcode == Opcode.IGET_BOOLEAN && (ins as ReferenceInstruction).reference as FieldReference == access
        }.map { method to it.index } }
        if (reads.size != 2) throw PatchException("Twitch deleted messages: expected two access-flag reads, found ${reads.size}.")
        reads.groupBy({ it.first }, { it.second }).forEach { (method, indexes) ->
            indexes.sortedDescending().forEach { index ->
                val register = method.getInstruction<TwoRegisterInstruction>(index).registerA
                method.addInstructionsWithLabels(index + 1, """
                    if-nez v$register, :keep_access
                    invoke-static {}, $POLICY->enabled()Z
                    move-result v$register
                """, ExternalLabel("keep_access", method.getInstruction(index + 1)))
            }
        }

        val application = mutableClassDefBy("Ltv/twitch/android/app/consumer/TwitchApplication;").methods
            .filter { it.name == "onCreate" && it.isInstance(emptyList(), "V") }
            .uniqueHook("deleted message settings initialization")
        application.code().withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }.asReversed()
            .forEach { application.insertAtReturn(it, "invoke-static/range {p0 .. p0}, $POLICY->initialize(Landroid/app/Application;)V") }
    }
}
