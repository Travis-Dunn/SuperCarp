package whitetail.graphics;

import whitetail.utility.logging.LogLevel;
import org.lwjgl.opengl.*;

import static whitetail.utility.logging.Logger.LogSession;

public class Framebuffer {
    private int fboID;
    private int colorTexID;
    private int depthTexID;
    private int width, height;
    private boolean init;

    private static boolean supportsARBFBO;
    private static boolean supportsEXTFBO;

    private static final String ERR_STR_NOT_SUPPORTED;
    private static final String ERR_STR_FAILED_INIT;

    public Framebuffer(int width, int height) {
        this.width = width;
        this.height = height;
        init = false;
    }

    public static void CheckFBOSupport() {
        ContextCapabilities capabilities = GLContext.getCapabilities();

        supportsARBFBO = capabilities.GL_ARB_framebuffer_object;
        supportsEXTFBO = capabilities.GL_EXT_framebuffer_object;

        if (!supportsARBFBO && ! supportsEXTFBO) {
            LogSession(LogLevel.WARNING, ERR_STR_NOT_SUPPORTED);
        }
    }

    public static boolean IsSupported() {
        return supportsARBFBO || supportsEXTFBO;
    }

    public boolean init() {
        if (!IsSupported()) return init = false;

        if (supportsARBFBO) {
            fboID = ARBFramebufferObject.glGenFramebuffers();
            ARBFramebufferObject.glBindFramebuffer(
                    ARBFramebufferObject.GL_FRAMEBUFFER, fboID);
        } else {
            fboID = EXTFramebufferObject.glGenFramebuffersEXT();
            EXTFramebufferObject.glBindFramebufferEXT(
                    EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboID);
        }
        colorTexID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexID);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
                width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                (java.nio.ByteBuffer)null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
                GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
                GL12.GL_CLAMP_TO_EDGE);

        if (supportsARBFBO) {
            ARBFramebufferObject.glFramebufferTexture2D(
                    ARBFramebufferObject.GL_FRAMEBUFFER,
                    ARBFramebufferObject.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, colorTexID, 0);
        } else {
            EXTFramebufferObject.glFramebufferTexture2DEXT(
                    EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
                    GL11.GL_TEXTURE_2D, colorTexID, 0);
        }
        if (supportsARBFBO) {
            depthTexID = ARBFramebufferObject.glGenRenderbuffers();
            ARBFramebufferObject.glBindRenderbuffer(
                    ARBFramebufferObject.GL_RENDERBUFFER, depthTexID);
            ARBFramebufferObject.glRenderbufferStorage(
                    ARBFramebufferObject.GL_RENDERBUFFER,
                    GL14.GL_DEPTH_COMPONENT24, width, height);
            ARBFramebufferObject.glFramebufferRenderbuffer(
                    ARBFramebufferObject.GL_FRAMEBUFFER,
                    ARBFramebufferObject.GL_DEPTH_ATTACHMENT,
                    ARBFramebufferObject.GL_RENDERBUFFER, depthTexID);
        } else {
            depthTexID = EXTFramebufferObject.glGenRenderbuffersEXT();
            EXTFramebufferObject.glBindRenderbufferEXT(
                    EXTFramebufferObject.GL_RENDERBUFFER_EXT, depthTexID);
            EXTFramebufferObject.glRenderbufferStorageEXT(
                    EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    GL14.GL_DEPTH_COMPONENT24, width, height);
            EXTFramebufferObject.glFramebufferRenderbufferEXT(
                    EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                    EXTFramebufferObject.GL_RENDERBUFFER_EXT, depthTexID);
        }

        int status;
        if (supportsARBFBO) {
            status = ARBFramebufferObject.glCheckFramebufferStatus(
                    ARBFramebufferObject.GL_FRAMEBUFFER);
            if (status != ARBFramebufferObject.GL_FRAMEBUFFER_COMPLETE) {
                LogSession(LogLevel.WARNING, ERR_STR_FAILED_INIT + status +
                        "]\n");
                cleanup();
                return init = false;
            }
        } else {
            status = EXTFramebufferObject.glCheckFramebufferStatusEXT(
                    EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
            if (status != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) {
                LogSession(LogLevel.WARNING, ERR_STR_FAILED_INIT + status +
                        "]\n");
                cleanup();
                return init = false;
            }
        }
        unbind();

        return init = true;
    }

    public void bind() {
        if (supportsARBFBO) {
            ARBFramebufferObject.glBindFramebuffer(
                    ARBFramebufferObject.GL_FRAMEBUFFER, fboID);
        } else {
            EXTFramebufferObject.glBindFramebufferEXT(
                    EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboID);
        }
        GL11.glViewport(0, 0, width, height);
    }

    public void unbind() {
        if (supportsARBFBO) {
            ARBFramebufferObject.glBindFramebuffer(
                    ARBFramebufferObject.GL_FRAMEBUFFER, 0);
        } else {
            EXTFramebufferObject.glBindFramebufferEXT(
                    EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
        }
    }

    public void cleanup() {
        if (colorTexID != 0) {
            GL11.glDeleteTextures(colorTexID);
            colorTexID = 0;
        }
        if (depthTexID != 0) {
            if (supportsARBFBO) {
                ARBFramebufferObject.glDeleteRenderbuffers(depthTexID);
            } else {
                EXTFramebufferObject.glDeleteRenderbuffersEXT(depthTexID);
            }
            depthTexID = 0;
        }
        if (fboID != 0) {
            if (supportsARBFBO) {
                ARBFramebufferObject.glDeleteFramebuffers(fboID);
            } else {
                EXTFramebufferObject.glDeleteFramebuffersEXT(fboID);
            }
            fboID = 0;
        }
        init = false;
    }

    public void resize(int width, int height) {
        if (this.width == width && this.height == height) return;

        cleanup();
        this.width = width;
        this.height = height;
        init();
    }

    public int      getColorTexID() { return colorTexID; }
    public int      getWidth()      { return width; }
    public int      getHeight()     { return height; }
    public boolean  isInit()        { return init; }

    static {
        ERR_STR_NOT_SUPPORTED = Framebuffer.class.getSimpleName() +
                " not supported.\n";
        ERR_STR_FAILED_INIT = Framebuffer.class.getSimpleName() +
                " failed to initialize. Status [";
    }
}
