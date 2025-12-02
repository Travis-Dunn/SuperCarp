package whitetail.graphics;

import org.lwjgl.util.vector.Vector3f;

public final class GlobalReferenceFrame {
    private static final Vector3f X_INTERNAL = new Vector3f(1, 0, 0);
    private static final Vector3f Y_INTERNAL = new Vector3f(0, 1, 0);
    private static final Vector3f Z_INTERNAL = new Vector3f(0, 0, 1);

    private static final Vector3f RIGHT_INTERNAL   = new Vector3f(1,  0,  0);
    private static final Vector3f UP_INTERNAL      = new Vector3f(0,  1,  0);
    private static final Vector3f FORWARD_INTERNAL = new Vector3f(0,  0, -1);

    public static Vector3f X()       { return new Vector3f(X_INTERNAL); }
    public static Vector3f Y()       { return new Vector3f(Y_INTERNAL); }
    public static Vector3f Z()       { return new Vector3f(Z_INTERNAL); }

    public static Vector3f Right()   { return new Vector3f(RIGHT_INTERNAL); }
    public static Vector3f Up()      { return new Vector3f(UP_INTERNAL); }
    public static Vector3f Forward() { return new Vector3f(FORWARD_INTERNAL); }

    private GlobalReferenceFrame() {}
}
