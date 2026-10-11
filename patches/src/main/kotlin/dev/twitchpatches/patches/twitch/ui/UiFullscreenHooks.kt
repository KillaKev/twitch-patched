package dev.twitchpatches.patches.twitch.ui

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import dev.twitchpatches.patches.twitch.shared.*

private const val VIEW = "Landroid/view/View;"
private const val LANDING = "Ltv/twitch/android/feature/viewer/landing/ViewerLandingActivity;"

internal fun BytecodePatchContext.injectUiFullscreenInsets() {
    val matches = mutableListOf<Method>()
    classDefForEach { type ->
        type.methods.filterTo(matches) { method ->
            method.name == "onApplyWindowInsets" && method.parameterTypes.size == 2 &&
                method.parameterTypes[0].toString() == VIEW && method.returnType == method.parameterTypes[1].toString() &&
                method.references().filterIsInstance<FieldReference>().any { it.type == LANDING } &&
                method.references().filterIsInstance<MethodReference>().any {
                    it.definingClass == VIEW && it.name == "setPadding" && it.parameterTypes.map(CharSequence::toString) == listOf("I", "I", "I", "I")
                }
        }
    }
    val target = matches.uniqueHook("viewer layout inset listener")
    resolveUiInsetHook(target)
    val method = mutableClassDefBy(target.definingClass).methods.filter { it.reference == target.reference }
        .uniqueHook("mutable viewer layout inset listener")
    injectUiInsetFilter(method)
}

internal data class UiInsetHook(val callIndex: Int, val maskRegister: Int, val viewRegister: Int)

internal fun resolveUiInsetHook(method: Method): UiInsetHook {
    require(method.isInstance(listOf(VIEW, method.returnType), method.returnType)) {
        "UI settings: viewer inset listener signature changed."
    }
    val code = method.code()
    val maskIndex = code.indices.filter {
        code[it].opcode == Opcode.CONST_16 && (code[it] as? NarrowLiteralInstruction)?.narrowLiteral == 655
    }.uniqueHook("viewer inset type mask")
    val mask = (code[maskIndex] as OneRegisterInstruction).registerA
    val callIndex = maskIndex + 2
    val read = code.getOrNull(maskIndex + 1)
    val call = code.getOrNull(callIndex)
    val arguments = call as? FiveRegisterInstruction
    val reference = (call as? ReferenceInstruction)?.reference as? MethodReference
    val field = (read as? ReferenceInstruction)?.reference as? FieldReference
    val view = requireNotNull(method.implementation).registerCount - 2
    require(read?.opcode == Opcode.IGET_OBJECT && field != null && field.definingClass == method.parameterTypes[1].toString()) {
        "UI settings: viewer inset implementation changed."
    }
    require(call?.opcode == Opcode.INVOKE_VIRTUAL && arguments != null && arguments.registerCount == 2 &&
        arguments.registerD == mask && arguments.registerC == (read as? TwoRegisterInstruction)?.registerA &&
        reference != null && reference.definingClass == field.type && reference.parameterTypes.map(CharSequence::toString) == listOf("I") &&
        reference.returnType.startsWith("L") && code.getOrNull(callIndex + 1)?.opcode == Opcode.MOVE_RESULT_OBJECT) {
        "UI settings: viewer inset query changed."
    }
    require(view in 0..15 && mask in 0 until view) {
        "UI settings: viewer inset registers changed."
    }
    return UiInsetHook(callIndex, mask, view)
}

internal fun injectUiInsetFilter(method: MutableMethod) {
    val hook = resolveUiInsetHook(method)
    method.insertBeforeWithLabels(hook.callIndex, """
        invoke-static {v${hook.viewRegister}, v${hook.maskRegister}}, $UI_SETTINGS->filterWindowInsetTypes(${VIEW}I)I
        move-result v${hook.maskRegister}
    """)
}
