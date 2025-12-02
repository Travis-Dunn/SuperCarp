package whitetail.graphics.cameras;

import whitetail.graphics.GlobalReferenceFrame;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalWithFloatAndExit;
import static whitetail.utility.MathUtils.*;

/* Everything is radians unless specified otherwise */
public class Camera {
    private final CameraType type;

    private float aspectRatio;
    private float fov;
    private float fovMin, fovMax;
    private float nearClip, farClip;

    /* attitude in global frame */
    private Vector3f    pos;
    private Quaternion  rot;
    private Vector3f    right;
    private Vector3f    up;
    private Vector3f    forward;

    private Vector3f    dPosGlobal;
    private Vector3f    dPosLocal;
    private Vector3f    dRotGlobal; /* euler degrees */
    private Vector3f    dRotLocal;  /* euler degrees */
    private float       dFov;       /* euler degrees */

    private boolean viewDirty, projDirty;

    private Matrix4f matView, matProj;

    /* read only derived values */
    private float pitchDegrees, yawDegrees, rollDegrees;
    private float fovDegrees;

    /* unitless scalar tuning parameters */
    private float sensitivity       = 0.04f;
    private float translateFactor   = 1.50f;
    private float rotateFactor      = 3.00f;
    private float zoomFactor        = 0.04f;

    /* external reference */
    private float deltaTime;

    /* type specific */
    private Vector3f followTarget;
    private float followDistance;
    private float followAngleDegrees;
    private Vector3f followOffset;

    /* utility */
    private Vector3f cache;

    /* TODO: Populate from config file */
    /* The reason we have "min" and "default min" is that the config file parser
        may and may not be able to produce a valid value to set "min" to, and in
        that case, it will set "min" to "default min". Setting "min" to
        "default min" is a temporary placeholder operation that will go away
        when we write the config file parser. */

    /**
     * Exclusive limits for bounds checking values passed as arguments or read
     * from config files.
     * <p>
     * These are more like domain checks than useful range checks. */
    public static final float LIMIT_MIN_FOV_DEGREES        = 0.000f;
    public static final float LIMIT_MAX_FOV_DEGREES        = 180.0f;
    public static final float LIMIT_MIN_NEAR_CLIP          = 0.000f;
    public static final float LIMIT_MIN_AR                 = 0.000f;
    public static final float LIMIT_MIN_FOLLOW_ANGLE       = 0.000f;
    public static final float LIMIT_MAX_FOLLOW_ANGLE       = 90.00f;
    public static final float LIMIT_MIN_FOLLOW_DIST        = 0.000f;

    /**
     * Safe defaults intended for one of two uses. First, if the config file
     * parser fails to interpret a value, it will set it to this default.
     * Second, these may be passed to camera factory methods if you don't feel
     * like deciding on a value. */
    public static final float DEFAULT_ZOOM_MIN_FOV_DEGREES = 30.00f;
    public static final float DEFAULT_ZOOM_MAX_FOV_DEGREES = 100.0f;
    public static final float DEFAULT_FOV_DEGREES          = 60.00f;
    public static final float DEFAULT_NEAR_CLIP            = 0.100f;
    public static final float DEFAULT_FAR_CLIP             = 100.0f;

    private static final float DEFAULT_MIN_FOV_DEGREES      = 15.0f;
    private static final float DEFAULT_MAX_FOV_DEGREES      = 175.f;
    private static final float DEFAULT_MIN_FOLLOW_DIST      = 1.00f;
    private static final float DEFAULT_MAX_FOLLOW_DIST      = 100.f;

    private static final String ERR_STR_NEAR_CLIP_OUT_OF_BOUNDS =
            "Tried to create a Camera with an invalid near clip distance";
    private static final String ERR_STR_FAR_CLIP_OUT_OF_BOUNDS =
            "Tried to create a Camera with an invalid far clip distance";
    private static final String ERR_STR_FOV_OUT_OF_BOUNDS =
            "Tried to create a Camera with an invalid field of view";
    private static final String ERR_STR_AR_OUT_OF_BOUNDS =
            "Tried to create a Camera with an invalid aspect ratio";
    private static final String ERR_STR_FOLLOW_DIST_OUT_OF_BOUNDS =
            "Tried to create a Camera of type {" +
                    CameraType.FOLLOW.toString() +
                    "} with an invalid distance";
    private static final String ERR_STR_FOLLOW_ANGLE_OUT_OF_BOUNDS =
            "Tried to create a Camera of type {" +
                    CameraType.FOLLOW.toString() +
                    "} with an invalid angle";
    public static final String ERR_STR_FOLLOW_TARGET_NULL =
            "Tried to update a Camera of type {" +
                    CameraType.FOLLOW.toString() +
                    "} that had a null followTarget";

