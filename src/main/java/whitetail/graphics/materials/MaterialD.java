package whitetail.graphics.materials;

import whitetail.graphics.Shader;
import whitetail.graphics.Texture;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class MaterialD extends MaterialBase {
    public final Texture diffuse;
    public final int diffuseLoc;

    public MaterialD(Shader s, Texture d) {
        super(s);
        diffuse = d;

        diffuseLoc = shader.getUniformLoc("diffuse");
        if (diffuseLoc == -1) {
            throw new RuntimeException("Problem creating MaterialD; shader " +
                    "doesn't have 'diffuse'");
        }
    }

    public void bind() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuse.getID());
        ARBShaderObjects.glUniform1iARB(diffuseLoc, 0);
    }

    public void unbind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
}
