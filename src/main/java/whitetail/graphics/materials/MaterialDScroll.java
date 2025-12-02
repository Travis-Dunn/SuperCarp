package whitetail.graphics.materials;

import whitetail.graphics.Shader;
import whitetail.graphics.Texture;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class MaterialDScroll extends MaterialBase {
    public final Texture diffuse;
    public final int diffuseLoc;
    public final int scrollOffsetLoc;

    public float scrollS, scrollT;

    public MaterialDScroll(Shader s, Texture d) {
        super(s);
        diffuse = d;

        diffuseLoc = shader.getUniformLoc("diffuse");
        if (diffuseLoc == -1) {
            throw new RuntimeException("Problem creating MaterialD; shader " +
                    "doesn't have 'diffuse'");
        }
        scrollOffsetLoc = shader.getUniformLoc("scrollOffset");
        if (scrollOffsetLoc == -1) {
            throw new RuntimeException("Problem creating MaterialDScroll; shader " +
                    "doesn't have 'scrollOffset'");
        }
        scrollS = scrollT = 0.0f;
    }

    public void bind() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuse.getID());
        ARBShaderObjects.glUniform1iARB(diffuseLoc, 0);

        ARBShaderObjects.glUniform2fARB(scrollOffsetLoc, scrollS, scrollT);
    }

    public void unbind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    public void setScrollOffset(float s, float t) {
        scrollS = s;
        scrollT = t;
    }

    public void addScrollOffset(float ds, float dt) {
        scrollS += ds;
        scrollT += dt;
    }
}
