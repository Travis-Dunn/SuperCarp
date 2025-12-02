package whitetail.graphics;

import whitetail.graphics.cameras.Camera;
import whitetail.graphics.materials.MaterialBase;
import whitetail.loaders.MeshFileParser;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class SpriteRenderer {
    private static boolean init;
    private static List<Sprite> sprites;
    private static Matrix4f matProj;
    private static Matrix4f matView;
    private static Matrix4f matMVP;
    private static Matrix4f matBuf;
    private static int vaoID;
    private static int indexCount;
    private static final String UNIT_QUAD_FILENAME = "test_starfield_plane.obj";

    private static Camera cam;

    private SpriteRenderer() {}

    public static boolean Init() {
        assert(!init);

        sprites = new ArrayList<>();
        matMVP = new Matrix4f();
        matBuf = new Matrix4f();
        Mesh unitQuadMesh = MeshFileParser.FromFile(UNIT_QUAD_FILENAME);
        unitQuadMesh.upload();
        unitQuadMesh.freeData();
        vaoID = unitQuadMesh.getVaoID();
        indexCount = unitQuadMesh.indexCount;

        return init = true;
    }

    public static void Render() {
        assert(init);
        assert(cam != null);

        matView = cam.getMatView();
        matProj = cam.getMatProj();
        ARBVertexArrayObject.glBindVertexArray(vaoID);

        for (Sprite s : sprites) {
            if (!s.getRenderStatus()) continue;

            MaterialBase m = s.material;

            s.buildMatModel();

            ARBShaderObjects.glUseProgramObjectARB(s.shaderID);

            Matrix4f.mul(matView, s.matModel, matBuf);
            Matrix4f.mul(matProj, matBuf, matMVP);

            s.shader.updateMatMVP(matMVP);

            m.bind();

            GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount,
                    GL11.GL_UNSIGNED_INT, 0);

            m.unbind();
            ARBShaderObjects.glUseProgramObjectARB(0);
        }
        ARBVertexArrayObject.glBindVertexArray(0);
    }

    public static void SetCamera(Camera c) { cam = c; };

    public static void Add(Sprite s) {
        assert(init);

        sprites.add(s);
    }
    public static void Remove(Sprite s) {
        assert(init);

        sprites.remove(s);
    }

    public static void Shutdown() {
        assert(init);

        sprites.clear();
        sprites = null;
        matMVP = null;
        matBuf = null;
        matProj = null;
        matView = null;
        cam = null;
        init = false;
    }
}
