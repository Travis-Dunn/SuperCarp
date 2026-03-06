package whitetail.software_framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.IntBuffer;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;
import static whitetail.utility.logging.Logger.LogSession;
import whitetail.utility.logging.LogLevel;

/**
 * Requires Java 5, Windows XP, OpenGL 1.2
 *
 * Only for 8-bit per channel, four channel src configs.
 * For other configs use a different backend.
 */

public final class GL12SoftwareFramebuffer {
    private static boolean init;

    private static int[] buf;

    private static IntBuffer uploadBuf;

    private static int texId;

    private static GLSourceTexelLayout srcFormat;
    private static GLTextureTexelLayout dstFormat;

    /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~IMPORTANT!~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    'vp' here refers to the viewport as far as openGL is concerned.
    The viewport in DisplayConfig is the emulated viewport - not the same thing.
     */
    private static int vpX, vpY;
    private static int vpW, vpH;
    private static int winW, winH;

    private GL12SoftwareFramebuffer() {}

    public static boolean Init(
            GLSourceTexelLayout layout,
            GLTextureTexelLayout internalLayout,
            int vpW, int vpH,
            int vpX, int vpY,
            int winW, int winH) {
        assert(!init);
        assert(layout != null && layout.bpp == 4);
        assert(internalLayout != null);

        int glErr;

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        GL12SoftwareFramebuffer.srcFormat = layout;
        GL12SoftwareFramebuffer.dstFormat = internalLayout;

        if (!(vpW > 0 && vpH > 0)) {
            LogFatalAndExit(ErrStrInitVPRes(vpW, vpH));
            return init = false;
        }

        if (!(winW > 0 && winH > 0)) {
            LogFatalAndExit(ErrStrInitWinRes(winW, winH));
            return init = false;
        }

        if (!(vpX >= 0 && vpY >= 0)) {
            LogFatalAndExit(ErrStrInitOffsetNeg(vpX, vpY));
            return init = false;
        }

        if (!(vpW + vpX <= winW)) {
            LogFatalAndExit(ErrStrInitTooWide(vpX, vpW, winW));
            return init = false;
        }

        if (!(vpH + vpY <= winH)) {
            LogFatalAndExit(ErrStrInitTooTall(vpY, vpH, winH));
            return init = false;
        }

        GL12SoftwareFramebuffer.vpW = vpW;
        GL12SoftwareFramebuffer.vpH = vpH;
        GL12SoftwareFramebuffer.vpX = vpX;
        GL12SoftwareFramebuffer.vpY = vpY;
        GL12SoftwareFramebuffer.winW = winW;
        GL12SoftwareFramebuffer.winH = winH;

        try {
            uploadBuf = BufferUtils.createIntBuffer(
                    GL12SoftwareFramebuffer.vpW *
                            GL12SoftwareFramebuffer.vpH);
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return init = false;
        }

        if ((glErr = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            LogFatalAndExit(ErrStrPreExistingGlErr(glErr));
            return init = false;
        }

        texId = GL11.glGenTextures();

        if (texId == 0) {
            LogFatalAndExit(ERR_STR_FAILED_INIT_GEN_TEX);
            return init = false;
        }

        if (!CheckGlErrorInit("glGenTextures()")) return init = false;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);

        if (!CheckGlErrorInit("glBindTexture(GL_TEXTURE_2D, " + texId))
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

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0,
                GL12SoftwareFramebuffer.dstFormat.glInternalLayout,
                GL12SoftwareFramebuffer.vpW, GL12SoftwareFramebuffer.vpH,
                0, GL12SoftwareFramebuffer.srcFormat.glFormat,
                GL12SoftwareFramebuffer.srcFormat.glType, (IntBuffer) null);

        if (!CheckGlErrorInit("glTexImage2D(GL_TEXTURE_2D, 0, " +
                "GL_RGBA, [" + GL12SoftwareFramebuffer.vpW + "], [" +
                GL12SoftwareFramebuffer.vpH + "], 0, GL_RGBA, GL_UNSIGNED_" +
                "BYTE, (ByteBuffer) null)"))
            return init = false;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        if (!CheckGlErrorInit("glBindTexture(GL_TEXTURE_2D, 0)"))
            return init = false;

        try {
            buf = new int[GL12SoftwareFramebuffer.vpW *
                    GL12SoftwareFramebuffer.vpH];
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return init = false;
        }

