package production.sprite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;

public final class SpritePaletteFileParser {
    private static final String PALETTES_DIR = "palettes";

    public static SpritePalette FromFile(String filename) {
        assert(filename != null && !filename.isEmpty());

        String p = "/" + PALETTES_DIR + "/" + filename;

        if (!filename.toLowerCase().endsWith(".png")) {
            LogFatalAndExit(ErrStrInvalidExtension(filename));
            return null;
        }

        try (InputStream stream =
                     SpritePaletteFileParser.class.getResourceAsStream(p)) {
            if (stream == null) {
                LogFatalAndExit(ErrStrFailedLoad(filename));
                return null;
            }
            return FromStream(stream, filename);
        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(filename), e);
            return null;
        }
    }

    private static SpritePalette FromStream(InputStream s, String f) {
        BufferedImage image = null;
        int w, h, total, idx, y, x, color;
        Map<Integer, Integer> colorToIndexMap;
        int indexToColorArray[];

        try {
            image = ImageIO.read(s);
        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(f), e);
            return null;
        }

        if (image == null) {
            LogFatalAndExit(ErrStrNotAnImage(f));
            return null;
        }

        w = image.getWidth();
        h = image.getHeight();
        total = w * h;

        if (total < 1 || total > 256) {
            LogFatalAndExit(ErrStrDimOOB(f, total));
            return null;
        }

        colorToIndexMap = new HashMap<>(total);
        indexToColorArray = new int[total];

        idx = 0;

        for (y = 0; y < h; ++y) {
            for (x = 0; x < w; ++x) {
                color = image.getRGB(x, y);

                if (colorToIndexMap.containsKey(color)) {
                    LogFatalAndExit(ErrStrDuplicateColor(f, x, y, color));
                    return null;
                }

                colorToIndexMap.put(color, idx);
                indexToColorArray[idx] = color;
                idx++;
            }
        }

        return new SpritePalette(indexToColorArray, colorToIndexMap);
    }

    private static final String CLASS =
            SpritePaletteFileParser.class.getSimpleName();

    private static String ErrStrFailedLoad(String filename) {
        return String.format("%s failed to load file [%s].\n", CLASS, filename);
    }

    private static String ErrStrInvalidExtension(String filename) {
        return String.format("%s rejected file [%s]. Only .png files are " +
                "allowed.\n", CLASS, filename);
    }

    private static String ErrStrNotAnImage(String filename) {
        return String.format("%s failed to decode [%s]. The file is not a " +
                "valid image.\n", CLASS, filename);
    }

    private static String ErrStrDimOOB(String filename, int count) {
        return String.format("%s rejected [%s]. Image has %d pixels, " +
                "expected range [1 - 256].\n", CLASS, filename, count);
    }

    private static String ErrStrDuplicateColor(String filename, int x, int y,
                                               int color) {
        return String.format("%s rejected [%s]. Duplicate color [0x%08X] " +
                "found at %d,%d.\n", CLASS, filename, color, x, y);
    }
}