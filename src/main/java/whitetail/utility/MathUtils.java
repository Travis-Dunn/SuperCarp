package whitetail.utility;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

public final class MathUtils {
    public static Quaternion QuatFromAxisAngle(Vector3f axis, float theta) {
        float halfTheta = theta / 2.0f;
        float s = (float)Math.sin(halfTheta);
        Quaternion q = new Quaternion();
        q.x = axis.x * s;
        q.y = axis.y * s;
        q.z = axis.z * s;
        q.w = (float)Math.cos(halfTheta);
        return q;
    }

    public static Vector3f quatRotate(Quaternion q, Vector3f v) {
        Quaternion qv = new Quaternion(v.x, v.y, v.z, 0.0f);
        Quaternion qConj = new Quaternion();
        qConj.negate(q);
        qConj.w = q.w;

        Quaternion ret = new Quaternion();
        Quaternion.mul(q, qv, ret);
        Quaternion.mul(ret, qConj, ret);

        return new Vector3f(ret.x, ret.y, ret.z);
    }

    public static void buildLookAt(Matrix4f m, Vector3f eye, Vector3f centre,
            Vector3f up) {
        Vector3f f = new Vector3f();
        Vector3f s = new Vector3f();
        Vector3f u = new Vector3f();

        Vector3f.sub(eye, centre, f);
        f.normalise();

        Vector3f.cross(f, up, s);
        s.normalise();

        Vector3f.cross(s, f, u);

        m.setIdentity();
        m.m00 = s.x;
        m.m10 = s.y;
        m.m20 = s.z;
        m.m01 = u.x;
        m.m11 = u.y;
        m.m21 = u.z;
        m.m02 = -f.x;
        m.m12 = -f.y;
        m.m22 = -f.z;
        m.m30 = -Vector3f.dot(s, eye);
        m.m31 = -Vector3f.dot(u, eye);
        m.m32 = Vector3f.dot(f, eye);
    }

    public static void BuildPerspective(Matrix4f m, float fov, float ar,
            float near, float far) {
        float yScale = 1.0f / (float)Math.tan(fov / 2.0f);
        float xScale = yScale / ar;
        float frustumLength = far - near;

        m.setZero();
        m.m00 = xScale;
        m.m11 = yScale;
        m.m22 = -((far + near) / frustumLength);
        m.m23 = -1.0f;
        m.m32 = -((2.0f * near * far) / frustumLength);
    }

    public static void BuildOrthographic(Matrix4f m, float left, float right,
            float bottom, float top, float near, float far) {
        m.setZero();
        m.m00 = 2.0f / (right - left);
        m.m11 = 2.0f / (top - bottom);
        m.m22 = -2.0f / (far - near);
        m.m30 = -(right + left) / (right - left);
        m.m31 = -(top + bottom) / (top - bottom);
        m.m32 = -(far + near) / (far - near);
        m.m33 = 1.0f;
    }

    public static boolean fboundse(float min, float max, float n) {
        return (n > min && n < max);
    }

    public static boolean isfinite(float n) {
        return (!Float.isNaN(n) && !Float.isInfinite(n));
    }
}
