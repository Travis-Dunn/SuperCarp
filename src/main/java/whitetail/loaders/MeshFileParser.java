package whitetail.loaders;

import whitetail.graphics.Mesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshFileParser {
    private static final String MODELS_DIR = "models";
    /*
        - Must be triangulated
        - Supports: official wavefront .obj format, i.e. position, uvs, normals
    */
    public static Mesh FromFile(String filename) {
        String path = "/" + MODELS_DIR + "/" + filename;
        InputStream stream = MeshFileParser.class.getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Model not found: " + filename);
        }
        String extension = ExtractExtension(filename);
        
        switch(extension) {
            case "obj":
                return FromObjStream(stream, filename);
            default:
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } throw new RuntimeException("Unsupported 3d file format: " +
                    extension);
        }
    }

    private static Mesh FromObjStream(InputStream stream, String filename) {
        List<Float>         positions       = new ArrayList<Float>();
        List<Float>         uvs             = new ArrayList<Float>();
        List<Float>         normals         = new ArrayList<Float>();
        List<VertexIndices> faceVertices    = new ArrayList<VertexIndices>();

        boolean hasUvs = false;
        boolean hasNormals = false;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(stream));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("v ")) {
                    String[] parts = line.split("\\s+");
                    positions.add(Float.parseFloat(parts[1]));
                    positions.add(Float.parseFloat(parts[2]));
                    positions.add(Float.parseFloat(parts[3]));

                } else if (line.startsWith("vt ")) {
                    String[] parts = line.split("\\s+");
                    uvs.add(Float.parseFloat(parts[1]));
                    uvs.add(Float.parseFloat(parts[2]));
                    hasUvs = true;

                } else if (line.startsWith("vn ")) {
                    String[] parts = line.split("\\s+");
                    normals.add(Float.parseFloat(parts[1]));
                    normals.add(Float.parseFloat(parts[2]));
                    normals.add(Float.parseFloat(parts[3]));
                    hasNormals = true;

                } else if (line.startsWith("f ")) {
                    String[] parts = line.split("\\s+");

                    for (int i = 1; i < parts.length; i++) {
                        String[] vData = parts[i].split("/");

                        VertexIndices vi = new VertexIndices();

                        vi.posIdx = Integer.parseInt(vData[0]) - 1;

                        if (vData.length > 1 && !vData[1].isEmpty()) {
                            vi.uvIdx = Integer.parseInt(vData[1]) - 1;
                        } else {
                            vi.uvIdx = -1;
                        }

                        if (vData.length > 2 && !vData[2].isEmpty()) {
                            vi.normIdx = Integer.parseInt(vData[2]) - 1;
                        } else {
                            vi.normIdx = -1;
                        }

                        faceVertices.add(vi);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read obj file: " + filename, e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to parse number in obj file: " + filename, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }

        return buildIndexedMeshOBJ(positions, uvs, normals, faceVertices, hasUvs,
                hasNormals, filename);
    }

    private static Mesh buildIndexedMeshOBJ(List<Float> positions, List<Float> uvs,
            List<Float> normals, List<VertexIndices> faceVertices,
            boolean hasUvs, boolean hasNormals, String filename) {
        int stride = 3;
        if (hasUvs) stride += 2;
        if (hasNormals) stride += 3;

        Map<VertexIndices, Integer> uniqueVertices =
                new HashMap<VertexIndices, Integer>();
        List<Float> vertexData = new ArrayList<Float>();
        List<Integer> indices = new ArrayList<Integer>();

        for (VertexIndices vi : faceVertices) {
            Integer existingIndex = uniqueVertices.get(vi);

            if (existingIndex != null) {
                indices.add(existingIndex);
            } else {
                int newIndex = vertexData.size() / stride;
                uniqueVertices.put(vi, newIndex);
                indices.add(newIndex);

                int posIdx = vi.posIdx * 3;
                vertexData.add(positions.get(posIdx));
                vertexData.add(positions.get(posIdx + 1));
                vertexData.add(positions.get(posIdx + 2));

                if (hasUvs) {
                    if (vi.uvIdx >= 0) {
                        int uvIdx = vi.uvIdx * 2;
                        vertexData.add(uvs.get(uvIdx));
                        vertexData.add(uvs.get(uvIdx + 1));
                    } else {
                        vertexData.add(0.0f);
                        vertexData.add(0.0f);
                    }
                }

                if (hasNormals) {
                    if (vi.normIdx >= 0) {
                        int normIdx = vi.normIdx * 3;
                        vertexData.add(normals.get(normIdx));
                        vertexData.add(normals.get(normIdx + 1));
                        vertexData.add(normals.get(normIdx + 2));
                    } else {
                        vertexData.add(0.0f);
                        vertexData.add(1.0f);
                        vertexData.add(0.0f);
                    }
                }
            }
        }

        float[] vData = new float[vertexData.size()];
        for (int i = 0; i < vertexData.size(); i++) {
            vData[i] = vertexData.get(i);
        }

        int[] indicesArr = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indicesArr[i] = indices.get(i);
        }

        System.out.println("Loaded mesh: " + filename + ", " + (vData.length / stride) +
                " vertices, " + (indicesArr.length / 3) + " triangles");

        return new Mesh(vData, indicesArr, stride, hasUvs, hasNormals,
            false, false, false);
    }

    private static class VertexIndices {
        int posIdx;
        int uvIdx;
        int normIdx;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof VertexIndices)) return false;
            VertexIndices other = (VertexIndices) obj;
            return posIdx == other.posIdx &&
                    uvIdx == other.uvIdx &&
                    normIdx == other.normIdx;
        }

        @Override
        public int hashCode() {
            return posIdx * 73856093 + uvIdx * 19349663 + normIdx * 83492791;
        }
    }

    private static String ExtractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1)
            return "";
        return filename.substring(lastDot + 1).toLowerCase();
    }
}