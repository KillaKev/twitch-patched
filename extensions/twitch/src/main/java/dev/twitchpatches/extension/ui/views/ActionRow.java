package dev.twitchpatches.extension.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Space;
import android.widget.TextView;
import dev.twitchpatches.extension.ui.UiSettings;
import dev.twitchpatches.extension.ui.UiViewPolicy;

public final class ActionRow extends android.widget.LinearLayout {
    public ActionRow(Context context, AttributeSet attributes) {
        super(context, attributes);
        UiViewPolicy.bind(this);
    }

    @Override public void setVisibility(int visibility) { super.setVisibility(UiViewPolicy.visibility(this, visibility)); }

    @Override protected void onMeasure(int width, int height) {
        super.onMeasure(width, height);
        boolean customized = UiSettings.enabled("hide_channel_extensions") || UiSettings.enabled("classic_support_hide_bits") ||
                UiSettings.enabled("classic_support_hide_subscribe") || UiSettings.enabled("classic_support_hide_gift");
        if (customized && getChildCount() > 0 && getChildAt(0) instanceof ViewGroup && !hasContent((ViewGroup) getChildAt(0)))
            setMeasuredDimension(getMeasuredWidth(), 0);
    }

    private static boolean hasContent(ViewGroup row) {
        for (int index = 0; index < row.getChildCount(); index++) {
            View child = row.getChildAt(index);
            if (child.getVisibility() != VISIBLE || child instanceof Space) continue;
            if (child instanceof TextView && ((TextView) child).length() == 0) continue;
            if (child.getMeasuredWidth() > 0 && child.getMeasuredHeight() > 0) return true;
        }
        return false;
    }
}
