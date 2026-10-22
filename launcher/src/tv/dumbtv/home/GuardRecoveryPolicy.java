package tv.dumbtv.home;

/** Bounds short HOME recovery retries so they cannot become a persistent focus poller. */
final class GuardRecoveryPolicy {
    static final int MAX_RETRIES = 2;

    private GuardRecoveryPolicy() {}

    static boolean shouldRetry(int retries, boolean paused, boolean active) {
        return !paused && active && retries < MAX_RETRIES;
    }

    static long delayMs(int retryNumber) {
        return 250L * retryNumber;
    }

    static boolean shouldCancelForWindow(String ownPackage, String packageName, String className,
            String amazonPackage, String homeActivity, String overlayClass) {
        if (packageName == null) return false;
        if (ownPackage.equals(packageName)) {
            return className != null && !homeActivity.equals(className)
                    && !overlayClass.equals(className);
        }
        return !amazonPackage.equals(packageName)
                && !"com.android.systemui".equals(packageName)
                && !"android".equals(packageName);
    }
}
