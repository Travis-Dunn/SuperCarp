package production.sprite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;

public final class SpriteAtlasFileParser {
    private static final String ATLASES_DIR = "atlases";
    public static final int MAX_ATLAS_IDX = 0xFFFF;
    public static final int MAX_SIZE = 0x7FFF;

    public static SpriteAtlas FromFile(String filename, int spriteSize,
                                       SpritePalette palette) {
        assert(filename != null && !filename.isEmpty());

        String p = "/" + ATLASES_DIR + "/" + filename;

        if (!filename.toLowerCase().endsWith(".png")) {
            LogFatalAndExit(ErrStrInvalidExtension(filename));
            return null;
        }

        try (InputStream stream =
                     SpriteAtlasFileParser.class.getResourceAsStream(p)) {
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
        int w, h, spriteCount, spritesPerRow, y, x, c, idx;
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
            LogFatalAndExit(ErrStrDimOOB(f, "width"));
            return null;
        }

        if (h == 0) {
            LogFatalAndExit(ErrStrDimOOB(f, "height"));
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

        if (w > MAX_SIZE) {
            LogFatalAndExit(ErrStrImgTooLarge(f, w));
            return null;
        }

        spritesPerRow = w / spriteSize;
        spriteCount = spritesPerRow * spritesPerRow;

        if (spriteCount > MAX_ATLAS_IDX + 1) {
            LogFatalAndExit(ErrStrTooManySprites(f, spriteCount));
            return null;
        }

        if (palette == null) {
            LogFatalAndExit(ErrStrPaletteNull(f));
            return null;
        }

        data = new byte[w * h];

        for (y = 0; y < h; ++y) {
            for (x = 0; x < w; ++x) {
                if ((idx = palette.getIndex(c = image.getRGB(x, y))) == -1) {
                    LogFatalAndExit(ErrStrImgHasNonPaletteColor(f, x, y, c));
                    return null;
                }

                /* i think this is broken for indices > 127 */
                data[y * w + x] = (byte)idx;
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

    private static String ErrStrDimOOB(String filename, String dim) {
        return String.format("%s rejected [%s]. Image has zero [%s].\n", CLASS,
                filename);
    }

    private static String ErrStrImgNotSquare(String filename) {
        return String.format("%s rejected [%s]. Image must be square.\n");
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
                " maximum [%d].\n", CLASS, filename, w, MAX_SIZE);
    }

    private static String ErrStrTooManySprites(String filename, int c) {
        return String.format("%s rejected [%s]. Sprite count [%d] exceeds the" +
                " maximum [%d].\n", CLASS, filename, c, MAX_ATLAS_IDX + 1);
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
}