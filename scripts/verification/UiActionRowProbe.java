import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import dev.twitchpatches.extension.ui.UiViewPolicy;
import dev.twitchpatches.extension.ui.views.ActionRow;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Set;

public final class UiActionRowProbe {
    private static final String ROW = "classic_support_hide_row";
    private static final String EXTENSIONS = "hide_channel_extensions";
    private static Field instance;
    private static Constructor<?> settings;

    public static void main(String[] args) {
        try { run(); }
        catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }

    private static void run() throws Exception {
        Looper.prepareMainLooper();
        Class.forName("android.graphics.Typeface").getDeclaredMethod("loadPreinstalledSystemFontMap").invoke(null);
        Class<?> thread = Class.forName("android.app.ActivityThread");
        Object systemThread = thread.getMethod("systemMain").invoke(null);
        Context context = (Context) thread.getMethod("getSystemContext").invoke(systemThread);
        context = new ContextThemeWrapper(context, android.R.style.Theme_Material);
        Class<?> registry = Class.forName("dev.twitchpatches.extension.ui.UiSettings");
        instance = registry.getDeclaredField("instance");
        instance.setAccessible(true);
        settings = registry.getDeclaredConstructor(Application.class);
        settings.setAccessible(true);
        setPreferences(Set.of());
        ActionRow row = new ActionRow(context, null);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setTag("twitchpatches_ui:" + ROW);
        UiViewPolicy.bind(row);
        LinearLayout header = new LinearLayout(context);
        row.addView(header, new LinearLayout.LayoutParams(320, 80));
        row.addView(new View(context), new LinearLayout.LayoutParams(320, 1));
        TextView title = new TextView(context);
        header.addView(title, new LinearLayout.LayoutParams(0, 40, 1));
        FrameLayout extensions = new dev.twitchpatches.extension.ui.views.FrameLayout(context, null);
        extensions.setTag("twitchpatches_ui:" + EXTENSIONS);
        UiViewPolicy.bind(extensions);
        header.addView(extensions, new LinearLayout.LayoutParams(100, 40));
        FrameLayout buttons = new FrameLayout(context);
        header.addView(buttons, new LinearLayout.LayoutParams(100, 40));
        checkHeight(row, 81, "unchanged defaults");
        setPreferences(Set.of(EXTENSIONS));
        extensions.setVisibility(View.VISIBLE);
        if (extensions.getVisibility() != View.GONE) throw new AssertionError("extension visibility override");
        checkHeight(row, 81, "remaining buttons retain row height");
        buttons.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        checkHeight(row, 0, "empty contents collapse fixed header and divider");
        title.setText("Chat");
        checkHeight(row, 81, "visible title remains");
        title.setText("");
        buttons.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
        checkHeight(row, 81, "restored action reopens the row");
        setPreferences(Set.of(EXTENSIONS, ROW));
        row.setVisibility(View.VISIBLE);
        if (row.getVisibility() != View.GONE) throw new AssertionError("whole-row visibility override");
        setPreferences(Set.of(EXTENSIONS));
        row.setVisibility(View.VISIBLE);
        if (row.getVisibility() != View.VISIBLE) throw new AssertionError("row re-enable requires restart");
        checkHeight(row, 81, "row restored while extensions remain hidden");
        setPreferences(Set.of());
        extensions.setVisibility(View.VISIBLE);
        if (extensions.getVisibility() != View.VISIBLE) throw new AssertionError("extension re-enable requires restart");
        checkHeight(row, 81, "original row restored");
        instance.set(null, null);
        System.out.println("PASS action-row policy: defaults, independent extensions, empty-row collapse, title preservation and re-enable");
    }

    private static void checkHeight(ActionRow row, int height, String label) {
        row.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        if (row.getMeasuredHeight() != height) throw new AssertionError(label + ": " + row.getMeasuredHeight());
    }

    private static void setPreferences(Set<String> enabled) throws Exception {
        Application application = new Application() {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return (SharedPreferences) Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(), new Class<?>[] {SharedPreferences.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("contains")) return false;
                            if (method.getName().equals("getBoolean")) return enabled.contains(args[0]);
                            throw new UnsupportedOperationException(method.getName());
                        });
            }
        };
        instance.set(null, settings.newInstance(application));
    }
}
