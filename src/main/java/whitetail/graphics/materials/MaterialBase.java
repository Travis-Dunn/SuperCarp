package whitetail.graphics.materials;

import whitetail.graphics.Shader;

public abstract class MaterialBase {
    public final Shader shader;

    public MaterialBase(Shader s) {
        this.shader = s;
    }

    public abstract void bind();
    public abstract void unbind();
}
