package dev.twitchpatches.patches.twitch.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import dev.twitchpatches.patches.twitch.shared.code
import org.junit.Assert.*
import org.junit.Test

class UiFullscreenHooksTest {
    @Test fun filtersOnlyTheTypeMaskAndPreservesTheOriginalQueryAndItsLabel() {
        val method = fixture()
        val before = method.code()
        val implementation = requireNotNull(method.implementation)
        val label = implementation.newLabelForIndex(2)
        injectUiInsetFilter(method)
        val code = method.code()
        assertEquals(7, implementation.registerCount)
        assertEquals(2, label.location.index)
        val call = code[2] as FiveRegisterInstruction
        assertEquals(5, call.registerC)
        assertEquals(0, call.registerD)
        assertEquals("$UI_SETTINGS->filterWindowInsetTypes(Landroid/view/View;I)I",
            (call as ReferenceInstruction).reference.toString())
        assertEquals(Opcode.MOVE_RESULT, code[3].opcode)
        assertEquals(0, (code[3] as OneRegisterInstruction).registerA)
        assertEquals(before.map { it.opcode }, (code.take(2) + code.drop(4)).map { it.opcode })
        assertEquals((before[2] as ReferenceInstruction).reference, (code[4] as ReferenceInstruction).reference)
    }

    @Test fun rejectsChangedQueriesAndParameterRegisterClobbering() {
        assertThrows(IllegalArgumentException::class.java) {
            resolveUiInsetHook(fixture(query = "invoke-virtual {p2, v1}, Lfixture/InsetsImpl;->insets(I)Lfixture/Edges;"))
        }
        assertThrows(IllegalArgumentException::class.java) { resolveUiInsetHook(fixture(mask = "p1")) }
    }

    private fun fixture(mask: String = "v0", query: String =
        "invoke-virtual {p2, v0}, Lfixture/InsetsImpl;->insets(I)Lfixture/Edges;") =
        ImmutableMethod("Lfixture/Listener;", "onApplyWindowInsets",
            listOf("Landroid/view/View;", "Lfixture/Insets;").map { ImmutableMethodParameter(it, null, null) },
            "Lfixture/Insets;", AccessFlags.PUBLIC.value, null, null, MutableMethodImplementation(7)).toMutable().apply {
            addInstructionsWithLabels(0, """
                const/16 $mask, 0x28f
                iget-object p2, p2, Lfixture/Insets;->impl:Lfixture/InsetsImpl;
                $query
                move-result-object v4
                return-object p2
            """)
        }
}
