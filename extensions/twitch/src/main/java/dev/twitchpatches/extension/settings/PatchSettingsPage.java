package dev.twitchpatches.extension.settings;

import android.app.DialogFragment;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toolbar;

@SuppressWarnings("deprecation") // Platform fragment avoids depending on Twitch's obfuscated AndroidX router.
public final class PatchSettingsPage extends DialogFragment {
    static final String TAG = "twitch_patches_settings";
    private SettingsNavigation navigation;
    private LinearLayout contents;
    private ScrollView scroll;
    private Toolbar toolbar;

    static PatchSettingsPage forGroup(String key) {
        PatchSettingsPage page = new PatchSettingsPage();
        Bundle arguments = new Bundle();
        arguments.putString("group", key);
        page.setArguments(arguments);
        return page;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setStyle(STYLE_NO_TITLE, 0);
        if (state != null && state.containsKey("routes") && state.containsKey("offsets")) {
            navigation = new SettingsNavigation(state.getStringArrayList("routes"), state.getIntegerArrayList("offsets"));
        } else navigation = new SettingsNavigation(getArguments() == null ? null : getArguments().getString("group"));
    }

    @Override public Dialog onCreateDialog(Bundle state) {
        return new Dialog(getActivity(), getTheme()) {
            @Override public void onBackPressed() { back(); }
        };
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        Context context = getActivity();
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(TwitchSettingsResources.color(context, "background_body"));
        toolbar = TwitchSettingsToolbar.create(context, this::back);
        page.addView(toolbar,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        contents = new LinearLayout(context);
        contents.setOrientation(LinearLayout.VERTICAL);
        int padding = TwitchSettingsResources.spacing(context, "space_16");
        contents.setPadding(0, 0, 0, padding);
        renderPage();
        scroll.addView(contents, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        restoreScroll();
        return page;
    }

    private void open(SettingGroup group) {
        navigation.open(group.key(), scroll.getScrollY());
        renderPage();
        scroll.scrollTo(0, 0);
    }

    private void renderPage() {
        SettingGroup group = PatchSettings.group(navigation.current());
        toolbar.setTitle(group == null ? "Patch settings" : group.title());
        contents.removeAllViews();
        TwitchSettingsRows.populate(contents, group == null ? PatchSettings.options() : group.children(), this::open, group == null);
        if (group != null && group.children().isEmpty() && !group.emptyMessage().isEmpty())
            TwitchSettingsRows.empty(contents, group.emptyMessage());
    }

    private void back() {
        if (!navigation.back()) { closePage(); return; }
        renderPage();
        restoreScroll();
    }

    private void restoreScroll() {
        int offset = navigation.scroll();
        String route = navigation.current();
        ScrollView target = scroll;
        target.post(() -> {
            if (scroll == target && java.util.Objects.equals(route, navigation.current())) target.scrollTo(0, offset);
        });
    }

    @Override public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (scroll != null) navigation.saveScroll(scroll.getScrollY());
        state.putStringArrayList("routes", navigation.routes());
        state.putIntegerArrayList("offsets", navigation.offsets());
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        contents = null;
        scroll = null;
        toolbar = null;
    }

    @Override public void onStart() {
        super.onStart();
        android.app.Dialog dialog = getDialog();
        if (dialog == null || getActivity() == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(TwitchSettingsResources.color(getActivity(), "background_body")));
        window.setDimAmount(0);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void closePage() {
        android.app.FragmentManager manager = getFragmentManager();
        if (manager == null || manager.isStateSaved()) dismissAllowingStateLoss();
        else dismiss();
    }
}