    public Camera(CameraType t, float ar, float nClip, float fClip) {
        if (!(isfinite(ar) && ar > LIMIT_MIN_AR))
            LogFatalWithFloatAndExit(ERR_STR_AR_OUT_OF_BOUNDS, ar);
        if (!(isfinite(nClip) && nClip > LIMIT_MIN_NEAR_CLIP))
            LogFatalWithFloatAndExit(ERR_STR_NEAR_CLIP_OUT_OF_BOUNDS, nClip);
        if (!(isfinite(fClip) && fClip > nClip))
            LogFatalWithFloatAndExit(ERR_STR_FAR_CLIP_OUT_OF_BOUNDS, fClip);

        type = t;

        aspectRatio =   ar;
        nearClip =      nClip;
        farClip =       fClip;

        pos =           new Vector3f(0, 0, 0);
        right =         new Vector3f(GlobalReferenceFrame.Right());
        up =            new Vector3f(GlobalReferenceFrame.Forward());
        forward =       new Vector3f(GlobalReferenceFrame.Up());
        rot =           new Quaternion(0, 0, 0, 1);

        dPosGlobal =    new Vector3f(0, 0, 0);
        dPosLocal =     new Vector3f(0, 0, 0);
        dRotGlobal =    new Vector3f(0, 0, 0);
        dRotLocal =     new Vector3f(0, 0, 0);
        dFov =          0.0f;

        viewDirty = true;
        projDirty = true;
        matView = new Matrix4f();
        matProj = new Matrix4f();

        pitchDegrees    = 0.0f;
        yawDegrees      = 0.0f;
        rollDegrees     = 0.0f;

        cache = new Vector3f();
    }

    /* pointed at origin initially, update */
    public static Camera MakeFollower(float ar, float fovDeg, float dist,
            float angle, float nClip, float fClip) {
        if (!(isfinite(fovDeg) && fovDeg > LIMIT_MIN_FOV_DEGREES &&
                fovDeg < LIMIT_MAX_FOV_DEGREES))
            LogFatalWithFloatAndExit(ERR_STR_FOV_OUT_OF_BOUNDS, fovDeg);
        if (!(isfinite(dist) && dist > LIMIT_MIN_FOLLOW_DIST))
            LogFatalWithFloatAndExit(ERR_STR_FOLLOW_DIST_OUT_OF_BOUNDS, dist);
        if (!isfinite(angle) && angle > LIMIT_MIN_FOLLOW_ANGLE &&
                angle < LIMIT_MAX_FOLLOW_ANGLE)
            LogFatalWithFloatAndExit(ERR_STR_FOLLOW_ANGLE_OUT_OF_BOUNDS, angle);

        Camera cam = new Camera(CameraType.FOLLOW, ar, nClip, fClip);

        /* Set to domain limits here, game probably should set these after
            calling MakeFollower to useful limits */
        cam.fovMin = LIMIT_MIN_FOV_DEGREES;
        cam.fovMax = LIMIT_MAX_FOV_DEGREES;

        cam.fov = (float)Math.toRadians(fovDeg);
        cam.fovDegrees = fovDeg;
        cam.followDistance = dist;
        cam.followAngleDegrees = angle;
        cam.followOffset = new Vector3f(0.0f,
                (float)Math.sin(Math.toRadians(cam.followAngleDegrees))
                        * cam.followDistance,
                (float)Math.cos(Math.toRadians(cam.followAngleDegrees))
                        * cam.followDistance);
        cam.followTarget = null; /* Must call setTarget! */

        /* We only need to do this once for this type of camera */
        cam.lookAt(new Vector3f(-cam.followOffset.x, -cam.followOffset.y, -cam.followOffset.z));

        /* TODO: free unused attribs */
        return cam;
    }

    public static Camera MakeMenu(float w, float h, float n, float f) {
        Camera cam = new Camera(CameraType.MENU, 1.0f, n, f);

        cam.pos.set(0, 0, 0);
        BuildOrthographic(cam.matProj, 0, w, h, 0, n, f);
        cam.projDirty = false;

        cam.matView.setIdentity();
        cam.viewDirty = false;
        return cam;
    }

