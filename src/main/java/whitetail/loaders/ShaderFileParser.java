package whitetail.loaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShaderFileParser {
    private static final String SHADERS_DIR = "shaders";

    public static String FromFile(String filename) {
        String path = "/" + SHADERS_DIR + "/" + filename;
        InputStream stream = ShaderFileParser.class.getResourceAsStream(path);
        if (stream == null)
            throw new RuntimeException("Shader file not found: " + filename);

        try {
            return StringFromStream(stream);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String StringFromStream(InputStream stream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read from stream: " + e);
        }
    }
}
