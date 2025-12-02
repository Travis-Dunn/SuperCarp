package whitetail.utility;

import whitetail.utility.logging.LogLevel;

import static whitetail.utility.logging.Logger.LogSession;
import static whitetail.utility.logging.Logger.LogSessionNoTime;

public final class FramerateManager {
    public static long targetSleepTime;            /* ns               */
    public static long mark;                       /* ns               */
    public static float deltaTime;                  /* seconds          */
    public static int realFramerate;              /* frames/second    */
    public static float realFrameTime;              /* ms               */
    public static int framerate;                  /* frames/seconds   */
    public static long targetDur;                  /* ns               */
    public static boolean init = false;
    public static boolean pastFirstFrame;

    public static long beforeSwap, afterSwap;

    public static long lastPrintTime;

    public static final int defaultFPS = 60;

    private static long nanos;

    public static boolean Init(int targetFramerate) {
        assert (!init);

        targetDur = targetFramerate > 0 ? (long) (1e9 / targetFramerate) : 0;
        System.out.println("targetDur set to [" + targetDur + "] nanos");
        pastFirstFrame = false;
        lastPrintTime = 0;

        return init = true;
    }

    public static long CurrentTimeNanos() { return nanos; }

    public static void Update() {
        assert(init);

        nanos = System.nanoTime();

        if (!pastFirstFrame) {
            mark = nanos;
            pastFirstFrame = true;
            return;
        }

        // Sleep if needed
        if (targetSleepTime > 0) {
            LogSession(LogLevel.DEBUG, "Sleeping for [" + targetSleepTime / 1e6 + "]");
            try {
                Thread.sleep(targetSleepTime / 1_000_000L); // ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long now = System.nanoTime();
        long dur = now - mark;
        mark = now;

        // Seconds per frame
        deltaTime = (float)(dur / 1e9);
        framerate = (int)(1.0f / deltaTime);
        LogSessionNoTime("totalMs," + String.valueOf((float)(dur / 1e6)));
        LogSessionNoTime("swapMs," + String.valueOf((float)((afterSwap - beforeSwap) / 1e6)));

        // Actual frame time in ms (including sleep)
        realFrameTime = dur / 1_000_000.0f;

        // Actual FPS
        realFramerate = (int)(1000.0 / realFrameTime);

        // Compute how long to sleep next time
        long signedSleepTime = targetDur - dur;
        long lastSleepTime = targetSleepTime;
        targetSleepTime = Math.max(signedSleepTime, 0);

        // Optional: print once per second
        if (now - lastPrintTime > 1_000_000_000L) {
            lastPrintTime = now;
        }
    }
}
