package whitetail.graphics;

import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public class Mesh {
    /*
    Indices[i] refers to vertices[i * stride] through vertices[(i * stride) +
    stride - 1].

    Stride varies depending on the per-vertex attribs that are present. The only
    information that is guaranteed to be present is position, and thus stride
    is at least 3, but may be up to 14 if all possible attribs are present.

    The ordering of the attribs is determined by which are present, and may be
    queried with getters such as 'getUvOffset'. The layout is:
    position, uvs, normals, bitangents, colors.

    Whitetail is only ever expected to support triangulated meshes, so the
    stride of indices is always 3. I.E. indices 0, 1, and 2 form the first
    triangle, etc.

    Attrib offsets and stride are expressed in floats, not bytes.
     */
    private float[]   vertices;
    private int[]     indices;

    private int       stride;
    private final boolean   hasUvs;
    private final boolean   hasNormals;
    private boolean   hasTangents;
    private boolean   hasBitangents;
    private final boolean   hasColors;
    /*
        position        3
        uv              2
        normal          3
        tangent         3
        bi-tangent      3
        color           3
     */
    private static final int MAX_STRIDE = 17;
    private static boolean USE_VAO;

    private int vaoID = -1;
    private int vboID = -1;
    private int iboID = -1;

    private final int vertCount;
    private final int triCount;
    public final int indexCount;

    public Mesh(float[] verts, int[] indices, int stride, boolean hasUvs,
                boolean hasNormals, boolean hasTangents, boolean hasBitangents,
                boolean hasColors) {
        assert(indices != null);
        assert(verts != null);
        assert(stride >= 3 && stride < MAX_STRIDE);
        this.vertices = verts;
        this.indices = indices;
        this.stride = stride;
        this.hasUvs = hasUvs;
        this.hasNormals = hasNormals;
        this.hasTangents = hasTangents;
        this.hasBitangents = hasBitangents;
        this.hasColors = hasColors;
        this.vertCount = this.vertices.length / this.stride;
        this.triCount = this.indices.length / 3;
        indexCount = indices.length;
    }

    public static void SetVaoCompatibility(boolean useVao) {
        USE_VAO = useVao;
    }

    public float[]  getVertices()      { return vertices; }
    public int[]    getIndices()       { return indices; }
    public int      getStride()        { return stride; }
    public boolean  hasUvs()           { return hasUvs; }
    public boolean  hasNormals()       { return hasNormals; }
    public boolean  hasTangents()      { return hasTangents; }
    public boolean  hasBitangents()    { return hasBitangents; }
    public boolean  hasColors()        { return hasColors; }
    public int      getVertCount()     { return vertCount; }
    public int      getTriCount()      { return triCount; }
    public int      getIndexCount()    { return indices.length; }
    public int      getVaoID()         { return vaoID; }
    public int      getVboID()         { return vboID; }
    public int      getIboID()         { return iboID; }
    public boolean  isUploaded()       { return vaoID != -1; }

    public int getPositionOffset() {
        return 0;
    }

    public int getUvOffset() {
        if (!hasUvs) return -1;
        return 3;
    }

    public int getNormalOffset() {
        if (!hasNormals) return -1;
        int offset = 3;
        if (hasUvs) offset += 2;
        return offset;
    }

    public int getTangentOffset() {
        if (!hasTangents) return -1;
        int offset = 3;
        if (hasUvs) offset += 2;
        if (hasNormals) offset += 3;
        return offset;
    }

    public int getBitangentOffset() {
        if (!hasBitangents) return -1;
        int offset = 3; // After position
        if (hasUvs) offset += 2; // After UVs
        if (hasNormals) offset += 3; // After normals
        if (hasTangents) offset += 3;
        return offset;
    }

    public int getColorOffset() {
        if (!hasColors) return -1;
        int offset = 3; // After position
        if (hasUvs) offset += 2; // After UVs
        if (hasNormals) offset += 3; // After normals
        if (hasTangents) offset += 3;
        if (hasBitangents) offset += 3; // After bitangents
        return offset;
    }

    public void setHandles(int vaoID, int vboID, int iboID) {
        this.vaoID = vaoID;
        this.vboID = vboID;
        this.iboID = iboID;
    }

    public void freeData() {
        this.vertices = null;
        this.indices = null;
    }

    @Override
    public String toString() {
        return "Mesh{vertCount=" + vertCount + ", triCount=" + triCount +
                ", stride=" + stride + ", hasUvs=" + hasUvs + ", hasNormals="
                + hasNormals + ", hasTangents=" + hasTangents +
                ", hasBitangents=" + hasBitangents + ", hasColors=" + hasColors
                + ", isUploaded=" + isUploaded() + ", isFreed=" +
                (vertices == null && indices == null) + "}";
    }

    public void generateTangents() {
        assert(hasUvs);
        assert(vertices != null && indices != null);
        assert(!hasTangents && !hasBitangents);

        int oldStride = stride;
        int newStride = stride + 6;
        int vCount = vertCount;

        float[] tanArr = new float[vCount * 3];
        float[] bitanArr = new float[vCount * 3];

        int uvOffset = getUvOffset();
        int posOffset = getPositionOffset();
        
        for (int i = 0; i < triCount * 3; i += 3) {
            int i0 = indices[i + 0];
            int i1 = indices[i + 1];
            int i2 = indices[i + 2];

            int base0 = i0 * oldStride;
            int base1 = i1 * oldStride;
            int base2 = i2 * oldStride;

            float p0x = vertices[base0 + posOffset + 0];
            float p0y = vertices[base0 + posOffset + 1];
            float p0z = vertices[base0 + posOffset + 2];
            float p1x = vertices[base1 + posOffset + 0];
            float p1y = vertices[base1 + posOffset + 1];
            float p1z = vertices[base1 + posOffset + 2];
            float p2x = vertices[base2 + posOffset + 0];
            float p2y = vertices[base2 + posOffset + 1];
            float p2z = vertices[base2 + posOffset + 2];

            float u0 = vertices[base0 + uvOffset + 0];
            float v0 = vertices[base0 + uvOffset + 1];
            float u1 = vertices[base1 + uvOffset + 0];
            float v1 = vertices[base1 + uvOffset + 1];
            float u2 = vertices[base2 + uvOffset + 0];
            float v2 = vertices[base2 + uvOffset + 1];

            float edge0x = p1x - p0x, edge0y = p1y - p0y, edge0z = p1z - p0z;
            float edge1x = p2x - p0x, edge1y = p2y - p0y, edge1z = p2z - p0z;
            float du0 = u1 - u0, dv0 = v1 - v0;
            float du1 = u2 - u0, dv1 = v2 - v0;

            float f = du0 * dv1 - du1 * dv0;
            if (Math.abs(f) < 1e-6f) continue;
            f = 1.0f / f;

            float tx = f * (dv1 * edge0x - dv0 * edge1x);
            float ty = f * (dv1 * edge0y - dv0 * edge1y);
            float tz = f * (dv1 * edge0z - dv0 * edge1z);
            float bx = f * (-du1 * edge0x + du0 * edge1x);
            float by = f * (-du1 * edge0y + du0 * edge1y);
            float bz = f * (-du1 * edge0z + du0 * edge1z);

            int a = i0 * 3, b = i1 * 3, c = i2 * 3;
            tanArr[a] += tx; tanArr[a + 1] += ty; tanArr[a + 2] += tz;
            bitanArr[a] += bx; bitanArr[a + 1] += by; bitanArr[a + 2] += bz;
            tanArr[b] += tx; tanArr[b + 1] += ty; tanArr[b + 2] += tz;
            bitanArr[b] += bx; bitanArr[b + 1] += by; bitanArr[b + 2] += bz;
            tanArr[c] += tx; tanArr[c + 1] += ty; tanArr[c + 2] += tz;
            bitanArr[c] += bx; bitanArr[c + 1] += by; bitanArr[c + 2] += bz;
        }

        float[] newVerts = new float[vCount * newStride];
        for (int v = 0; v < vCount; v++) {
            int oldBase = v * oldStride;
            int newBase = v * newStride;

            System.arraycopy(vertices, oldBase, newVerts, newBase, oldStride);

            int tIdx = v * 3;
            float tx = tanArr[tIdx + 0];
            float ty = tanArr[tIdx + 1];
            float tz = tanArr[tIdx + 2];
            float tLen = (float)Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (tLen > 1e-6f) { tx /= tLen; ty /= tLen; tz /= tLen; }
            newVerts[newBase + oldStride + 0] = tx;
            newVerts[newBase + oldStride + 1] = ty;
            newVerts[newBase + oldStride + 2] = tz;

            float bx = bitanArr[tIdx + 0];
            float by = bitanArr[tIdx + 1];
            float bz = bitanArr[tIdx + 2];
            float bLen = (float)Math.sqrt(bx * bx + by * by + bz * bz);
            if (bLen > 1e-6f) { bx /= bLen; by /= bLen; bz /= bLen; }
            newVerts[newBase + oldStride + 3] = bx;
            newVerts[newBase + oldStride + 4] = by;
            newVerts[newBase + oldStride + 5] = bz;
        }
        vertices = newVerts;
        stride = newStride;
        hasTangents = hasBitangents = true;
    }

    public boolean upload() {
        assert(!isUploaded());
        assert(vertices != null);
        assert(indices != null);
        assert(stride > 0 && stride <= MAX_STRIDE);
        assert(vertCount > 0);
        assert(triCount > 0);

        int vaoID = 0, vboID = 0, iboID = 0;

        try {
            if (USE_VAO) {
                vaoID = ARBVertexArrayObject.glGenVertexArrays();
                ARBVertexArrayObject.glBindVertexArray(vaoID);
            }

            vboID = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
                    MakeFloatBuffer(vertices), GL15.GL_STATIC_DRAW);

            int stride = this.stride * 4;

            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride,
                    getPositionOffset() * 4L);
            GL20.glEnableVertexAttribArray(0);

            if (hasUvs()) {
                GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT,
                        false, stride,
                        getUvOffset() * 4L);
                GL20.glEnableVertexAttribArray(1);
            }

            if (hasNormals()) {
                GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT,
                        false, stride,
                        getNormalOffset() * 4L);
                GL20.glEnableVertexAttribArray(2);
            }

            if (hasTangents()) {
                GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT,
                        false, stride,
                        getTangentOffset() * 4L);
                GL20.glEnableVertexAttribArray(3);
            }

            if (hasBitangents()) {
                GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT,
                        false, stride,
                        getBitangentOffset() * 4L);
                GL20.glEnableVertexAttribArray(4);
            }

            if (hasColors()) {
                GL20.glVertexAttribPointer(5, 3, GL11.GL_FLOAT,
                        false, stride,
                        getColorOffset() * 4L);
                GL20.glEnableVertexAttribArray(5);
            }

            iboID = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboID);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER,
                    MakeIntBuffer(indices), GL15.GL_STATIC_DRAW);

            if (USE_VAO) ARBVertexArrayObject.glBindVertexArray(0);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

            this.vaoID = vaoID;
            this.vboID = vboID;
            this.iboID = iboID;

            System.out.println("Uploaded mesh with vaoID=" + vaoID +
                    ", vboID=" + vboID + ", iboID=" + iboID);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to upload mesh. " + e.getMessage());
            e.printStackTrace();

            if (vaoID != 0) ARBVertexArrayObject
                    .glDeleteVertexArrays(vaoID);
            if (vboID != 0) GL15.glDeleteBuffers(vboID);
            if (iboID != 0) GL15.glDeleteBuffers(iboID);

            return false;
        }
    }

    private static java.nio.FloatBuffer MakeFloatBuffer(float[] data) {
        java.nio.FloatBuffer buf = java.nio.ByteBuffer
                .allocateDirect(data.length * 4)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(data).flip();
        return buf;
    }

    private static java.nio.IntBuffer MakeIntBuffer(int[] data) {
        java.nio.IntBuffer buf = java.nio.ByteBuffer
                .allocateDirect(data.length * 4)
                .order(java.nio.ByteOrder.nativeOrder())
                .asIntBuffer();
        buf.put(data).flip();
        return buf;
    }

    public void destroy() {
        assert(isUploaded());

        if (vaoID != -1) ARBVertexArrayObject.glDeleteVertexArrays(vaoID);
        if (vboID != -1) GL15.glDeleteBuffers(vboID);
        if (iboID != -1) GL15.glDeleteBuffers(iboID);

        vaoID = vboID = iboID = -1;
    }

}