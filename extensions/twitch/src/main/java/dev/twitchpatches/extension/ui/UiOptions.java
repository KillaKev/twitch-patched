package dev.twitchpatches.extension.ui;

import dev.twitchpatches.extension.settings.SettingEntry;
import dev.twitchpatches.extension.settings.SettingGroup;
import dev.twitchpatches.extension.settings.SettingSection;
import dev.twitchpatches.extension.settings.ToggleSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class UiOptions {
    static List<SettingEntry> create(UiFeatureRegistry registry) {
        return Arrays.asList(layout(registry, "classic", "Classic Split"), layout(registry, "vertical", "Vertical View"),
                fullscreen(registry));
    }

    private static SettingGroup fullscreen(UiFeatureRegistry registry) {
        List<ToggleSetting> toggles = Arrays.asList(
                registry.register("fullscreen_hide_status_bar", "Hide status bar", "Use the camera cutout area during fullscreen playback."),
                registry.register("fullscreen_hide_navigation_bar", "Hide navigation bar", "Hide Android navigation during fullscreen playback."));
        List<SettingEntry> entries = new ArrayList<>();
        entries.add(registry.master("fullscreen_all", toggles));
        entries.addAll(toggles);
        return group("ui_fullscreen", "Fullscreen", "System bars and camera cutout.", entries);
    }

    private static SettingGroup layout(UiFeatureRegistry registry, String prefix, String title) {
        return group("ui_" + prefix, title, "Player controls and channel actions.", Arrays.asList(
                buttons(registry, prefix + "_controls", "Player controls", new String[][] {
                        {"clip", "Clip"}, {"cast", "Cast"}, {"share", "Share"}}),
                buttons(registry, prefix + "_support", "Channel actions", new String[][] {
                        {"subscribe", "Subscribe"}, {"gift", "Gift a Sub"}, {"bits", "Bits"}})));
    }

    private static SettingGroup buttons(UiFeatureRegistry registry, String prefix, String title, String[][] definitions) {
        List<ToggleSetting> toggles = new ArrayList<>();
        for (String[] definition : definitions)
            toggles.add(registry.register(prefix + "_hide_" + definition[0], "Hide " + definition[1], ""));
        List<SettingEntry> entries = new ArrayList<>();
        entries.add(registry.master(prefix + "_all", toggles));
        entries.addAll(toggles);
        return group("ui_" + prefix, title, "", entries);
    }

    private static SettingGroup group(String key, String title, String summary, List<? extends SettingEntry> entries) {
        return new SettingGroup() {
            @Override public String key() { return key; }
            @Override public String title() { return title; }
            @Override public String summary() { return summary; }
            @Override public SettingSection section() { return SettingSection.INTERFACE; }
            @Override public List<? extends SettingEntry> children() { return entries; }
        };
    }
}
