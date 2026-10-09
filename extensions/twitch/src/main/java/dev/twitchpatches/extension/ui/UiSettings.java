package dev.twitchpatches.extension.ui;

import android.app.Application;
import android.content.SharedPreferences;
import dev.twitchpatches.extension.settings.PatchSettings;
import dev.twitchpatches.extension.settings.SettingGroup;
import dev.twitchpatches.extension.settings.SettingEntry;
import dev.twitchpatches.extension.settings.SettingSection;
import dev.twitchpatches.extension.shared.ReactNativeRuntime;
import java.util.List;
import org.json.JSONObject;

public final class UiSettings implements SettingGroup {
    public static final String KEY = "ui_settings";
    private static volatile UiSettings instance;
    private final UiFeatureRegistry features;
    private final List<SettingEntry> categories;

    private UiSettings(Application application) {
        SharedPreferences preferences = application.getSharedPreferences("twitch_patches_ui", 0);
        features = new UiFeatureRegistry(new UiFeatureRegistry.Preferences() {
            @Override public boolean read(String key) { return preferences.getBoolean(key, false); }
            @Override public void write(String key, boolean enabled) {
                preferences.edit().putBoolean(key, enabled).apply();
            }
        });
        categories = UiOptions.create(features);
        features.observe(() -> { ReactNativeRuntime.refresh(); UiComposeBridge.update(); });
    }

    public static synchronized void initialize(Application application) {
        if (instance != null) return;
        instance = new UiSettings(application);
        UiComposeBridge.update();
        UiFullscreenPolicy.initialize(application);
        ReactNativeRuntime.select(7);
        PatchSettings.register(instance);
    }

    public static boolean enabled(String key) {
        UiSettings settings = instance;
        return settings != null && settings.features.enabled(key);
    }

    public static AutoCloseable observe(Runnable listener) { return requireInstance().features.observe(listener); }

    public static String snapshot() {
        UiSettings settings = instance;
        return settings == null ? "{}" : new JSONObject(settings.features.snapshot()).toString();
    }

    public static boolean openLink(String url) {
        if (instance == null || !"twitch-patches://settings/ui".equals(url)) return false;
        open();
        return true;
    }

    public static void open() { if (instance != null) PatchSettings.openGroup(KEY); }

    private static UiSettings requireInstance() {
        UiSettings settings = instance;
        if (settings == null) throw new IllegalStateException("UI settings must initialize before registering options.");
        return settings;
    }

    @Override public String key() { return KEY; }
    @Override public String title() { return "UI settings"; }
    @Override public String summary() { return "Customize the Twitch interface."; }
    @Override public String emptyMessage() { return "No UI options are available in this build."; }
    @Override public SettingSection section() { return SettingSection.INTERFACE; }
    @Override public List<SettingEntry> children() { return categories; }
}
