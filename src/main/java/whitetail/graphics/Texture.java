package whitetail.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class Texture {
    private byte buf[];
    private int width, height;
    private int id; /* GPU handle */
    private final boolean hasAlpha;

    public Texture(byte data[], int w, int h, boolean hasAlpha) {
        this.buf = data;
        this.width = w;
        this.height = h;
        this.hasAlpha = hasAlpha;
        this.id = -1;
    }

    public void freeData() {
        buf = null;
    }

    public void upload() {
        assert(id == -1);
        assert(width != 0);
        assert(height != 0);
        assert(buf != null);

        id = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
                GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
                GL11.GL_REPEAT);

        ByteBuffer bb = BufferUtils.createByteBuffer(buf.length);
        bb.put(buf);
        bb.flip();

        int format = hasAlpha ? GL11.GL_RGBA : GL11.GL_RGB;

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,     /* target */
                0,                      /* mipmap level */
                format,                 /* format that we want to store as */
                width,
                height,
                0,               /* border */
                format,                 /* format of the buffer we're passing */
                GL11.GL_UNSIGNED_BYTE,
                bb
        );

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    @Override
    public String toString() {
        return "Texture{width=" + width + ", height=" + height + ", alpha=" +
                hasAlpha + ", id=" + id + ", isFreed=" + (buf != null) + "}";
    }

    public boolean isUploaded() { return id != -1; }
    public int getID() { return id; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean hasAlpha() { return hasAlpha; }

    public void destroy() {
        assert(id != -1);

        GL11.glDeleteTextures(id);
        id = -1;
    }
}
