package tv.dumbtv.home;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS may route HOME to Amazon's protected launcher, so this service filters HOME before
 * device policy and opens our home directly. The window observer remains a fallback for launches
 * that do not pass through the accessibility key filter.
 *
 * A plain startActivity right after a HOME press is held back by Android's five-second
 * app-switch delay. An alarm-fired PendingIntent is sent by system_server, which is exempt,
 * so {@link #home} launches through an alarm. That alarm only fires at once when the global
 * {@code alarm_manager_constants} has {@code min_futurity=0}; install.sh sets it.
 *
 * Amazon's home is covered with a plain overlay until ours has drawn, so it never shows.
 *
 * Android 11 refuses an activity start from an app with no visible window, even one sent
 * through system_server by an alarm ("Background activity start ... callingUidHasAnyVisibleWindow:
 * false"). The cover is such a window, so the launch waits until the cover has reached the
 * window manager: {@link #launchWhenCovered} fires it from the cover's first draw pass.
 */
public final class Guard extends AccessibilityService {
    static final String AMAZON = "com.amazon.tv.launcher";
    private static final String AMAZON_HOME = "com.amazon.tv.launcher.ui.HomeActivity_vNext";
    private static final String DEBUG_TAG = "DumbtvHomeDbg";
    private static final long COVER_MAX_MS = 1500;
    private static final long REPEAT_MS = 150;
    private static final long LAUNCH_FALLBACK_MS = 150;

    private static volatile boolean paused;
    private static Guard running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable uncover = this::uncover;
    private final Runnable launch = () -> dispatchHome("draw-fallback");
    private final Runnable keyLaunch = this::launchWhenCovered;
    private final Runnable recovery = this::retryHome;
    private View cover;
    private ViewTreeObserver.OnPreDrawListener pendingPredraw;
    private long lastLaunch;
    private int recoveryRetries;
    private boolean recoveryActive;

    private static boolean debugEnabled() {
        return Log.isLoggable(DEBUG_TAG, Log.DEBUG);
    }

    private static void debug(String message) {
        Log.d(DEBUG_TAG, "uptime=" + SystemClock.uptimeMillis() + " " + message);
    }

    /** Lets Amazon's home stay on screen until the user leaves it. */
    static void pause() {
        paused = true;
        if (running != null) running.stopRecovery("paused");
        Keeper.pause();
    }

    static void home(Context c) {
        if (debugEnabled()) debug("schedule-home context=" + c.getClass().getSimpleName());
        AlarmManager alarms = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        alarms.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), launch(c));
    }

    /** Drops a queued launch, so it cannot pull the user back after they opened an app. */
    static void arrived(Context c) {
        if (debugEnabled()) debug("home-arrived activity=" + c.getClass().getSimpleName());
        AlarmManager alarms = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(launch(c));
        if (running != null && debugEnabled()) debug("home-arrived-recovery-retained");
    }

    /** Called once our home window is on screen. */
    static void shown() {
        if (running != null) running.uncover();
    }

    private static PendingIntent launch(Context c) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setClass(c, Home.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT,
                ActivityOptions.makeCustomAnimation(c, 0, 0).toBundle());
    }

    static void amazon(Context c) {
        pause();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setPackage(AMAZON).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
        } catch (ActivityNotFoundException e) {
            paused = false;
        }
    }

    @Override
    protected void onServiceConnected() {
        running = this;
        if (debugEnabled()) debug("guard-connected");
        View view = new View(this);
        view.setVisibility(View.GONE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE);
        lp.windowAnimations = 0;
        ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(view, lp);
        cover = view;
    }

    @Override
    public void onDestroy() {
        if (debugEnabled()) debug("guard-destroyed");
        handler.removeCallbacks(uncover);
        handler.removeCallbacks(launch);
        handler.removeCallbacks(keyLaunch);
        handler.removeCallbacks(recovery);
        recoveryActive = false;
        if (cover != null) ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(cover);
        if (running == this) running = null;
        super.onDestroy();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (debugEnabled()) {
            debug("key-event key=" + KeyEvent.keyCodeToString(event.getKeyCode())
                    + " action=" + (event.getAction() == KeyEvent.ACTION_DOWN ? "down" : "up")
                    + " repeat=" + event.getRepeatCount() + " eventTime=" + event.getEventTime()
                    + " deviceId=" + event.getDeviceId() + " source=0x"
                    + Integer.toHexString(event.getSource()) + " flags=0x"
                    + Integer.toHexString(event.getFlags()));
        }
        if (event.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (debugEnabled()) debug("key-home consumed=true");
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            paused = false;
            stopRecovery("new-home-press");
            handler.removeCallbacks(keyLaunch);
            handler.post(keyLaunch);
        }
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = e.getPackageName(), cls = e.getClassName();
        if (pkg == null) return;
        if ((AMAZON.contentEquals(pkg) || getPackageName().contentEquals(pkg)) && debugEnabled()) {
            debug("window package=" + pkg + " class=" + cls);
        }
        if (!AMAZON.contentEquals(pkg)) {
            if (!GuardRecoveryPolicy.shouldCancelForWindow(
                    getPackageName(), pkg.toString(), cls == null ? null : cls.toString(), AMAZON,
                    getPackageName() + ".Home", View.class.getName())) {
                if (getPackageName().contentEquals(pkg) && debugEnabled()) {
                    debug("own-package-window-ignored class=" + cls);
                }
                return;
            }
            paused = false;
            stopRecovery("foreground-left-amazon package=" + pkg);
            arrived(this);
            return;
        }
        if (paused) {
            if (debugEnabled()) debug("amazon-home-ignored reason=paused class=" + cls);
            return;
        }
        if (cls == null || !AMAZON_HOME.contentEquals(cls)) {
            if (debugEnabled()) debug("amazon-window-ignored reason=not-home class=" + cls);
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastLaunch < REPEAT_MS) {
            if (debugEnabled()) debug("amazon-home-ignored reason=debounced elapsed=" + (now - lastLaunch));
            return;
        }
        lastLaunch = now;
        launchWhenCovered();
    }

    /**
     * Shows the cover, then starts the launch once that window is visible to the window manager.
     * A cover that is already on screen (a repeat within {@link #COVER_MAX_MS}) gets no new draw
     * pass, so the launch goes out at once; otherwise it waits for the first pre-draw, which runs
     * after the window's relayout. A short fallback covers a window that never draws.
     */
    private void launchWhenCovered() {
        handler.removeCallbacks(launch);
        if (pendingPredraw != null) {
            ViewTreeObserver observer = cover == null ? null : cover.getViewTreeObserver();
            if (observer != null && observer.isAlive()) observer.removeOnPreDrawListener(pendingPredraw);
            pendingPredraw = null;
        }
        if (debugEnabled()) debug("redirect-attempt cover="
                + (cover == null ? "missing" : cover.getVisibility() == View.VISIBLE ? "visible" : "hidden"));
        if (cover == null) {
            dispatchHome("cover-missing");
            return;
        }
        boolean shown = cover.getVisibility() == View.VISIBLE;
        cover();
        if (shown) {
            dispatchHome("cover-already-visible");
            return;
        }
        final ViewTreeObserver observer = cover.getViewTreeObserver();
        pendingPredraw = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (pendingPredraw != this) {
                    if (debugEnabled()) debug("cover-predraw-ignored reason=stale");
                    return true;
                }
                pendingPredraw = null;
                cover.getViewTreeObserver().removeOnPreDrawListener(this);
                handler.removeCallbacks(launch);
                if (debugEnabled()) debug("cover-predraw-ready");
                dispatchHome("cover-predraw");
                return true;
            }
        };
        observer.addOnPreDrawListener(pendingPredraw);
        handler.postDelayed(launch, LAUNCH_FALLBACK_MS);
    }

    private void dispatchHome(String reason) {
        if (debugEnabled()) debug("home-dispatch reason=" + reason);
        home(this);
        recoveryRetries = 0;
        recoveryActive = !paused;
        handler.removeCallbacks(recovery);
        if (recoveryActive) {
            handler.postDelayed(recovery, GuardRecoveryPolicy.delayMs(1));
            if (debugEnabled()) debug("recovery-watchdog scheduled retry=1 delay="
                    + GuardRecoveryPolicy.delayMs(1));
        }
    }

    private void retryHome() {
        if (!GuardRecoveryPolicy.shouldRetry(recoveryRetries, paused, recoveryActive)) {
            stopRecovery("cancelled-before-retry");
            return;
        }
        recoveryRetries++;
        if (debugEnabled()) debug("recovery-retry attempt=" + recoveryRetries);
        home(this);
        if (GuardRecoveryPolicy.shouldRetry(recoveryRetries, paused, recoveryActive)) {
            long delay = GuardRecoveryPolicy.delayMs(recoveryRetries + 1);
            handler.postDelayed(recovery, delay);
            if (debugEnabled()) debug("recovery-watchdog scheduled retry="
                    + (recoveryRetries + 1) + " delay=" + delay);
        } else {
            recoveryActive = false;
            if (debugEnabled()) debug("recovery-exhausted retries=" + recoveryRetries);
        }
    }

    private void stopRecovery(String reason) {
        handler.removeCallbacks(recovery);
        handler.removeCallbacks(launch);
        if (pendingPredraw != null) {
            ViewTreeObserver observer = cover == null ? null : cover.getViewTreeObserver();
            if (observer != null && observer.isAlive()) observer.removeOnPreDrawListener(pendingPredraw);
            pendingPredraw = null;
            if (debugEnabled()) debug("cover-predraw-cancelled reason=" + reason);
        }
        if (recoveryActive && debugEnabled()) debug("recovery-stopped reason=" + reason
                + " retries=" + recoveryRetries);
        recoveryActive = false;
    }

    /** The overlay window exists from service start; a GONE root keeps its surface hidden. */
    private void cover() {
        if (cover == null) return;
        cover.setBackgroundColor(Config.get(this).background());
        cover.setVisibility(View.VISIBLE);
        handler.removeCallbacks(uncover);
        handler.postDelayed(uncover, COVER_MAX_MS);
    }

    private void uncover() {
        handler.removeCallbacks(uncover);
        if (cover != null) cover.setVisibility(View.GONE);
    }

    @Override
    public void onInterrupt() {}
}
