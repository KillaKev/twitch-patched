package dev.twitchpatches.extension.ui;

import dev.twitchpatches.extension.settings.SettingSection;
import dev.twitchpatches.extension.settings.ToggleSetting;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UiFeatureRegistry {
    public interface Preferences {
        boolean read(String key);
        void write(String key, boolean enabled);
    }

    private final Preferences preferences;
    private final Map<String, ToggleSetting> features = new LinkedHashMap<>();
    private final Set<Runnable> listeners = new LinkedHashSet<>();

    public UiFeatureRegistry(Preferences preferences) { this.preferences = preferences; }

    public synchronized ToggleSetting register(String key, String title, String summary) {
        if (key == null || !key.matches("[a-z][a-z0-9_]{0,63}") || title == null || title.trim().isEmpty() || summary == null)
            throw new IllegalArgumentException("UI option requires a stable key, title and summary.");
        if (features.containsKey(key)) throw new IllegalStateException("Duplicate UI option: " + key);
        ToggleSetting option = new ToggleSetting() {
            private volatile boolean enabled = preferences.read(key);
            @Override public String key() { return key; }
            @Override public String title() { return title; }
            @Override public String summary() { return summary; }
            @Override public SettingSection section() { return SettingSection.INTERFACE; }
            @Override public boolean isEnabled() { return enabled; }
            @Override public void setEnabled(boolean value) {
                synchronized (UiFeatureRegistry.this) {
                    if (enabled == value) return;
                    preferences.write(key, value);
                    enabled = value;
                }
                notifyChanged();
            }
        };
        features.put(key, option);
        return option;
    }

    public synchronized boolean enabled(String key) {
        ToggleSetting option = features.get(key);
        return option != null && option.isEnabled();
    }

    public synchronized List<ToggleSetting> options() { return new ArrayList<>(features.values()); }

    public synchronized Map<String, Boolean> snapshot() {
        Map<String, Boolean> values = new LinkedHashMap<>();
        features.forEach((key, option) -> values.put(key, option.isEnabled()));
        return values;
    }

    public synchronized AutoCloseable observe(Runnable listener) {
        if (listener == null) throw new IllegalArgumentException("UI listener is required.");
        Runnable subscription = listener::run;
        listeners.add(subscription);
        return () -> { synchronized (UiFeatureRegistry.this) { listeners.remove(subscription); } };
    }

    private void notifyChanged() {
        List<Runnable> callbacks;
        synchronized (this) { callbacks = new ArrayList<>(listeners); }
        callbacks.forEach(Runnable::run);
    }
}
