package whitetail.graphics;

import org.lwjgl.opengl.*;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

/*
    Core of the 3d rendering system.
    Manages OpenGL state, uploads meshes and textures to the GPU
    More later
 */
public class RenderContext {
    private static boolean init;
    private static int width, height;

    private static boolean supportsVAO;

    /* As we write more special purpose renderers in the future, there will be
    a bool here for each (they're all static, we don't need an instance). But,
    right now I only have the generic ObjRenderer */
    private static boolean objRendererActive = false;
    private static boolean spriteRendererActive = false;
    /* post-processing */
    private static boolean postProcessingActive = false;

    /* post-processing */
    private static Framebuffer postProcessingFramebuffer;
    public static int postProcessingShaderID;

    private static final String ERR_STR_FAILED_INIT_OBJ_RENDERER;
    private static final String ERR_STR_FAILED_INIT_SPRITE_RENDERER;

    public static void ActivateObjRenderer() {
        objRendererActive = true;
        if (!ObjRenderer.Init(width, height)) {
            LogFatalAndExit(ERR_STR_FAILED_INIT_OBJ_RENDERER);
        }
    }

    public static void ActivateSpriteRenderer() {
        spriteRendererActive = true;
        if (!SpriteRenderer.Init()) {
            LogFatalAndExit(ERR_STR_FAILED_INIT_SPRITE_RENDERER);
        }
    }

    public static void ActivatePostProcessingRenderer() {
        postProcessingActive = true;
    }

    public static boolean Init(int w, int h) {
        assert(!init);

        width = w; height = h;

        supportsVAO = GLContext.getCapabilities().GL_ARB_vertex_array_object;
        if (!supportsVAO) {
            System.out.println("VAO support not found," +
                    "falling back to VBO only mode");
        } else {
            System.out.println("VAO support found via ARB Extensions");
        }
        Mesh.SetVaoCompatibility(supportsVAO);

        GL11.glViewport(0, 0, width, height);
        GL11.glClearColor(0.1f, 0.06f, 0.1f, 1.0f);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glFrontFace(GL11.GL_CCW);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        /* post-processing */
        Framebuffer.CheckFBOSupport();
        if (Framebuffer.IsSupported()) {
            postProcessingFramebuffer = new Framebuffer(width, height);
            if (postProcessingFramebuffer.init()) {
                if (!PostProcessingRenderer.Init(supportsVAO)) {
                    System.err.println("No post processing!");
                }
            } else {
                System.err.println("No Framebuffers!");
                postProcessingFramebuffer = null;
            }
        }

        System.out.println("Renderer3D initialized");
        return init = true;
    }

    public static void BeginFrame() {
        /* post-processing */
        if (postProcessingActive && postProcessingFramebuffer != null) {
            postProcessingFramebuffer.bind();
        }

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public static void resize(int w, int h) {
        width = w; height = h;
        GL11.glViewport(0, 0, width, height);
        if (postProcessingFramebuffer != null) {
            postProcessingFramebuffer.resize(width, height);
        }
    }

    public static void Shutdown() {
        assert(init);

        if (postProcessingFramebuffer != null) {
            postProcessingFramebuffer.cleanup();
            postProcessingFramebuffer = null;
        }

        if (postProcessingActive) PostProcessingRenderer.Shutdown();
        if (objRendererActive) ObjRenderer.Shutdown();
        if (spriteRendererActive) SpriteRenderer.Shutdown();
        init = false;
    }

    public static boolean   isInitialized()     { return init; }
    public static int       getWidth()          { return width; }
    public static int       getHeight()         { return height; }
    public static boolean   getSupportsVAO()    { return supportsVAO; }

    public static void Render() {
        if (objRendererActive) ObjRenderer.Render();
        if (spriteRendererActive) SpriteRenderer.Render();

        /* post-processing */
        if (postProcessingActive && postProcessingFramebuffer != null &&
            postProcessingShaderID != 0) {
            postProcessingFramebuffer.unbind();
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glViewport(0, 0, width, height);
            PostProcessingRenderer.RenderQuad(
                    postProcessingFramebuffer.getColorTexID(),
                    postProcessingShaderID);
        }
    }

    static {
        ERR_STR_FAILED_INIT_SPRITE_RENDERER = SpriteRenderer.class
                .getSimpleName() + " failed to initialize.\n";
        ERR_STR_FAILED_INIT_OBJ_RENDERER = ObjRenderer.class.getSimpleName() +
                " failed to initialize.\n";
    }
}
