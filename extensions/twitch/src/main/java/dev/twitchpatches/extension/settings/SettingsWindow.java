package dev.twitchpatches.extension.settings;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public final class SettingsWindow {
    private SettingsWindow() { }

    public static void configure(Window window, int background) {
        window.setBackgroundDrawable(new ColorDrawable(background));
        window.setDimAmount(0);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attributes);
        }
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
}
