package whitetail.software_framebuffer;

public enum GLSourceTexelLayout {
    /*                  GL11.GL_RGBA,       GL11.GL_UNSIGNED_BYTE             */
    RGBA_UBYTE          (0x1908,    0x1401, 4),
    /*                  GL12.GL_BGRA,       GL11.GL_UNSIGNED_BYTE             */
    BGRA_UBYTE          (0x80E1,    0x1401, 4),
    /*                  GL11.GL_RGBA,       GL12.GL_UNSIGNED_INT_8_8_8_8      */
    RGBA_UINT_8888      (0x1908,    0x8035, 4),
    /*                  GL11.GL_RGBA,       GL12.GL_UNSIGNED_INT_8_8_8_8_REV  */
    RGBA_UINT_8888_REV  (0x1908,    0x8367, 4),
    /*                  GL12.GL_BGRA,       GL12.GL_UNSIGNED_INT_8_8_8_8      */
    BGRA_UINT_8888      (0x80E1,    0x8035, 4),
    /*                  GL12.GL_BGRA,       GL12.GL_UNSIGNED_INT_8_8_8_8_REV  */
    BGRA_UINT_8888_REV  (0x80E1,    0x8367, 4);

    public final int glFormat;
    public final int glType;
    public final int bpp;

    GLSourceTexelLayout(int glFormat, int glType, int bpp) {
        this.glFormat = glFormat;
        this.glType = glType;
        this.bpp = bpp;
    }
}
