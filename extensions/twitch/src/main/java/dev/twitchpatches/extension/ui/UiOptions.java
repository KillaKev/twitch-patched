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
        List<SettingEntry> entries = new ArrayList<>();
        entries.add(buttons(registry, "classic_controls", "Classic Split controls", SettingSection.PLAYER_CONTROLS,
                "Clip, Cast and Share in Classic Split.", controls()));
        entries.add(buttons(registry, "vertical_controls", "Vertical View controls", SettingSection.PLAYER_CONTROLS,
                "Clip, Cast and Share in Vertical View.", controls()));
        entries.add(buttons(registry, "classic_support", "Classic Split actions", SettingSection.CHANNEL_ACTIONS,
                "Subscribe, Gift a Sub and Bits in Classic Split.", actions()));
        entries.add(buttons(registry, "vertical_support", "Vertical View actions", SettingSection.CHANNEL_ACTIONS,
                "Subscribe, Gift a Sub and Bits in Vertical View.", actions()));
        List<ToggleSetting> bars = Arrays.asList(
                registry.register("fullscreen_hide_status_bar", "Hide status bar",
                        "Hide Android's status bar and use the camera cutout area in landscape fullscreen.",
                        SettingSection.FULLSCREEN, "ic_fullscreen_enter"),
                registry.register("fullscreen_hide_navigation_bar", "Hide navigation bar",
                        "Hide Android's navigation bar in landscape fullscreen.",
                        SettingSection.FULLSCREEN, "ic_fullscreen_enter"));
        entries.add(registry.master("fullscreen_all", bars, "Hide both system bars", SettingSection.FULLSCREEN));
        entries.addAll(bars);
        return entries;
    }

    private static String[][] controls() {
        return new String[][] {{"clip", "Clip", "ic_clip"}, {"cast", "Cast", "chromecast"}, {"share", "Share", "ic_share"}};
    }

    private static String[][] actions() {
        return new String[][] {{"subscribe", "Subscribe", "ic_subscribe"}, {"gift", "Gift a Sub", "ic_gift"}, {"bits", "Bits", "ic_bits"}};
    }

    private static SettingGroup buttons(UiFeatureRegistry registry, String prefix, String title, SettingSection section,
            String summary, String[][] definitions) {
        List<ToggleSetting> toggles = new ArrayList<>();
        for (String[] definition : definitions)
            toggles.add(registry.register(prefix + "_hide_" + definition[0], "Hide " + definition[1] + " button", "", section, definition[2]));
        List<SettingEntry> entries = new ArrayList<>();
        String all = section == SettingSection.PLAYER_CONTROLS ? "Hide all player buttons" : "Hide all channel buttons";
        entries.add(registry.master(prefix + "_all", toggles, all, section));
        entries.addAll(toggles);
        return new SettingGroup() {
            @Override public String key() { return "ui_" + prefix; }
            @Override public String title() { return title; }
            @Override public String summary() { return summary; }
            @Override public SettingSection section() { return section; }
            @Override public List<? extends SettingEntry> children() { return entries; }
        };
    }
}
