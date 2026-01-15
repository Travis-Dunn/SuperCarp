package production.sprite;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import whitetail.utility.logging.LogLevel;

import java.nio.ByteBuffer;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;
import static whitetail.utility.logging.Logger.LogSession;

public final class SpriteBackend {
    private static boolean init;

    private static int texID;
    private static int fbWidth;
    private static int fbHeight;
    private static ByteBuffer uploadBuffer;

    private static final int BYTES_PER_PIXEL = 4;  // RGBA

    private SpriteBackend() {}

    public static boolean Init(int framebufferWidth, int framebufferHeight) {
        assert(!init);
        assert(framebufferWidth > 0);
        assert(framebufferHeight > 0);

        fbWidth = framebufferWidth;
        fbHeight = framebufferHeight;

        try {
            uploadBuffer = BufferUtils.createByteBuffer(
                    fbWidth * fbHeight * BYTES_PER_PIXEL);
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return init = false;
        }

        texID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID);

        /* nearest-neighbor filtering for crisp pixels */
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        /* clamp to edge to avoid bleeding */
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        /* allocate texture storage */
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                fbWidth, fbHeight, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        LogSession(LogLevel.DEBUG, CLASS + " initialized with ["
                + SpriteBackend.fbWidth + "] width, [" + fbHeight
                + "] height.\n");

        return init = true;
    }

    /**
     * Upload framebuffer data and draw to screen.
     * Expects packed RGBA bytes, row-major, top-left origin.
     * Length must be exactly fbWidth * fbHeight * 4.
     */
    public static void Present(byte[] framebuffer) {
        assert(init);
        assert(framebuffer != null);
        assert(framebuffer.length == fbWidth * fbHeight * BYTES_PER_PIXEL);

        uploadBuffer.clear();
        uploadBuffer.put(framebuffer);
        uploadBuffer.flip();

        Present(uploadBuffer);
    }

    /**
     * Upload framebuffer data and draw to screen.
     * Expects packed RGBA bytes, row-major, top-left origin.
     * Buffer must have at least fbWidth * fbHeight * 4 bytes remaining.
     */
    public static void Present(ByteBuffer framebuffer) {
        assert(init);
        assert(framebuffer != null);
        assert(framebuffer.remaining() >= fbWidth * fbHeight * BYTES_PER_PIXEL);

        /* upload texture data */
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                fbWidth, fbHeight,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, framebuffer);

        /* setup state for fullscreen quad */
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        /* draw fullscreen quad in NDC (-1 to 1)
         * texture coords flipped vertically so (0,0) = top-left in source */
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex2f(-1.0f, -1.0f);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex2f( 1.0f, -1.0f);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex2f( 1.0f,  1.0f);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex2f(-1.0f,  1.0f);
        GL11.glEnd();

        /* restore state */
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public static int GetWidth() {
        assert(init);
        return fbWidth;
    }

    public static int GetHeight() {
        assert(init);
        return fbHeight;
    }

    public static void Shutdown() {
        assert(init);

        GL11.glDeleteTextures(texID);
        texID = 0;
        uploadBuffer = null;

        init = false;
    }

    public static final String CLASS = SpriteBackend.class.getSimpleName();
}