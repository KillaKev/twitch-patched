package dev.twitchpatches.extension.ui;

import dev.twitchpatches.extension.settings.ToggleSetting;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class UiFeatureRegistryTest {
    private final Map<String, Boolean> saved = new HashMap<>();
    private UiFeatureRegistry registry() {
        return new UiFeatureRegistry(new UiFeatureRegistry.Preferences() {
            @Override public boolean read(String key) { return saved.getOrDefault(key, false); }
            @Override public void write(String key, boolean value) { saved.put(key, value); }
        });
    }

    @Test public void optionsAreOffUntilEnabledAndRemainIndependent() {
        UiFeatureRegistry registry = registry();
        ToggleSetting first = registry.register("hide_clip", "Hide Clip", "Hide the Clip control.");
        registry.register("hide_share", "Hide Share", "Hide the Share control.");
        assertFalse(first.isEnabled());
        assertFalse(registry.enabled("missing"));
        first.setEnabled(true);
        assertTrue(registry.enabled("hide_clip"));
        assertFalse(registry.enabled("hide_share"));
        assertTrue(registry().register("hide_clip", "Hide Clip", "").isEnabled());
        first.setEnabled(false);
        assertFalse(registry().register("hide_clip", "Hide Clip", "").isEnabled());
    }

    @Test public void removedOptionsCannotActivateAndSnapshotsAreDetached() {
        saved.put("removed_option", true);
        UiFeatureRegistry registry = registry();
        assertFalse(registry.enabled("removed_option"));
        assertTrue(registry.options().isEmpty());
        registry.register("hide_clip", "Hide Clip", "");
        registry.snapshot().put("hide_clip", true);
        registry.options().clear();
        assertEquals(1, registry.options().size());
        assertFalse(registry.enabled("hide_clip"));
        assertFalse(registry.snapshot().containsKey("removed_option"));
    }

    @Test public void changesNotifyOnceAndClosedSubscriptionsStop() throws Exception {
        UiFeatureRegistry registry = registry();
        ToggleSetting option = registry.register("hide_clip", "Hide Clip", "");
        AtomicInteger changes = new AtomicInteger();
        AutoCloseable subscription = registry.observe(changes::incrementAndGet);
        option.setEnabled(true);
        option.setEnabled(true);
        option.setEnabled(false);
        assertEquals(2, changes.get());
        subscription.close();
        option.setEnabled(true);
        assertEquals(2, changes.get());
    }

    @Test public void rejectsDuplicateAndInvalidKeys() {
        UiFeatureRegistry registry = registry();
        registry.register("hide_clip", "Hide Clip", "");
        assertThrows(IllegalStateException.class, () -> registry.register("hide_clip", "Other", ""));
        for (String key : new String[] {"", "Hide Clip", "1hide", "bad-key"})
            assertThrows(IllegalArgumentException.class, () -> registry.register(key, "Title", ""));
    }

    @Test public void subscriptionsWithTheSameListenerHaveIndependentLifetimes() throws Exception {
        UiFeatureRegistry registry = registry();
        ToggleSetting option = registry.register("hide_clip", "Hide Clip", "");
        AtomicInteger changes = new AtomicInteger();
        Runnable listener = changes::incrementAndGet;
        AutoCloseable first = registry.observe(listener);
        AutoCloseable second = registry.observe(listener);
        first.close();
        option.setEnabled(true);
        assertEquals(1, changes.get());
        second.close();
        option.setEnabled(false);
        assertEquals(1, changes.get());
    }
}
