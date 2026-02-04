package production.sprite;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import whitetail.utility.logging.LogLevel;

import java.nio.ByteBuffer;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;
import static whitetail.utility.logging.Logger.LogSession;

public final class SpriteBackend {
    private static boolean init;

    private static ByteBuffer uploadBuffer;

    private static int bpp;

    private SpriteBackend() {}

    static boolean Init(int bpp) {
        assert(!init);

        int glErr;

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        if (bpp < 1) {
            LogFatalAndExit(ErrStrFailedInitBppTooSmall(bpp));
            return init = false;
        } else {
            SpriteBackend.bpp = bpp;
        }

        try {
            uploadBuffer = BufferUtils.createByteBuffer(
           SpriteSys.fbWidth * SpriteSys.fbHeight *
                SpriteBackend.bpp);
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return init = false;
        }

        if ((glErr = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            LogFatalAndExit(ErrStrPreExistingGlErr(glErr));
            return init = false;
        }

        SpriteSys.texID = GL11.glGenTextures();

        if (SpriteSys.texID == 0) {
            LogFatalAndExit(ERR_STR_FAILED_INIT_GEN_TEX);
            return init = false;
        }
        if (!CheckGlErrorInit("glGenTextures()"))
            return init = false;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, SpriteSys.texID);

        if (!CheckGlErrorInit("glBindTexture(GL_TEXTURE_2D, " +
                "<id from glGenTextures>)"))
            return init = false;

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

        if (!CheckGlErrorInit("glTexParameteri(GL_TEXTURE_2D, " +
                "GL_TEXTURE_MIN_FILTER, GL_NEAREST)"))
            return init = false;

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        if (!CheckGlErrorInit("glTexParameteri(GL_TEXTURE_2D, " +
                "GL_TEXTURE_MAG_FILTER, GL_NEAREST)"))
            return init = false;

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);

        if (!CheckGlErrorInit("glTexParameteri(GL_TEXTURE_2D, " +
                "GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)"))
            return init = false;

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        if (!CheckGlErrorInit("glTexParameteri(GL_TEXTURE_2D, " +
                "GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)"))
            return init = false;

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                SpriteSys.fbWidth, SpriteSys.fbHeight, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

        if (!CheckGlErrorInit("glTexImage2D(GL_TEXTURE_2D, 0, " +
                "GL_RGBA, [" + SpriteSys.fbWidth + "], [" + SpriteSys.fbHeight
                + "], 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null)"))
            return init = false;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        if (!CheckGlErrorInit("glBindTexture(GL_TEXTURE_2D, 0)"))
            return init = false;

        LogSession(LogLevel.DEBUG, CLASS + " initialized with ["
                + SpriteSys.fbWidth + "] width, [" + SpriteSys.fbHeight
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
        assert(framebuffer.length == SpriteSys.fbWidth *
                SpriteSys.fbHeight * bpp);

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
        assert(framebuffer.remaining() >= SpriteSys.fbWidth *
                SpriteSys.fbHeight * bpp);

        /* upload texture data */
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, SpriteSys.texID);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                SpriteSys.fbWidth, SpriteSys.fbHeight,
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

    static void Shutdown() {
        assert(init);

        int glErr;

        LogSession(LogLevel.DEBUG, CLASS + " shutting down...\n");

        if ((glErr = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            LogSession(LogLevel.WARNING, ErrStrPreExistingGlErrShutdown(glErr));
        }

        /* glDeleteTextures silently ignores 0 and invalid names, but we
         * check anyway in case Init partially failed or something went
         * very wrong */
        if (SpriteSys.texID != 0) {
            GL11.glDeleteTextures(SpriteSys.texID);

            if ((glErr = GL11.glGetError()) != GL11.GL_NO_ERROR) {
                LogSession(LogLevel.WARNING, ErrStrDeleteTextures(glErr));
            }

            SpriteSys.texID = 0;
        } else {
            LogSession(LogLevel.WARNING, ERR_STR_TEX_ID_ZERO);
        }

        uploadBuffer = null;

        init = false;

        LogSession(LogLevel.DEBUG, CLASS + " shutdown complete.\n");
    }

    private static boolean CheckGlErrorInit(String operation) {
        assert(!init);

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            LogFatalAndExit(CLASS + " failed to initialize because an OpenGL error was detected after ["
                    + operation + "] " + GlErrorString(error));
            return false;
        }
        return true;
    }

    private static boolean CheckGlErrorRun(String ctx, String operation) {
        assert(init);

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            LogFatalAndExit(CLASS + " failed to " + ctx +
                    " because an OpenGL error was detected after ["
                    + operation + "] " + GlErrorString(error));
            return false;
        }
        return true;
    }

    private static String GlErrorString(int error) {
        switch (error) {
            case GL11.GL_INVALID_ENUM:      return "[GL_INVALID_ENUM]\n";
            case GL11.GL_INVALID_VALUE:     return "[GL_INVALID_VALUE]\n";
            case GL11.GL_INVALID_OPERATION: return "[GL_INVALID_OPERATION]\n";
            case GL11.GL_STACK_OVERFLOW:    return "[GL_STACK_OVERFLOW]\n";
            case GL11.GL_STACK_UNDERFLOW:   return "[GL_STACK_UNDERFLOW]\n";
            case GL11.GL_OUT_OF_MEMORY:     return "[GL_OUT_OF_MEMORY]\n";
            default: return "Unknown error [" + error + "]\n";
        }
    }

    public static final String CLASS = SpriteBackend.class.getSimpleName();
    private static String ErrStrPreExistingGlErr(int glErr) {
        return String.format("%s encountered a pre-existing OpenGL error " +
                "during initialization: %s", CLASS, GlErrorString(glErr));
    }
    private static String ErrStrPreExistingGlErrShutdown(int glErr) {
        return String.format("%s encountered a pre-existing OpenGL error " +
                "during shutdown: %s", CLASS, GlErrorString(glErr));
    }
    private static String ErrStrDeleteTextures(int glErr) {
        return String.format("%s encountered an OpenGL error after " +
                        "glDeleteTextures([%d]): %s", CLASS, SpriteSys.texID,
                GlErrorString(glErr));
    }
    private static final String ERR_STR_FAILED_INIT_GEN_TEX = CLASS +
            " failed to initialize because OpenGL failed to generate a " +
            "texture.\n";
    private static final String ERR_STR_TEX_ID_ZERO = CLASS +
            " shutdown called but texID was already 0. This may indicate " +
            "Init() failed or Shutdown() was called twice.\n";

    private static String ErrStrFailedInitBppTooSmall(int bpp) {
        return String.format("%s failed to initialize because bpp [%d] must " +
                "be at least 1", CLASS, bpp);
    }
}