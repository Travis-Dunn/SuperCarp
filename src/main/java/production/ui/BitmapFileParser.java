package production.ui;

import production.sprite.SpritePalette;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_BUILD_FROM_FILE_OOM;

/**
 * Loads PNG images and converts them to palette-indexed Bitmaps.
 */
public final class BitmapFileParser {
    private static final String BITMAPS_DIR = "bitmaps";

    public static Bitmap FromFile(String filename, SpritePalette palette) {
        assert(filename != null && !filename.isEmpty());

        String p = "/" + BITMAPS_DIR + "/" + filename;

        if (!filename.toLowerCase().endsWith(".png")) {
            LogFatalAndExit(ErrStrInvalidExtension(filename));
            return null;
        }

        try (InputStream stream = BitmapFileParser.class.getResourceAsStream(p)) {
            if (stream == null) {
                LogFatalAndExit(ErrStrFailedLoad(filename));
                return null;
            }
            return FromStream(stream, filename, palette);
        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(filename), e);
            return null;
        }
    }

    private static Bitmap FromStream(InputStream s, String f,
                                     SpritePalette palette) {
        BufferedImage image;
        int w, h, x, y, c, idx, alpha;
        byte[] data;

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

        if (w == 0) {
            LogFatalAndExit(ErrStrDimZero(f, "width"));
            return null;
        }

        if (h == 0) {
            LogFatalAndExit(ErrStrDimZero(f, "height"));
            return null;
        }

        if (palette == null) {
            LogFatalAndExit(ErrStrPaletteNull(f));
            return null;
        }

        try {
            data = new byte[w * h];
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_BUILD_FROM_FILE_OOM);
            return null;
        }

        for (y = 0; y < h; ++y) {
            for (x = 0; x < w; ++x) {
                c = image.getRGB(x, y);
                alpha = (c >> 24) & 0xFF;

                if (alpha == 0) {
                    data[y * w + x] = SpritePalette.TRANSPARENT_IDX;
                } else if (alpha != 255) {
                    LogFatalAndExit(ErrStrPartialAlpha(f, x, y, alpha));
                    return null;
                } else if ((idx = palette.getIndex(c)) == -1) {
                    LogFatalAndExit(ErrStrNonPaletteColor(f, x, y, c));
                    return null;
                } else {
                    data[y * w + x] = (byte) idx;
                }
            }
        }

        return new Bitmap(w, h, data, palette);
    }

    private static final String CLASS = BitmapFileParser.class.getSimpleName();

    private static String ErrStrInvalidExtension(String filename) {
        return String.format("%s rejected file [%s]. Only .png files are " +
                "allowed.\n", CLASS, filename);
    }

    private static String ErrStrFailedLoad(String filename) {
        return String.format("%s failed to load file [%s].\n", CLASS, filename);
    }

    private static String ErrStrNotAnImage(String filename) {
        return String.format("%s failed to decode [%s]. The file is not a " +
                "valid image.\n", CLASS, filename);
    }

    private static String ErrStrDimZero(String filename, String dim) {
        return String.format("%s rejected [%s]. Image has zero [%s].\n", CLASS,
                filename, dim);
    }

    private static String ErrStrPaletteNull(String filename) {
        return String.format("%s rejected [%s]. Palette arg was null.\n", CLASS,
                filename);
    }

    private static String ErrStrNonPaletteColor(String filename,
                                                int x, int y, int c) {
        return String.format("%s rejected [%s]. Pixel [%d, %d] is [0x%08X], " +
                "which isn't in the palette.\n", CLASS, filename, x, y, c);
    }

    private static String ErrStrPartialAlpha(String filename, int x, int y,
                                             int a) {
        return String.format("%s rejected [%s]. Pixel [%d, %d] has partial " +
                "alpha [%d]. Only fully opaque (255) or fully transparent " +
                "(0) pixels are allowed.\n", CLASS, filename, x, y, a);
    }
}