package whitetail.graphics;

import whitetail.graphics.materials.MaterialBase;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class Sprite {
    public final Vector3f scale;
    public final Vector3f rot; /* euler degrees */
    public final Vector3f pos;
    public final Matrix4f matModel;

    private boolean renderStatus = false;
    public final MaterialBase material;
    public final Shader shader;
    public final int shaderID;

    public Sprite(Vector3f scale, Vector3f rot, Vector3f pos,
            MaterialBase material) {
        this.scale = scale;
        this.rot = rot;
        this.pos = pos;
        this.matModel = new Matrix4f();
        this.material = material;
        this.shader = material.shader;
        this.shaderID = shader.id;
    }

    public void buildMatModel() {
        matModel.setIdentity();
        matModel.translate(pos);
        matModel.rotate((float)Math.toRadians(rot.y), GlobalReferenceFrame.Y());
        matModel.rotate((float)Math.toRadians(rot.x), GlobalReferenceFrame.X());
        matModel.rotate((float)Math.toRadians(rot.z), GlobalReferenceFrame.Z());
        matModel.scale(scale);
    }

    public boolean  getRenderStatus()           { return renderStatus; }
    public void     setRenderStatus(boolean b)  { renderStatus = b; }
}
