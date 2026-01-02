package production;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public class EntryPoint {
    public static void main(String args[]) {
        System.out.println("Platform: " + Platform.getName());

        SuperCarpEngine engine = new SuperCarpEngine();

        if (!engine.initFirstHalf()) {
            System.err.println("Engine failed to init first half");
            return;
        }
        if (!engine.initSecondHalf("SuperCarp dev build", 1280, 960,
                0, true, false, 0.6)) {
            LogFatalAndExit("Engine failed to init second half");
            return;
        }


        engine.run();
    }
}
