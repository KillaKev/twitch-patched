package dev.twitchpatches.extension.ui;

import dev.twitchpatches.extension.settings.ToggleSetting;
import dev.twitchpatches.extension.settings.SettingSection;
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
        assertThrows(IllegalArgumentException.class,
                () -> registry.register("missing_section", "Title", "", null, "ic_settings"));
        assertThrows(IllegalArgumentException.class,
                () -> registry.register("bad_icon", "Title", "", SettingSection.INTERFACE, "bad-icon"));
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

    @Test public void masterReflectsMixedStateAndPublishesOneCompleteUpdate() {
        UiFeatureRegistry registry = registry();
        ToggleSetting subscribe = registry.register("subscribe", "Subscribe", "");
        ToggleSetting bits = registry.register("bits", "Bits", "");
        ToggleSetting master = registry.master("all", java.util.List.of(subscribe, bits));
        AtomicInteger updates = new AtomicInteger();
        registry.observe(() -> {
            assertEquals(subscribe.isEnabled(), bits.isEnabled());
            updates.incrementAndGet();
        });
        master.setEnabled(true);
        assertTrue(master.isEnabled());
        assertEquals(1, updates.get());
        master.setEnabled(true);
        assertEquals(1, updates.get());
        master.setEnabled(false);
        assertFalse(master.isEnabled());
        assertEquals(2, updates.get());
        assertFalse(saved.containsKey("all"));
        assertFalse(registry.snapshot().containsKey("all"));
    }

    @Test public void catalogueKeepsLayoutsIndependentAndEveryOptionOff() {
        UiFeatureRegistry registry = registry();
        var categories = UiOptions.create(registry, true);
        assertEquals(8, categories.size());
        assertEquals(java.util.List.of(SettingSection.PLAYER_CONTROLS, SettingSection.CHANNEL_ACTIONS, SettingSection.FULLSCREEN),
                categories.stream().map(option -> option.section()).distinct().collect(java.util.stream.Collectors.toList()));
        assertTrue(categories.stream().allMatch(option -> !option.title().isEmpty() && !option.icon().isEmpty()));
        assertEquals(17, registry.options().size());
        assertTrue(registry.options().stream().noneMatch(ToggleSetting::isEnabled));
        ToggleSetting classic = registry.options().stream().filter(option -> option.key().equals("classic_support_hide_bits")).findFirst().orElseThrow();
        classic.setEnabled(true);
        assertTrue(registry.enabled("classic_support_hide_bits"));
        assertFalse(registry.enabled("vertical_support_hide_bits"));
    }

    @Test public void reorganizedCatalogueRetainsSavedPreferencesAndContextualMasters() {
        saved.put("classic_support_hide_bits", true);
        saved.put("fullscreen_hide_status_bar", true);
        UiFeatureRegistry registry = registry();
        var categories = UiOptions.create(registry, true);
        assertTrue(registry.enabled("classic_support_hide_bits"));
        assertFalse(registry.enabled("vertical_support_hide_bits"));
        assertTrue(registry.enabled("fullscreen_hide_status_bar"));
        ToggleSetting bars = (ToggleSetting) categories.get(5);
        assertEquals("Hide both system bars", bars.title());
        assertEquals("1 of 2 hidden.", bars.summary());
        assertFalse(bars.isEnabled());
        bars.setEnabled(true);
        assertTrue(registry.enabled("fullscreen_hide_navigation_bar"));
        bars.setEnabled(false);
        assertFalse(registry.enabled("fullscreen_hide_status_bar"));
        assertTrue(registry.enabled("classic_support_hide_bits"));
    }

    @Test public void headerControlsAreIndependentAndUnsupportedButtonsAreExcluded() {
        UiFeatureRegistry registry = registry();
        var groups = UiOptions.create(registry, true);
        var header = (dev.twitchpatches.extension.settings.SettingGroup) groups.get(4);
        ToggleSetting all = (ToggleSetting) header.children().get(0);
        assertEquals("Hide all header buttons", all.title());
        assertFalse(all.isEnabled());
        registry.options().stream().filter(option -> option.key().equals("vertical_header_hide_gift"))
                .findFirst().orElseThrow().setEnabled(true);
        assertFalse(registry.enabled("vertical_header_hide_leaderboard"));
        assertFalse(registry.enabled("vertical_support_hide_gift"));
        assertEquals("1 of 3 hidden.", all.summary());
        all.setEnabled(true);
        assertTrue(registry.enabled("vertical_header_hide_leaderboard"));
        assertFalse(registry.enabled("classic_support_hide_gift"));
        all.setEnabled(false);
        assertFalse(registry.enabled("vertical_header_hide_gift"));
        assertFalse(registry.enabled("vertical_header_hide_leaderboard"));
        UiFeatureRegistry previous = registry();
        UiOptions.create(previous, false);
        assertEquals(16, previous.options().size());
        assertFalse(previous.snapshot().containsKey("vertical_header_hide_leaderboard"));
    }
}
