package whitetail.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;

public class PostProcessingRenderer {
    private static int quadVBO;
    private static int quadVAO;
    private static boolean init;
    private static boolean useVAO;

    private static final float quadVerts[] = {
            -1.0f,  1.0f,  0.0f, 1.0f,
            -1.0f, -1.0f,  0.0f, 0.0f,
            1.0f, -1.0f,  1.0f, 0.0f,
            -1.0f,  1.0f,  0.0f, 1.0f,
            1.0f, -1.0f,  1.0f, 0.0f,
            1.0f,  1.0f,  1.0f, 1.0f
    };

    public static boolean Init(boolean supportsVAO) {
        useVAO = supportsVAO;

        FloatBuffer vBuf = BufferUtils.createFloatBuffer(quadVerts.length);
        vBuf.put(quadVerts);
        vBuf.flip();

        quadVBO = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vBuf, GL15.GL_STATIC_DRAW);

        if (useVAO) {
            quadVAO = ARBVertexArrayObject.glGenVertexArrays();
            ARBVertexArrayObject.glBindVertexArray(quadVAO);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT,
                    false, 4 * 4, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT,
                    false, 4 * 4, 2 * 4);
            ARBVertexArrayObject.glBindVertexArray(0);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        return init = true;
    }

    public static void RenderQuad(int texID, int shaderID) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        ARBShaderObjects.glUseProgramObjectARB(shaderID);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID);

        int loc = ARBShaderObjects.glGetUniformLocationARB(shaderID,
                "screenTexture");
        ARBShaderObjects.glUniform1iARB(loc, 0);

        if (useVAO) {
            ARBVertexArrayObject.glBindVertexArray(quadVAO);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            ARBVertexArrayObject.glBindVertexArray(0);
        } else {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVBO);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT,
                    false, 4 * 4, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT,
                    false, 4 * 4, 2 * 4);

            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        ARBShaderObjects.glUseProgramObjectARB(0);
    }

    public static void Shutdown() {
        if (quadVBO != 0) {
            GL15.glDeleteBuffers(quadVBO);
            quadVBO = 0;
        }
        if (useVAO && quadVAO != 0) {
            ARBVertexArrayObject.glDeleteVertexArrays(quadVAO);
            quadVAO = 0;
        }
        init = false;
    }
}
