package production.save;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

/**
 * Manages background saving of game state.
 *
 * Save requests are processed on a dedicated thread. If a save is in progress
 * when a new request arrives, the request is skipped. If too many consecutive
 * saves are skipped, the game terminates with a fatal error.
 *
 * Uses atomic temp-file-then-rename to prevent corruption on crash.
 */
public final class SaveManager {
    private static boolean init;

    private static Thread saveThread;
    private static volatile boolean running;
    private static volatile boolean saveInProgress;
    private static volatile SaveData pendingSave;
    private static final Object saveLock = new Object();
    private static int consecutiveSkips;

    private static final int MAX_CONSECUTIVE_SKIPS = 10;
    private static final int MAGIC = 0x53435250;  // "SCRP"
    private static final short VERSION = 2;
    private static final String SAVE_DIR_NAME = ".supercarp";
    private static final String SAVE_FILE_NAME = "save.dat";
    private static final String TEMP_FILE_NAME = "save.dat.tmp";

    private static File saveDir;
    private static File saveFile;
    private static File tempFile;

    private SaveManager() {}

    public static boolean Init() {
        assert(!init);

        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isEmpty()) {
            LogFatalAndExit(ErrStrNoUserHome());
            return false;
        }

        saveDir = new File(userHome, SAVE_DIR_NAME);
        if (!saveDir.exists() && !saveDir.mkdirs()) {
            LogFatalAndExit(ErrStrFailedCreateDir(saveDir.getAbsolutePath()));
            return false;
        }

        saveFile = new File(saveDir, SAVE_FILE_NAME);
        tempFile = new File(saveDir, TEMP_FILE_NAME);

        /* clean up stray temp file from a previous crash */
        if (tempFile.exists()) {
            tempFile.delete();
        }

        consecutiveSkips = 0;
        saveInProgress = false;
        pendingSave = null;
        running = true;

        saveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                saveLoop();
            }
        }, "SaveThread");
        saveThread.setDaemon(true);
        saveThread.start();

        return init = true;
    }

    /**
     * Request a save of the given game state.
     * If a save is currently in progress, the request is skipped.
     * Call this from the main thread each tick.
     */
    public static void RequestSave(SaveData data) {
        assert(init);

        synchronized (saveLock) {
            if (saveInProgress) {
                consecutiveSkips++;
                if (consecutiveSkips >= MAX_CONSECUTIVE_SKIPS) {
                    LogFatalAndExit(ErrStrTooManySkips(consecutiveSkips));
                }
                return;
            }
            pendingSave = data;
            saveLock.notify();
        }
    }

    private static void saveLoop() {
        while (running) {
            SaveData dataToSave = null;

            synchronized (saveLock) {
                while (pendingSave == null && running) {
                    try {
                        saveLock.wait();
                    } catch (InterruptedException e) {
                        if (!running) return;
                    }
                }
                if (pendingSave != null) {
                    dataToSave = pendingSave;
                    pendingSave = null;
                    saveInProgress = true;
                }
            }

            if (dataToSave != null) {
                try {
                    writeSaveFile(dataToSave);
                    consecutiveSkips = 0;
                } catch (IOException e) {
                    LogFatalAndExit(ErrStrFailedWrite(e.getMessage()));
                } finally {
                    saveInProgress = false;
                }
            }
        }
    }

    private static void writeSaveFile(SaveData data) throws IOException {
        FileOutputStream fos = null;
        DataOutputStream dos = null;

        try {
            fos = new FileOutputStream(tempFile);
            dos = new DataOutputStream(new BufferedOutputStream(fos));

            /* header */
            dos.writeInt(MAGIC);
            dos.writeShort(VERSION);

            /* player data */
            dos.writeInt(data.playerTileX);
            dos.writeInt(data.playerTileY);

            /* flush and sync to disk */
            dos.flush();
            fos.getFD().sync();
        } finally {
            if (dos != null) {
                try { dos.close(); } catch (IOException ignored) {}
            } else if (fos != null) {
                try { fos.close(); } catch (IOException ignored) {}
            }
        }

        /* atomic rename */
        if (saveFile.exists() && !saveFile.delete()) {
            throw new IOException("Failed to delete old save file");
        }
        if (!tempFile.renameTo(saveFile)) {
            throw new IOException("Failed to rename temp file to save file");
        }
    }

    /**
     * Load game state from save file.
     * Returns null if no save file exists.
     */
    public static SaveData Load() {
        assert(init);

        if (!saveFile.exists()) {
            return null;
        }

        FileInputStream fis = null;
        DataInputStream dis = null;

        try {
            fis = new FileInputStream(saveFile);
            dis = new DataInputStream(new BufferedInputStream(fis));

            int magic = dis.readInt();
            if (magic != MAGIC) {
                LogFatalAndExit(ErrStrInvalidMagic(magic));
                return null;
            }

            short version = dis.readShort();
            /*
            if (version != VERSION) {
                LogFatalAndExit(ErrStrInvalidVersion(version));
                return null;
            }
             */
            if (version < VERSION) {
                // Outdated save from previous build, ignore it
                return null;
            }
            if (version > VERSION) {
                // Save from future version, that's actually a problem
                LogFatalAndExit(ErrStrInvalidVersion(version));
                return null;
            }

            int playerTileX = dis.readInt();
            int playerTileY = dis.readInt();

            return new SaveData(playerTileX, playerTileY);

        } catch (IOException e) {
            LogFatalAndExit(ErrStrFailedRead(e.getMessage()));
            return null;
        } finally {
            if (dis != null) {
                try { dis.close(); } catch (IOException ignored) {}
            } else if (fis != null) {
                try { fis.close(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Check if a save file exists.
     */
    public static boolean HasSaveFile() {
        assert(init);
        return saveFile.exists();
    }

    /**
     * Delete the save file. Returns true if deleted or didn't exist.
     */
    public static boolean DeleteSaveFile() {
        assert(init);
        if (!saveFile.exists()) return true;
        return saveFile.delete();
    }

    public static void Shutdown() {
        assert(init);

        running = false;
        synchronized (saveLock) {
            saveLock.notify();
        }

        try {
            saveThread.join(1000);
        } catch (InterruptedException ignored) {}

        saveThread = null;
        init = false;
    }

    public static boolean IsInitialized() {
        return init;
    }

    /* --- error strings --- */

    private static final String CLASS = SaveManager.class.getSimpleName();

    private static String ErrStrNoUserHome() {
        return String.format("%s failed to determine user home directory.\n",
                CLASS);
    }

    private static String ErrStrFailedCreateDir(String path) {
        return String.format("%s failed to create save directory [%s].\n",
                CLASS, path);
    }

    private static String ErrStrTooManySkips(int skips) {
        return String.format("%s detected [%d] consecutive skipped saves. " +
                "The disk may be too slow or unresponsive.\n", CLASS, skips);
    }

    private static String ErrStrFailedWrite(String msg) {
        return String.format("%s failed to write save file: %s\n", CLASS, msg);
    }

    private static String ErrStrFailedRead(String msg) {
        return String.format("%s failed to read save file: %s\n", CLASS, msg);
    }

    private static String ErrStrInvalidMagic(int magic) {
        return String.format("%s found invalid magic number [0x%08X] in save " +
                "file. The file may be corrupted.\n", CLASS, magic);
    }

    private static String ErrStrInvalidVersion(short version) {
        return String.format("%s found unsupported save file version [%d]. " +
                "This save may be from a newer game version.\n", CLASS, version);
    }
}