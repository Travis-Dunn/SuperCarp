package whitetail.graphics;

import whitetail.graphics.materials.MaterialBase;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class Obj {
    public final Mesh mesh;
    public final Vector3f scale;
    public final Vector3f rot; /* euler degrees */
    public final Vector3f pos;
    public final Matrix4f matModel;

    private boolean renderStatus = false;
    public final MaterialBase material;
    public final Shader shader;
    public final int shaderID;

    public Obj(Mesh mesh, Vector3f scale, Vector3f rot, Vector3f pos,
               MaterialBase mat) {
        this.mesh = mesh;
        this.scale = scale;
        this.rot = rot;
        this.pos = pos;
        this.material = mat;
        shader = mat.shader;
        this.shaderID = this.shader.id;
        this.matModel = new Matrix4f();
        buildMatModel();
    }

    public void     setRenderStatus(boolean b)  { renderStatus = b; }
    public boolean  getRenderStatus()           { return renderStatus; }

    public void buildMatModel() {
        matModel.setIdentity();
        matModel.translate(pos);
        matModel.rotate((float)Math.toRadians(rot.y), GlobalReferenceFrame.Y());
        matModel.rotate((float)Math.toRadians(rot.x), GlobalReferenceFrame.X());
        matModel.rotate((float)Math.toRadians(rot.z), GlobalReferenceFrame.Z());
        matModel.scale(scale);
    }

    public Obj(Obj other) {
        this.mesh = other.mesh;
        this.scale = new Vector3f(other.scale);
        this.rot = new Vector3f(other.rot);
        this.pos = new Vector3f(other.pos);
        this.material = other.material;
        this.shader = other.shader;
        this.shaderID = other.shaderID;
        this.matModel = new Matrix4f();
        buildMatModel();
    }

    public static Obj MakeCopy(Obj obj, Vector3f scale, Vector3f rot,
            Vector3f pos) {
        return new Obj(obj.mesh, scale, rot, pos, obj.material);
    }
}
