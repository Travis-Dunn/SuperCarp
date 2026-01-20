package production.tilemap;

import production.monster.MonsterSpawn;
import production.monster.MonsterSpawnFileParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;

public final class TileMapFileParser {
    private static final String MAPS_DIR = "maps";

    // =========================================================================
    // Extension Interfaces
    // =========================================================================

    /** Parses a single header field (key:value pair). */
    public interface HeaderFieldParser {
        void parse(String value, TileMapBuilder builder);
    }

    /** Parses lines within a named section. */
    public interface SectionParser {
        /**
         * Parse a single line within this section.
         * @return false if a fatal error occurred, true otherwise
         */
        boolean parseLine(String line, TileMapBuilder builder, String filename);
    }

    // =========================================================================
    // Parser Registries
    // =========================================================================

    private static final Map<String, HeaderFieldParser> headerParsers =
            new HashMap<String, HeaderFieldParser>();

    private static final Map<String, SectionParser> sectionParsers =
            new HashMap<String, SectionParser>();

    static {
        // Register built-in header field parsers
        registerHeaderParser("name", new HeaderFieldParser() {
            public void parse(String value, TileMapBuilder b) {
                b.mapName = value;
            }
        });

        registerHeaderParser("width", new HeaderFieldParser() {
            public void parse(String value, TileMapBuilder b) {
                b.width = Integer.parseInt(value);
            }
        });

        registerHeaderParser("height", new HeaderFieldParser() {
            public void parse(String value, TileMapBuilder b) {
                b.height = Integer.parseInt(value);
            }
        });

        registerHeaderParser("tileset", new HeaderFieldParser() {
            public void parse(String value, TileMapBuilder b) {
                b.atlasFilename = value;
            }
        });

        registerHeaderParser("origin", new HeaderFieldParser() {
            public void parse(String value, TileMapBuilder b) {
                String[] parts = value.split(",");
                if (parts.length == 2) {
                    b.originX = Integer.parseInt(parts[0].trim());
                    b.originY = Integer.parseInt(parts[1].trim());
                }
            }
        });

        registerHeaderParser("clear_color", new HeaderFieldParser() {
            public void parse(String value, TileMapBuilder b) {
                String hex = value.startsWith("#") ? value.substring(1) : value;
                if (hex.length() == 6) {
                    // RGB format - add full alpha
                    b.clearColor = (int) Long.parseLong("FF" + hex, 16);
                } else if (hex.length() == 8) {
                    // ARGB format
                    b.clearColor = (int) Long.parseLong(hex, 16);
                }
            }
        });

        // Register built-in section parsers
        registerSectionParser("tiles", new TilesSectionParser());
        registerSectionParser("examine", new ExamineSectionParser());
        registerSectionParser("spawns", new SpawnsSectionParser());
    }

    /** Register a custom header field parser. */
    public static void registerHeaderParser(String fieldName, HeaderFieldParser parser) {
        headerParsers.put(fieldName, parser);
    }

    /** Register a custom section parser. */
    public static void registerSectionParser(String sectionName, SectionParser parser) {
        sectionParsers.put(sectionName, parser);
    }

    // =========================================================================
    // Builder - Accumulates Parsed Data
    // =========================================================================

    public static final class TileMapBuilder {
        // Core fields
        public String mapName = "Untitled";
        public String atlasFilename = null;
        public int width = 0;
        public int height = 0;
        public int originX = 0;
        public int originY = 0;
        public Tile[][] tiles = null;
        public List<MonsterSpawn> spawns = new ArrayList<MonsterSpawn>();

        // Extended fields - add new fields here as needed
        public int clearColor = 0xFF000000;

        /** Called after header is complete, before tile parsing begins. */
        public void initTiles() {
            if (width > 0 && height > 0) {
                tiles = new Tile[height][width];
            }
        }

        public TileMap build() {
            return new TileMap(mapName, width, height, atlasFilename,
                    originX, originY, tiles, spawns, clearColor);
        }
    }

    // =========================================================================
    // Main Parsing Logic
    // =========================================================================

    public static TileMap FromFile(String filename) {
        assert(filename != null && !filename.isEmpty());

        String p = "/" + MAPS_DIR + "/" + filename;

        if (!filename.toLowerCase().endsWith(".map")) {
            LogFatalAndExit(ErrStrInvalidExtension(filename));
            return null;
        }

        try {
            InputStream stream = TileMapFileParser.class.getResourceAsStream(p);
            if (stream == null) {
                LogFatalAndExit(ErrStrFailedLoad(filename));
                return null;
            }
            try {
                return FromStream(stream, filename);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(filename), e);
            return null;
        }
    }

