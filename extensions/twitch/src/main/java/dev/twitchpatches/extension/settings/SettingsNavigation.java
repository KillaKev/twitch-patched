package dev.twitchpatches.extension.settings;

import java.util.ArrayList;
import java.util.List;

final class SettingsNavigation {
    private final List<String> routes = new ArrayList<>();
    private final List<Integer> offsets = new ArrayList<>();

    SettingsNavigation(String initial) { routes.add(initial); offsets.add(0); }

    SettingsNavigation(List<String> routes, List<Integer> offsets) {
        if (routes == null || offsets == null || routes.isEmpty() || routes.size() != offsets.size())
            throw new IllegalArgumentException("Invalid settings navigation.");
        this.routes.addAll(routes);
        this.offsets.addAll(offsets);
    }

    String current() { return routes.get(routes.size() - 1); }
    int scroll() { return offsets.get(offsets.size() - 1); }
    void saveScroll(int value) { offsets.set(offsets.size() - 1, Math.max(0, value)); }
    void open(String key, int scroll) { saveScroll(scroll); routes.add(key); offsets.add(0); }
    boolean back() {
        if (routes.size() == 1) return false;
        routes.remove(routes.size() - 1);
        offsets.remove(offsets.size() - 1);
        return true;
    }
    ArrayList<String> routes() { return new ArrayList<>(routes); }
    ArrayList<Integer> offsets() { return new ArrayList<>(offsets); }
}
