package whitetail.utility;

import whitetail.utility.logging.LogLevel;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public final class GCPriest {
    private static List<GarbageCollectorMXBean> gcBeans;
    private static long lastCollectionCounts[];
    private static long lastCollectionTimes[];

    private static boolean init;
    private static final String ERR_STR_FAILED_INIT;

    private GCPriest() {}

    public static boolean Init() {
        assert(!init);

        int i, size;

        try {
            gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            size = gcBeans.size();
            lastCollectionCounts = new long[size];
            lastCollectionTimes = new long[size];

            for (i = 0; i < size; i++) {
                lastCollectionCounts[i] = gcBeans.get(i).getCollectionCount();
                lastCollectionTimes[i] = gcBeans.get(i).getCollectionTime();

            }
            return init = true;
        } catch (Exception e) {
            LogFatalExcpAndExit(ERR_STR_FAILED_INIT, e);
            return init = false;
        }
    }

    static {
        init = false;
        ERR_STR_FAILED_INIT = GCPriest.class.getSimpleName() +
                " failed to initialize because an exception was encountered.\n";
    }

    public static void CheckAndLogGCActivity() {
        assert(init);

        int i, size;
        GarbageCollectorMXBean bean;
        long currentCount, currentTime, collections, duration;

        size = gcBeans.size();
        for (i = 0; i < size; i++) {
            bean = gcBeans.get(i);
            currentCount = bean.getCollectionCount();
            currentTime = bean.getCollectionTime();

            if (currentCount > lastCollectionCounts[i]) {
                collections = currentCount - lastCollectionCounts[i];
                duration = currentTime - lastCollectionTimes[i];

                LogSession(LogLevel.DEBUG, "[GC] " + bean.getName() +
                        " - Collections: " + collections +
                        " - Duration: " + duration + "ms\n");

                lastCollectionCounts[i] = currentCount;
                lastCollectionTimes[i] = currentTime;
            }
        }
    }
}
