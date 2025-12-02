package whitetail.graphics;

import whitetail.loaders.ShaderFileParser;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

public class Shader {
    public final int id;
    private final Map<String, Integer> uniformLocs;
    public static final String MVP_MATRIX_SYMBOL = "matMVP";
    public final int matMVPLoc;
    private final FloatBuffer matBuf;

    public Shader(String vsFilename, String fsFilename, AttribLoc attribs[]) {
        int programHandle = ARBShaderObjects.glCreateProgramObjectARB();
        if (programHandle == 0) {
            throw new RuntimeException("Failed to create shader program");
        }

        int vsHandle = compileShader(vsFilename,
                ARBVertexShader.GL_VERTEX_SHADER_ARB);
        int fsHandle = compileShader(fsFilename,
                ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);

        ARBShaderObjects.glAttachObjectARB(programHandle, vsHandle);
        ARBShaderObjects.glAttachObjectARB(programHandle, fsHandle);

        for (AttribLoc attrib : attribs) {
            ARBVertexShader.glBindAttribLocationARB(programHandle,
                    attrib.loc, attrib.name);
        }

        ARBShaderObjects.glLinkProgramARB(programHandle);
        int linked = ARBShaderObjects.glGetObjectParameteriARB(programHandle,
                ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB);
        if (linked == GL11.GL_FALSE) {
            String log = getInfoLog(programHandle);
            throw new RuntimeException("Shader linking failed for: " +
                    vsFilename + "/" + fsFilename + ":\n" + log);
        }
        id = programHandle;
        matMVPLoc = ARBShaderObjects.glGetUniformLocationARB(id,
                        MVP_MATRIX_SYMBOL);
        /*
        if (matMVPLoc == -1) {
            throw new RuntimeException("Unable to create shader: " + vsFilename
            + "/" + fsFilename + ":\n" + "Failed to find MVP matrix uniform. \n"
            + "Must be named: " + MVP_MATRIX_SYMBOL);
        }
        */

        matBuf = BufferUtils.createFloatBuffer(16);

        uniformLocs = new HashMap<String, Integer>();
        System.out.println("Created shader: " + vsFilename + "/" + fsFilename);
    }

    public int getUniformLoc(String name) {
        Integer loc = uniformLocs.get(name);
        if (loc == null) {
            loc = ARBShaderObjects.glGetUniformLocationARB(id, name);
            uniformLocs.put(name, loc);
        }
        return loc;
    }

    public void updateMatMVP(Matrix4f m) {
        m.store(matBuf);
        matBuf.flip();
        ARBShaderObjects.glUniformMatrix4ARB(matMVPLoc,
                false, matBuf);
    }

    private static int compileShader(String filename, int type) {
        int handle = ARBShaderObjects.glCreateShaderObjectARB(type);
        String source = ShaderFileParser.FromFile(filename);
        ARBShaderObjects.glShaderSourceARB(handle, source);
        ARBShaderObjects.glCompileShaderARB(handle);
        int compiled = ARBShaderObjects.glGetObjectParameteriARB(
                handle, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB);
        if (compiled == GL11.GL_FALSE) {
            String log = getInfoLog(handle);
            String shaderType = (type == ARBVertexShader.GL_VERTEX_SHADER_ARB) ?
                    "vertex" : "fragment";
            throw new RuntimeException("Failed to compile " + shaderType +
                    " shader " + filename + ":\n" + log);
        }
        return handle;
    }

    private static String getInfoLog(int handle) {
        return ARBShaderObjects.glGetInfoLogARB(
                handle, ARBShaderObjects.glGetObjectParameteriARB(
                                handle, ARBShaderObjects
                                        .GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }

    public static class AttribLoc {
        public final int loc;
        public final String name;

        public AttribLoc(int loc, String name) {
            this.loc = loc;
            this.name = name;
        }
    }
}