    /* TODO: look at these and decide if we need them */
    public void transGlobalXMinus() {
        dPosGlobal.x = -translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transGlobalXPlus() {
        dPosGlobal.x = translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transGlobalYMinus() {
        dPosGlobal.y = -translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transGlobalYPlus() {
        dPosGlobal.y = translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transGlobalZMinus() {
        dPosGlobal.z = -translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transGlobalZPlus() {
        dPosGlobal.z = translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transLocalForward() {
        dPosLocal.z = -translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transLocalBackward() {
        dPosLocal.z = translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transLocalLeft() {
        dPosLocal.x = -translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transLocalRight() {
        dPosLocal.x = translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transLocalUp() {
        dPosLocal.y = translateFactor * deltaTime;
        viewDirty = true;
    }

    public void transLocalDown() {
        dPosLocal.y = -translateFactor * deltaTime;
        viewDirty = true;
    }

    /* only have local rotation methods right now */
    public void rotLocalX(float delta) {
        dRotLocal.x = -rotateFactor * deltaTime * delta;
        viewDirty = true;
    }

    public void rotLocalY(float delta) {
        dRotLocal.y = -rotateFactor * deltaTime * delta;
        viewDirty = true;
    }

    public void rotGlobalY(float delta) {
        dRotGlobal.y = -rotateFactor * deltaTime * delta;
        viewDirty = true;
    }

    public void zoom(float delta) {
        dFov = delta * zoomFactor;
        projDirty = true;
    }

    public void update(float dt) {
        switch (type) {
            case FOLLOW: {
                updateFollow();
            } break;
            case MENU: {
                /* pass, menu type doesn't move. If we want movement, for
                * a 2d game for instance, use a different type */
            } break;
        }
        /* legacy update code */
        if (false) {
            deltaTime = dt;
            if (viewDirty) {
                /* convert delta local pos to global frame */
                Vector3f dx = new Vector3f(right);
                dx.scale(dPosLocal.x);
                Vector3f dy = new Vector3f(up);
                dy.scale(dPosLocal.y);
                Vector3f dz = new Vector3f(forward);
                dz.scale(dPosLocal.z);

                /* add that to pos */
                Vector3f.add(pos, dx, pos);
                Vector3f.add(pos, dy, pos);
                Vector3f.add(pos, dz, pos);
                /* add delta global pos */
                Vector3f.add(pos, dPosGlobal, pos);

                if (dRotGlobal.x != 0 || dRotGlobal.y != 0 || dRotGlobal.z != 0 ||
                        dRotLocal.x != 0 || dRotLocal.y != 0 || dRotLocal.z != 0) {
                    Quaternion gPitch = QuatFromAxisAngle(GlobalReferenceFrame.Right(),
                            (float) Math.toRadians(dRotGlobal.x));
                    Quaternion gYaw = QuatFromAxisAngle(GlobalReferenceFrame.Up(),
                            (float) Math.toRadians(dRotGlobal.y));
                    Quaternion gRoll = QuatFromAxisAngle(GlobalReferenceFrame.Forward(),
                            (float) Math.toRadians(dRotGlobal.z));
                    Quaternion lPitch = QuatFromAxisAngle(right,
                            (float) Math.toRadians(dRotLocal.x));
                    Quaternion lYaw = QuatFromAxisAngle(up,
                            (float) Math.toRadians(dRotLocal.y));
                    Quaternion lRoll = QuatFromAxisAngle(forward,
                            (float) Math.toRadians(dRotLocal.z));

                    Quaternion gQ = new Quaternion();
                    Quaternion lQ = new Quaternion();
                    Quaternion.mul(gYaw, gPitch, gQ);
                    Quaternion.mul(gQ, gRoll, gQ);
                    Quaternion.mul(lYaw, lPitch, lQ);
                    Quaternion.mul(lQ, lRoll, lQ);
                    Quaternion.mul(gQ, rot, rot);
                    Quaternion.mul(rot, lQ, rot);

                    forward = quatRotate(rot, GlobalReferenceFrame.Forward());
                    up = quatRotate(rot, GlobalReferenceFrame.Up());
                    right = quatRotate(rot, GlobalReferenceFrame.Right());

                /* TODO: update pitchDegrees, yawDegrees, rollDegrees,
                    fovDegrees. For this I need to extract the euler degrees
                    from rot.
                 */
                }
                buildMatView();

                dPosGlobal.set(0, 0, 0);
                dPosLocal.set(0, 0, 0);
                dRotGlobal.set(0, 0, 0);
                dRotLocal.set(0, 0, 0);

                viewDirty = false;
            }
            if (projDirty) {
                float newFov = fov + (float) Math.toRadians(dFov);
                newFov = Math.min(Math.max(fovMin, newFov), fovMax);
                fov = newFov;
                fovDegrees = (float) Math.toDegrees(fov);

                buildMatProj();

                dFov = 0.0f;
                projDirty = false;
            }
        }
    }

    private void buildMatView() {
        Vector3f.add(pos, forward, cache);
        buildLookAt(matView, pos, cache, up);
    }

    private void buildMatProj() {
        switch (type) {
            case FOLLOW: {
                BuildPerspective(matProj, fov, aspectRatio, nearClip, farClip);
            } break;
            case MENU: {
                BuildOrthographic(matProj, 0, aspectRatio * farClip,
                        farClip, 0, nearClip, farClip);
            } break;
        }
    }

    public Matrix4f getMatView() { return matView; }
    public Matrix4f getMatProj() { return matProj; }

    @Override
    public String toString() {
        return String.format(
                "Camera {\n" +
                        "  Position: (%.3f, %.3f, %.3f)\n" +
                        "  Rotation: Pitch=%.1f°, Yaw=%.1f°, Roll=%.1f°\n" +
                        "  FOV: %.1f° (%.1f° - %.1f°)\n" +
                        "  Aspect Ratio: %.3f\n" +
                        "  Clipping: Near=%.3f, Far=%.1f\n" +
                        "  Local Axes:\n" +
                        "    Right:   (%.3f, %.3f, %.3f)\n" +
                        "    Up:      (%.3f, %.3f, %.3f)\n" +
                        "    Forward: (%.3f, %.3f, %.3f)\n" +
                        "  Factors: Translate=%.2f, Rotate=%.2f, Zoom=%.3f\n" +
                        "}",
                pos.x, pos.y, pos.z,
                pitchDegrees, yawDegrees, rollDegrees,
                fovDegrees, Math.toDegrees(fovMin), Math.toDegrees(fovMax),
                aspectRatio,
                nearClip, farClip,
                right.x, right.y, right.z,
                up.x, up.y, up.z,
                forward.x, forward.y, forward.z,
                translateFactor, rotateFactor, zoomFactor
        );
    }

    private void printMatrix4f(Matrix4f matrix) {
        System.out.printf("┌ %8.3f %8.3f %8.3f %8.3f ┐\n", matrix.m00, matrix.m10, matrix.m20, matrix.m30);
        System.out.printf("│ %8.3f %8.3f %8.3f %8.3f │\n", matrix.m01, matrix.m11, matrix.m21, matrix.m31);
        System.out.printf("│ %8.3f %8.3f %8.3f %8.3f │\n", matrix.m02, matrix.m12, matrix.m22, matrix.m32);
        System.out.printf("└ %8.3f %8.3f %8.3f %8.3f ┘\n", matrix.m03, matrix.m13, matrix.m23, matrix.m33);
    }

    public void printMatView() {
        System.out.println("View Matrix:");
        printMatrix4f(matView);
    }

    public void printMatProj() {
        System.out.println("Projection Matrix:");
        printMatrix4f(matProj);
    }

    public void setPosition(float x, float y, float z) {
        pos.set(x, y, z);
        viewDirty = true;
    }

    public void lookAt(Vector3f target) {
        Vector3f direction = new Vector3f();
        Vector3f.sub(target, pos, direction);
        direction.normalise();

        forward.set(-direction.x, -direction.y, -direction.z);

        Vector3f.cross(GlobalReferenceFrame.Up(), forward, right);
        right.normalise();
        Vector3f.cross(forward, right, up);
        up.normalise();

        viewDirty = true;
    }

    public void setTarget(Vector3f target) {
        assert(type == CameraType.FOLLOW);
        assert(target != null);

        followTarget = target;
    }

    private void updateFollow() {
        assert(type == CameraType.FOLLOW);

        if (followTarget == null) LogFatalAndExit(ERR_STR_FOLLOW_TARGET_NULL);

        /* Don't bother caching the view matrix for this camera type because
        *  it's more hassle than it's worth. */
        Vector3f.add(followTarget, followOffset, pos);
        buildMatView();

        if (!projDirty) return;

        float newFov = fov + (float)Math.toRadians(dFov);
        fov = Math.min(Math.max(fovMin, newFov), fovMax);
        fovDegrees = (float)Math.toDegrees(fov);
        buildMatProj();
        dFov = 0.0f;
        projDirty = false;
    }
}