package production;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public class EntryPoint {
    public static void main(String args[]) {
        SuperCarpEngine engine = new SuperCarpEngine();

        if (!engine.initFirstHalf()) {
            System.err.println("Engine failed to init first half");
            return;
        }
        if (!engine.initSecondHalf("SuperCarp dev build", 960, 720,
                0, true, false)) {
            LogFatalAndExit("Engine failed to init second half");
            return;
        }


        engine.run();
    }
}
