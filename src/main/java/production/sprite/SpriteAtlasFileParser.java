package production.sprite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;
import static whitetail.utility.logging.ErrorStrings
        .ERR_STR_FAILED_BUILD_FROM_FILE_OOM;

public final class SpriteAtlasFileParser {
    private static final String ATLASES_DIR = "atlases";

    public static SpriteAtlas FromFile(String filename, int spriteSize,
            SpritePalette palette) {
        String p = "/" + ATLASES_DIR + "/" + filename;

        assert(filename != null && !filename.isEmpty());

        if (!filename.toLowerCase().endsWith(".png")) {
            LogFatalAndExit(ErrStrInvalidExtension(filename));
            return null;
        }

        try (InputStream stream = SpriteAtlasFileParser.class
                .getResourceAsStream(p)) {
            if (stream == null) {
                LogFatalAndExit(ErrStrFailedLoad(filename));
                return null;
            }
            return FromStream(stream, filename, spriteSize, palette);
        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(filename), e);
            return null;
        }
    }

    private static SpriteAtlas FromStream(InputStream s, String f,
            int spriteSize, SpritePalette palette) {
        BufferedImage image = null;
        int w, h, spriteCount, spritesPerRow, y, x, c, idx, alpha;
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

        if (!(h == w)) {
            LogFatalAndExit(ErrStrImgNotSquare(f));
            return null;
        }

        if (!((w & (w - 1)) == 0)) {
            LogFatalAndExit(ErrStrImgNotPow2(f));
            return null;
        }

        if (!(w % spriteSize == 0)) {
            LogFatalAndExit(ErrStrImgNotMultipleSpriteSize(f, spriteSize));
            return null;
        }

        if (w > SpriteAtlas.MAX_SIZE) {
            LogFatalAndExit(ErrStrImgTooLarge(f, w));
            return null;
        }

        spritesPerRow = w / spriteSize;
        spriteCount = spritesPerRow * spritesPerRow;

        if (spriteCount > SpriteAtlas.MAX_IDX + 1) {
            LogFatalAndExit(ErrStrTooManySprites(f, spriteCount));
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

                if (0 == alpha) {
                    data[y * w + x] = SpritePalette.TRANSPARENT_IDX;
                } else if (255 != alpha) {
                    LogFatalAndExit(ErrStrPartialAlpha(f, x, y, alpha));
                    return null;
                } else if ((idx = palette.getIndex(c)) == -1) {
                    LogFatalAndExit(ErrStrImgHasNonPaletteColor(f, x, y, c));
                    return null;
                } else {
                    data[y * w + x] = (byte)idx;
                }
            }
        }

        return new SpriteAtlas(palette, spriteSize, data, spritesPerRow,
                spriteCount);
    }

    private static final String CLASS =
            SpriteAtlasFileParser.class.getSimpleName();

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

    private static String ErrStrImgNotSquare(String filename) {
        return String.format("%s rejected [%s]. Image must be square.\n", CLASS,
                filename);
    }

    private static String ErrStrImgNotPow2(String filename) {
        return String.format("%s rejected [%s]. Image resolution must " +
                "be a power of 2.\n", CLASS, filename);
    }

    private static String ErrStrImgNotMultipleSpriteSize(String f, int s) {
        return String.format("%s rejected [%s]. Image resolution must be a " +
                "multiple of sprite size [%d].\n", CLASS, f, s);
    }

    private static String ErrStrImgTooLarge(String filename, int w) {
        return String.format("%s rejected [%s]. Image resolution [%d] exceeds" +
                " maximum [%d].\n", CLASS, filename, w, SpriteAtlas.MAX_SIZE);
    }

    private static String ErrStrTooManySprites(String filename, int c) {
        return String.format("%s rejected [%s]. Sprite count [%d] exceeds the" +
                " maximum [%d].\n", CLASS, filename, c, SpriteAtlas.MAX_IDX + 1);
    }

    private static String ErrStrPaletteNull(String filename) {
        return String.format("%s rejected [%s]. Palette arg was null.\n", CLASS,
                filename);
    }

    private static String ErrStrImgHasNonPaletteColor(String filename,
                                                      int x, int y, int c) {
        return String.format("%s rejected [%s]. Texel [%d, %d] is [0x%08X], " +
                "which isn't in the palette.\n", CLASS, filename, x, y, c);
    }

    private static String ErrStrPartialAlpha(String filename, int x, int y,
            int a) {
        return String.format("%s rejected [%s]. Texel [%d, %d] has partial " +
                "alpha [%d]. Only fully opaque (255) or fully transparent " +
                "(0) pixels are allowed.\n", CLASS, filename, x, y, a);
    }

    /* TODO: scrub the rest! */
    /**
     * many things to do.
     *
     * scrub the rest of the files (SpriteAtlas and SpriteAtlasFileParser done)
     *
     * fix mapper so that you don't have to press P to repaint the left panel
     *
     * add an eyedropper to mapper
     *
     * Make SpriteAtlasFileParser stop when it encounters a transparent pixel?
     * What I need is to handle the blank area, how we do that depends on
     * whether or not we adopt the "black is transparent" model. May be what we
     * want to do for this project, but is it what we want for the lib?
     *
     * also makes sense to go ahead and make the final version of atlas 0, which
     * is the one that we'll use for making the maps.
     *
     * at some point, we'll want to add to Mapper the ability to stamp down
     * "potentially interactable" sprites, although I'm not sure if we want them
     * to be drawn in place of the existing map sprite, or on top of.
     *
     * I do know that we'll want the ability to draw more than one sprite on top
     */
}