    private static TileMap FromStream(InputStream s, String filename) {
        BufferedReader reader;
        String line;
        TileMapBuilder builder = new TileMapBuilder();

        // Parsing state
        String currentSection = null; // null = header

        try {
            reader = new BufferedReader(new InputStreamReader(s));

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Check for section delimiter
                if (line.startsWith("---")) {
                    if (currentSection == null) {
                        // End of header - validate and init tiles
                        if (builder.width <= 0 || builder.height <= 0) {
                            LogFatalAndExit(ErrStrInvalidHeader(filename));
                            return null;
                        }
                        builder.initTiles();
                        currentSection = "tiles";
                    } else {
                        // Named section (e.g., "--- examine")
                        String sectionName = line.substring(3).trim();
                        currentSection = sectionName.isEmpty() ? "tiles" : sectionName;
                    }
                    continue;
                }

                // Dispatch to appropriate parser
                if (currentSection == null) {
                    // Header parsing
                    if (!parseHeaderLine(line, builder)) {
                        // Non-fatal: unknown header fields are ignored
                    }
                } else {
                    // Section parsing
                    SectionParser parser = sectionParsers.get(currentSection);
                    if (parser != null) {
                        if (!parser.parseLine(line, builder, filename)) {
                            return null; // Fatal error already logged
                        }
                    }
                    // Unknown sections are silently skipped (forward compatibility)
                }
            }

            if (builder.tiles == null) {
                LogFatalAndExit(ErrStrMissingSeparator(filename));
                return null;
            }

            return builder.build();

        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(filename), e);
            return null;
        }
    }

    /** Parse a header line using registered parsers. Returns false if field unknown. */
    private static boolean parseHeaderLine(String line, TileMapBuilder builder) {
        String[] parts = line.split(":", 2);
        if (parts.length != 2) {
            return false;
        }

        String key = parts[0].trim();
        String value = parts[1].trim();

        HeaderFieldParser parser = headerParsers.get(key);
        if (parser != null) {
            parser.parse(value, builder);
            return true;
        }

        return false;
    }

    // =========================================================================
    // Built-in Section Parsers
    // =========================================================================

    private static final class TilesSectionParser implements SectionParser {
        public boolean parseLine(String line, TileMapBuilder b, String filename) {
            String[] parts = line.split("\t");
            if (parts.length < 4) {
                return true; // Skip malformed lines
            }

            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int atlasIdx = Integer.parseInt(parts[2].trim());
                boolean blocked = parts[3].trim().equals("1");

                int ax = x - b.originX;
                int ay = y - b.originY;

                if (ax >= 0 && ax < b.width && ay >= 0 && ay < b.height) {
                    b.tiles[ay][ax] = new Tile((short) atlasIdx, -1, blocked);
                }

            } catch (NumberFormatException nfe) {
                LogFatalExcpAndExit(ErrStrCorruptData(filename, line), nfe);
                return false;
            }

            return true;
        }
    }

    private static final class ExamineSectionParser implements SectionParser {
        public boolean parseLine(String line, TileMapBuilder b, String filename) {
            String[] parts = line.split("\t", 3);
            if (parts.length < 3) {
                return true;
            }

            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                String examineText = parts[2]; // Preserve intentional whitespace

                int ax = x - b.originX;
                int ay = y - b.originY;

                if (ax >= 0 && ax < b.width && ay >= 0 && ay < b.height) {
                    Tile tile = b.tiles[ay][ax];
                    if (tile != null) {
                        tile.setExamine(examineText);
                    }
                }

            } catch (NumberFormatException ignored) {
                // Skip malformed examine lines
            }

            return true;
        }
    }

    private static final class SpawnsSectionParser implements SectionParser {
        public boolean parseLine(String line, TileMapBuilder b, String filename) {
            MonsterSpawn spawn = MonsterSpawnFileParser.FromLine(line);
            if (spawn == null) {
                LogFatalAndExit(ERR_STR_FAILED_PARSE_SPAWN);
                return false;
            }
            b.spawns.add(spawn);
            return true;
        }
    }

    // =========================================================================
    // Error Messages
    // =========================================================================

    private static final String CLASS =
            TileMapFileParser.class.getSimpleName();

    private static String ErrStrFailedLoad(String filename) {
        return String.format("%s failed to load file [%s].\n", CLASS, filename);
    }

    private static String ErrStrInvalidExtension(String filename) {
        return String.format("%s rejected file [%s]. Only .map files are " +
                "allowed.\n", CLASS, filename);
    }

    private static String ErrStrInvalidHeader(String filename) {
        return String.format("%s rejected [%s]. Header invalid or missing " +
                "dimensions.\n", CLASS, filename);
    }

    private static String ErrStrMissingSeparator(String filename) {
        return String.format("%s rejected [%s]. File format error: Missing " +
                "'---' separator.\n", CLASS, filename);
    }

    private static String ErrStrCorruptData(String filename, String line) {
        return String.format("%s failed parsing [%s]. Corrupt tile data: " +
                "[%s].\n", CLASS, filename, line);
    }

    private static final String ERR_STR_FAILED_PARSE_SPAWN = CLASS + " failed " +
            "to parse a spawn.\n";
}