package whitetail.loaders;

import whitetail.graphics.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class TextureFileParser {
    private static final String TEXTURES_DIR = "textures";

    public static Texture FromFile(String filename) {
        String path = "/" + TEXTURES_DIR + "/" + filename;
        InputStream stream = TextureFileParser.class.getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Texture not found: " + filename);
        }
        return FromStream(stream, filename);
    }

    private static Texture FromStream(InputStream stream, String filename) {
        try {
            BufferedImage image = ImageIO.read(stream);
            int w = image.getWidth();
            int h = image.getHeight();
            if (!(w == h) || !(w > 0 && (w & (w - 1)) == 0)) {
                System.out.println("Texture is haram");
            }

            boolean hasAlpha = image.getColorModel().hasAlpha();
            int bpp = hasAlpha ? 4 : 3;

            byte data[] = new byte[ w * h * bpp];

            for (int t = 0; t < h; t++) {
                for (int s = 0; s < w; s++) {
                    int argb = image.getRGB(s, t);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8 ) & 0xFF;
                    int b =  argb        & 0xFF;
                    int flipped = h - 1 - t;
                    int i = (flipped * w + s) * bpp;
                    data[i + 0] = (byte)r;
                    data[i + 1] = (byte)g;
                    data[i + 2] = (byte)b;
                    if (hasAlpha) data[i + 3] = (byte)a;
                }
            }
            return new Texture(data, w, h, hasAlpha);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture resource "
                    + e.getMessage());
        } finally { try { stream.close(); } catch (IOException ignored) {} }
    }
}