        /* TODO: update me */
        LogSession(LogLevel.DEBUG, CLASS + " initialized with ["
                + GL12SoftwareFramebuffer.vpW + "] width, [" +
                GL12SoftwareFramebuffer.vpH + "] height.\n");

        return init = true;
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

    private static boolean CheckGlErrorInit(String operation) {
        assert(!init);

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            LogFatalAndExit(CLASS + " failed to initialize because an " +
                    "OpenGL error was detected after ["
                    + operation + "] " + GlErrorString(error));
            return false;
        }
        return true;
    }

    public static void Present() {
        assert(init);
        assert(buf != null);
        assert(GL12SoftwareFramebuffer.srcFormat.bpp == 4);
        assert(buf.length == GL12SoftwareFramebuffer.vpW *
                GL12SoftwareFramebuffer.vpH);

        uploadBuf.clear();
        uploadBuf.put(buf);
        uploadBuf.flip();

        GL11.glViewport(vpX, vpY, vpW, vpH);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);

        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                vpW, vpH, srcFormat.glFormat, srcFormat.glType, uploadBuf);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex2f(-1.0f, -1.0f);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex2f( 1.0f, -1.0f);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex2f( 1.0f,  1.0f);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex2f(-1.0f,  1.0f);
        GL11.glEnd();


        GL11.glViewport(0, 0, winW, winH);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public static void Shutdown() {
        assert(init);

        int glErr;

        LogSession(LogLevel.DEBUG, CLASS + " shutting down...\n");

        if ((glErr = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            LogSession(LogLevel.WARNING, ErrStrPreExistingGlErrShutdown(glErr));
        }

        if (texId != 0) {
            GL11.glDeleteTextures(texId);

            if ((glErr = GL11.glGetError()) != GL11.GL_NO_ERROR)
                LogSession(LogLevel.WARNING, ErrStrDeleteTextures(glErr));

            texId = 0;
        } else
            LogSession(LogLevel.WARNING, ERR_STR_TEX_ID_ZERO);


        buf = null;
        uploadBuf = null;

        init = false;

        LogSession(LogLevel.DEBUG, CLASS + " shutdown complete.\n");
    }

    public static int GetBytesPerPixel() { assert(init); return srcFormat.bpp; }
    public static int[] GetBuf() { assert(init); return buf; }

    public static final String CLASS =
            GL12SoftwareFramebuffer.class.getSimpleName();
    private static String ErrStrInitVPRes(int w, int h) {
        return String.format("%s failed to initialize because viewport width " +
                "[%d] and height [%d] must be positive.\n", CLASS, w, h);
    }
    private static String ErrStrInitWinRes(int w, int h) {
        return String.format("%s failed to initialize because window width " +
                "[%d] and height [%d] must be positive.\n", CLASS, w, h);
    }
    private static String ErrStrInitOffsetNeg(int x, int y) {
        return String.format("%s failed to initialize because x offset " +
                "[%d] and y offset [%d] must not be negative.\n", CLASS, x, y);
    }
    private static String ErrStrInitTooWide(int vpX, int vpW, int winW) {
        return String.format("%s failed to initialize because x offset " +
                "[%d] + viewport width [%d] must not exceed window width [%d]" +
                ".\n", CLASS, vpX, vpW, winW);
    }
    private static String ErrStrInitTooTall(int vpY, int vpH, int winH) {
        return String.format("%s failed to initialize because y offset " +
                "[%d] + viewport height [%d] must not exceed window height " +
                "[%d].\n", CLASS, vpY, vpH, winH);
    }
    private static String ErrStrPreExistingGlErr(int glErr) {
        return String.format("%s encountered a pre-existing OpenGL error " +
                "during initialization: %s", CLASS, GlErrorString(glErr));
    }
    private static final String ERR_STR_FAILED_INIT_GEN_TEX = CLASS +
            " failed to initialize because OpenGL failed to generate a " +
            "texture.\n";
    private static String ErrStrPreExistingGlErrShutdown(int glErr) {
        return String.format("%s encountered a pre-existing OpenGL error " +
                "during shutdown: %s", CLASS, GlErrorString(glErr));
    }
    private static String ErrStrDeleteTextures(int glErr) {
        return String.format("%s encountered an OpenGL error after " +
                "glDeleteTextures([%d]): %s", CLASS, texId,
                GlErrorString(glErr));
    }
    private static final String ERR_STR_TEX_ID_ZERO = CLASS +
            " shutdown called but texId was already 0. This may indicate " +
            "Init() failed or Shutdown() was called twice.\n";
}
