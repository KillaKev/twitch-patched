package dev.twitchpatches.extension.ui;

public final class UiComposeBridge {
    public static Object state;
    // Replaced with Twitch's typed StateFlow calls during patching.
    public static void update() { throw new UnsupportedOperationException("UI composition bridge is not patched."); }
}
