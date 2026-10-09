package dev.twitchpatches.extension.settings;

import org.junit.Test;
import static org.junit.Assert.*;

public class SettingsNavigationTest {
    @Test public void nestedBackRestoresEachPageAndScrollPosition() {
        SettingsNavigation state = new SettingsNavigation((String) null);
        state.open("ui_settings", 180);
        state.open("ui_classic", 24);
        state.open("ui_classic_support", 80);
        assertEquals("ui_classic_support", state.current());
        assertEquals(0, state.scroll());
        assertTrue(state.back());
        assertEquals("ui_classic", state.current());
        assertEquals(80, state.scroll());
        assertTrue(state.back());
        assertEquals(24, state.scroll());
        assertTrue(state.back());
        assertNull(state.current());
        assertEquals(180, state.scroll());
        assertFalse(state.back());
    }

    @Test public void directEntryAndSavedStateKeepTheirOwnRoot() {
        SettingsNavigation state = new SettingsNavigation("ui_settings");
        state.open("ui_vertical", 90);
        state.saveScroll(45);
        SettingsNavigation restored = new SettingsNavigation(state.routes(), state.offsets());
        state.routes().clear();
        assertEquals("ui_vertical", restored.current());
        assertEquals(45, restored.scroll());
        assertTrue(restored.back());
        assertEquals("ui_settings", restored.current());
        assertEquals(90, restored.scroll());
        assertFalse(restored.back());
    }
}
