package whitetail.software_framebuffer;

public enum GLTextureTexelLayout {
    /*              GL11.GL_RGBA8                                             */
    RGBA8           (0x8058);

    public final int glInternalLayout;

    GLTextureTexelLayout(int val) {
        this.glInternalLayout = val;
    }
}
