package tv.dumbtv.home;

/** Small host-side checks for the pixel path, without a device or test framework. */
public final class FocusColorTest {
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }

    public static void main(String[] args) {
        check(GuardRecoveryPolicy.shouldRetry(0, false, true), "active recovery allows first retry");
        check(GuardRecoveryPolicy.shouldRetry(1, false, true), "active recovery allows second retry");
        check(!GuardRecoveryPolicy.shouldRetry(2, false, true), "recovery retries are bounded");
        check(!GuardRecoveryPolicy.shouldRetry(0, true, true), "paused recovery stops");
        check(!GuardRecoveryPolicy.shouldRetry(0, false, false), "inactive recovery stops");
        check(GuardRecoveryPolicy.delayMs(1) < GuardRecoveryPolicy.delayMs(2), "retries back off");
        check(!GuardRecoveryPolicy.shouldCancelForWindow("tv.dumbtv.home", "tv.dumbtv.home",
                "tv.dumbtv.home.Home", "com.amazon.tv.launcher", "tv.dumbtv.home.Home",
                "android.view.View"), "Home window does not cancel recovery");
        check(!GuardRecoveryPolicy.shouldCancelForWindow("tv.dumbtv.home", "tv.dumbtv.home",
                "android.view.View", "com.amazon.tv.launcher", "tv.dumbtv.home.Home",
                "android.view.View"), "own overlay does not cancel recovery");
        check(GuardRecoveryPolicy.shouldCancelForWindow("tv.dumbtv.home", "tv.dumbtv.home",
                "tv.dumbtv.home.Options", "com.amazon.tv.launcher", "tv.dumbtv.home.Home",
                "android.view.View"), "internal Options screen cancels recovery");
        check(GuardRecoveryPolicy.shouldCancelForWindow("tv.dumbtv.home", "tv.dumbtv.home",
                "tv.dumbtv.home.Input", "com.amazon.tv.launcher", "tv.dumbtv.home.Home",
                "android.view.View"), "internal Input screen cancels recovery");
        check(!GuardRecoveryPolicy.shouldCancelForWindow("tv.dumbtv.home", "com.android.systemui",
                "com.android.systemui.SomeWindow", "com.amazon.tv.launcher", "tv.dumbtv.home.Home",
                "android.view.View"), "system UI does not cancel recovery");
        check(!GuardRecoveryPolicy.shouldCancelForWindow("tv.dumbtv.home", "com.amazon.tv.launcher",
                "com.amazon.tv.launcher.HomeActivity_vNext", "com.amazon.tv.launcher",
                "tv.dumbtv.home.Home", "android.view.View"), "Amazon event does not cancel recovery");
        check(GuardRecoveryPolicy.shouldCancelForWindow("tv.dumbtv.home", "com.netflix.ninja",
                "com.netflix.ninja.MainActivity", "com.amazon.tv.launcher", "tv.dumbtv.home.Home",
                "android.view.View"), "another app cancels recovery");

        int[] transparent = new int[100];
        check(FocusColor.representative(transparent) == FocusColor.FALLBACK, "transparent fallback");

        int[] icon = new int[100];
        for (int i = 0; i < icon.length; i++) icon[i] = i < 75 ? 0xFFFFFFFF : 0xFFFF3020;
        int color = FocusColor.representative(icon);
        int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
        check(red > green * 2 && red > blue * 2, "colored logo beats white backdrop");
        check(red < 200, "bright art darkened");

        int[] blueIcon = new int[100];
        for (int i = 0; i < blueIcon.length; i++) blueIcon[i] = 0xFF2050E0;
        color = FocusColor.representative(blueIcon);
        check((color & 255) > (color >> 16 & 255), "blue stays blue");
        check((FocusColor.tint(color) >>> 24) == 61, "tint is 24 percent opaque");
        check((FocusColor.tint(color) & 0xFFFFFF) == (color & 0xFFFFFF), "tint keeps color");
        check("adaptive".equals(Config.BACKGROUND.fallback), "selected-app artwork is the default backdrop");
        check("Tinte adaptable del fondo".equals(Config.BACKGROUND.name("adaptive-tint")),
                "background choice is Spanish");
        check("Ámbar".equals(Config.ACCENT.name("amber")), "accent choice is Spanish");
        check("Color de acento".equals(Config.ACCENT.title), "settings labels are Spanish");
        System.out.println("FocusColorTest passed");
    }
}
