package production.ui;

import production.sprite.SpritePalette;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;

/**
 * Parses BMFont text format (.fnt) and accompanying atlas image.
 */
public final class FontAtlasFileParser {
    private static final String FONTS_DIR = "fonts";
    private static final int MAX_CHAR = 256;

    /**
     * Load a BMFont from .fnt file.
     * Expects the atlas PNG to be in the same directory.
     *
     * @param fntFilename the .fnt file name (e.g., "dialogue.fnt")
     * @param palette palette for color mapping (uses index 1 for foreground)
     * @param fgIndex palette index to use for glyph pixels
     */
    public static FontAtlas FromFile(String fntFilename, SpritePalette palette,
                                     int fgIndex) {
        assert(fntFilename != null && !fntFilename.isEmpty());
        assert(palette != null);

        String p = "/" + FONTS_DIR + "/" + fntFilename;

        if (!fntFilename.toLowerCase().endsWith(".fnt")) {
            LogFatalAndExit(ErrStrInvalidExtension(fntFilename));
            return null;
        }

        try {
            InputStream stream = FontAtlasFileParser.class.getResourceAsStream(p);
            if (stream == null) {
                LogFatalAndExit(ErrStrFailedLoad(fntFilename));
                return null;
            }
            try {
                return FromStream(stream, fntFilename, palette, fgIndex);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(fntFilename), e);
            return null;
        }
    }

    private static FontAtlas FromStream(InputStream s, String fntFilename,
                                        SpritePalette palette, int fgIndex)
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(s));

        int lineHeight = 0;
        int base = 0;
        int scaleW = 0;
        int scaleH = 0;
        String atlasFilename = null;
        FontAtlas.Glyph[] glyphs = new FontAtlas.Glyph[MAX_CHAR];

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("common ")) {
                lineHeight = parseIntField(line, "lineHeight");
                base = parseIntField(line, "base");
                scaleW = parseIntField(line, "scaleW");
                scaleH = parseIntField(line, "scaleH");
            }
            else if (line.startsWith("page ")) {
                atlasFilename = parseStringField(line, "file");
            }
            else if (line.startsWith("char ")) {
                int id = parseIntField(line, "id");
                if (id >= 0 && id < MAX_CHAR) {
                    glyphs[id] = new FontAtlas.Glyph(
                            parseIntField(line, "x"),
                            parseIntField(line, "y"),
                            parseIntField(line, "width"),
                            parseIntField(line, "height"),
                            parseIntField(line, "xoffset"),
                            parseIntField(line, "yoffset"),
                            parseIntField(line, "xadvance")
                    );
                }
            }
        }

        if (atlasFilename == null || scaleW == 0 || scaleH == 0) {
            LogFatalAndExit(ErrStrMissingFields(fntFilename));
            return null;
        }

        /* load atlas image */
        byte[] pixels = loadAtlasImage(fntFilename, atlasFilename,
                scaleW, scaleH, fgIndex);
        if (pixels == null) {
            return null;
        }

        return new FontAtlas(lineHeight, base, scaleW, scaleH, pixels, glyphs);
    }

    private static byte[] loadAtlasImage(String fntFilename, String atlasFilename,
                                         int expectedW, int expectedH, int fgIndex) {
        /* atlas is in same directory as .fnt file */
        String p = "/" + FONTS_DIR + "/" + atlasFilename;

        try {
            InputStream stream = FontAtlasFileParser.class.getResourceAsStream(p);
            if (stream == null) {
                LogFatalAndExit(ErrStrFailedLoadAtlas(fntFilename, atlasFilename));
                return null;
            }

            BufferedImage image;
            try {
                image = ImageIO.read(stream);
            } finally {
                stream.close();
            }

            if (image == null) {
                LogFatalAndExit(ErrStrNotAnImage(atlasFilename));
                return null;
            }

            int w = image.getWidth();
            int h = image.getHeight();

            if (w != expectedW || h != expectedH) {
                LogFatalAndExit(ErrStrDimensionMismatch(atlasFilename,
                        expectedW, expectedH, w, h));
                return null;
            }

            byte[] pixels = new byte[w * h];

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = (argb >> 24) & 0xFF;

                    /* treat any non-transparent pixel as foreground */
                    if (alpha > 127) {
                        pixels[y * w + x] = (byte) fgIndex;
                    } else {
                        pixels[y * w + x] = SpritePalette.TRANSPARENT_IDX;
                    }
                }
            }

            return pixels;

        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoadAtlas(fntFilename, atlasFilename), e);
            return null;
        }
    }

    /**
     * Parse an integer field from a BMFont line.
     * Format: "fieldName=123"
     */
    private static int parseIntField(String line, String field) {
        String search = field + "=";
        int start = line.indexOf(search);
        if (start == -1) return 0;

        start += search.length();
        int end = start;
        while (end < line.length() &&
                (Character.isDigit(line.charAt(end)) || line.charAt(end) == '-')) {
            end++;
        }

        if (end == start) return 0;

        try {
            return Integer.parseInt(line.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parse a quoted string field from a BMFont line.
     * Format: fieldName="value"
     */
    private static String parseStringField(String line, String field) {
        String search = field + "=\"";
        int start = line.indexOf(search);
        if (start == -1) return null;

        start += search.length();
        int end = line.indexOf('"', start);
        if (end == -1) return null;

        return line.substring(start, end);
    }

    /* --- error strings --- */

    private static final String CLASS = FontAtlasFileParser.class.getSimpleName();

    private static String ErrStrInvalidExtension(String filename) {
        return String.format("%s rejected file [%s]. Only .fnt files are " +
                "allowed.\n", CLASS, filename);
    }

    private static String ErrStrFailedLoad(String filename) {
        return String.format("%s failed to load file [%s].\n", CLASS, filename);
    }

    private static String ErrStrMissingFields(String filename) {
        return String.format("%s rejected [%s]. Missing required fields in " +
                "font definition.\n", CLASS, filename);
    }

    private static String ErrStrFailedLoadAtlas(String fntFile, String atlasFile) {
        return String.format("%s failed to load atlas [%s] for font [%s].\n",
                CLASS, atlasFile, fntFile);
    }

    private static String ErrStrNotAnImage(String filename) {
        return String.format("%s failed to decode [%s]. Not a valid image.\n",
                CLASS, filename);
    }

    private static String ErrStrDimensionMismatch(String filename,
                                                  int expectedW, int expectedH, int actualW, int actualH) {
        return String.format("%s rejected [%s]. Expected [%dx%d] but image is " +
                        "[%dx%d].\n", CLASS, filename, expectedW, expectedH,
                actualW, actualH);
    }
}