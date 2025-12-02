package whitetail.graphics;

import whitetail.graphics.cameras.Camera;
import whitetail.graphics.materials.MaterialBase;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class ObjRenderer {
    private static List<Obj> objs;

    private static boolean init;

    private static Matrix4f matProj;
    private static Matrix4f matView;
    private static Matrix4f matMVP;
    private static Matrix4f matBuf;

    /* may decide to handle the camera a different way */
    private static Camera cam;

    private ObjRenderer() {}

    public static boolean Init(int w, int h) {
        assert(!init);

        objs = new ArrayList<>();
        matMVP = new Matrix4f();
        matBuf = new Matrix4f();
        return init = true;
    }

    public static void SetCamera(Camera c) { cam = c; };

    public static void Add(Obj o) {
        assert(init);

        objs.add(o);
    }

    public static void Remove(Obj o) {
        assert(init);

        objs.remove(o);
    }

    public static void Render() {
        assert(init);
        assert(cam != null);

        matView = cam.getMatView();
        matProj = cam.getMatProj();

        for (Obj o : objs) {
            if (!o.getRenderStatus()) continue;

            MaterialBase m = o.material;

            /* build model matrix every frame. */
            /* OPTIMIZE: Cache and rebuild only when stale */
            o.buildMatModel();

            /* use shader program */
            ARBShaderObjects.glUseProgramObjectARB(o.shaderID);

            /* build mvp matrix */
            Matrix4f.mul(matView, o.matModel, matBuf);
            Matrix4f.mul(matProj, matBuf, matMVP);

            /* update mvp matrix uniform */
            o.shader.updateMatMVP(matMVP);

            /* bind all textures */
            m.bind();

            /* bind vao */
            ARBVertexArrayObject.glBindVertexArray(o.mesh.getVaoID());

            /* draw call */
            GL11.glDrawElements(GL11.GL_TRIANGLES, o.mesh.indexCount,
                    GL11.GL_UNSIGNED_INT, 0);

            /* unbind vao, textures, shader program */
            /* OPTIMIZE: Either don't unbind at all, or unbind only after
                the loop */
            ARBVertexArrayObject.glBindVertexArray(0);
            m.unbind();
            ARBShaderObjects.glUseProgramObjectARB(0);
        }
    }

    public static void Shutdown() {
        assert(init);

        objs.clear();
        objs = null;
        matView = null;
        matProj = null;
        matBuf = null;
        matMVP = null;
        cam = null;

        init = false;
    }

    public static int GetRenderListCount() { return objs.size(); }
}
