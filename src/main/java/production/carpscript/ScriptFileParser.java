package production.carpscript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;

public final class ScriptFileParser {
    private static final String SCRIPTS_DIR = "scripts";

    public static List<CompiledScript> FromFile(String filename) {
        assert(filename != null && !filename.isEmpty());

        String p = "/" + SCRIPTS_DIR + "/" + filename;

        if (!filename.toLowerCase().endsWith(".cs2")) {
            LogFatalAndExit(ErrStrInvalidExtension(filename));
            return null;
        }

        InputStream stream = null;
        try {
            stream = ScriptFileParser.class.getResourceAsStream(p);
            if (stream == null) {
                LogFatalAndExit(ErrStrFailedLoad(filename));
                return null;
            }
            return FromStream(stream, filename);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static List<CompiledScript> FromStream(InputStream s, String f) {
        String source;

        try {
            source = ReadStream(s);
        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(f), e);
            return null;
        }

        if (source.trim().isEmpty()) {
            LogFatalAndExit(ErrStrEmptyFile(f));
            return null;
        }

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens, f);
        return parser.parseFile();
    }

    private static String ReadStream(InputStream s) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(s));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static final String CLASS =
            ScriptFileParser.class.getSimpleName();

    private static String ErrStrFailedLoad(String filename) {
        return String.format("%s failed to load file [%s].\n", CLASS, filename);
    }

    private static String ErrStrInvalidExtension(String filename) {
        return String.format("%s rejected file [%s]. Only .cs2 files are " +
                "allowed.\n", CLASS, filename);
    }

    private static String ErrStrEmptyFile(String filename) {
        return String.format("%s rejected [%s]. The file is empty.\n",
                CLASS, filename);
    }
}