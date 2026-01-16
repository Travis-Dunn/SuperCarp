package production.tilemap;

import production.monster.MonsterSpawn;
import production.monster.MonsterSpawnFileParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;

public final class TileMapFileParser {
    private static final String MAPS_DIR = "maps";

    public static TileMap FromFile(String filename) {
        assert(filename != null && !filename.isEmpty());

        String p = "/" + MAPS_DIR + "/" + filename;

        if (!filename.toLowerCase().endsWith(".map")) {
            LogFatalAndExit(ErrStrInvalidExtension(filename));
            return null;
        }

        try (InputStream stream =
                     TileMapFileParser.class.getResourceAsStream(p)) {
            if (stream == null) {
                LogFatalAndExit(ErrStrFailedLoad(filename));
                return null;
            }
            return FromStream(stream, filename);
        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(filename), e);
            return null;
        }
    }

    private static TileMap FromStream(InputStream s, String f) {
        BufferedReader reader;
        String line;
        List<MonsterSpawn> spawns = new ArrayList<MonsterSpawn>();

        // Header data
        String mapName = "Untitled", atlasName = null;
        int width = 0, height = 0, originX = 0, originY = 0;

        // Parsing state
        String currentSection = null; // null = header, "tiles", "examine", etc.
        Tile[][] tiles = null;

        try {
            reader = new BufferedReader(new InputStreamReader(s));

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Check for section delimiter
                if (line.startsWith("---")) {
                    if (currentSection == null) {
                        // End of header, start of tiles
                        if (width <= 0 || height <= 0) {
                            LogFatalAndExit(ErrStrInvalidHeader(f));
                            return null;
                        }
                        tiles = new Tile[height][width];
                        currentSection = "tiles";
                    } else {
                        // Named section (e.g., "--- examine")
                        String sectionName = line.substring(3).trim();
                        currentSection = sectionName.isEmpty() ? "tiles" : sectionName;
                    }
                    continue;
                }

                // Dispatch based on current section
                if (currentSection == null) {
                    int[] headerResult = parseHeaderLine(line, mapName, width, height,
                            atlasName, originX, originY);
                    if (headerResult != null) {
                        width = headerResult[0];
                        height = headerResult[1];
                        originX = headerResult[2];
                        originY = headerResult[3];
                    }
                    // Update string values via re-parsing (not elegant but simple)
                    String[] stringResult = parseHeaderLineStrings(line, mapName, atlasName);
                    mapName = stringResult[0];
                    atlasName = stringResult[1];

                } else if (currentSection.equals("tiles")) {
                    if (!parseTileLine(line, tiles, width, height, originX, originY, f)) {
                        return null; // Fatal error already logged
                    }

                } else if (currentSection.equals("examine")) {
                    parseExamineLine(line, tiles, width, height, originX, originY);

                } else if (currentSection.equals("spawns")) {
                    MonsterSpawn spawn = MonsterSpawnFileParser.FromLine(line);
                    if (spawn == null) {
                        LogFatalAndExit(ERR_STR_FAILED_PARSE_SPAWN);
                        return null;
                    }
                    spawns.add(spawn);
                }
                // Unknown sections are silently skipped (forward compatibility)
            }

            if (tiles == null) {
                LogFatalAndExit(ErrStrMissingSeparator(f));
                return null;
            }

            return new TileMap(mapName, width, height, atlasName, originX,
                    originY, tiles, spawns);

        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(f), e);
            return null;
        }
    }

    // =========================================================================
    // Header Parsing
    // =========================================================================

    /**
     * Parse numeric header values. Returns [width, height, originX, originY].
     */
    private static int[] parseHeaderLine(String line, String mapName,
                                         int width, int height, String atlasName, int originX, int originY) {

        String[] parts = line.split(":", 2);
        if (parts.length != 2) return new int[]{width, height, originX, originY};

        String key = parts[0].trim();
        String value = parts[1].trim();

        switch (key) {
            case "width":
                width = Integer.parseInt(value);
                break;
            case "height":
                height = Integer.parseInt(value);
                break;
            case "origin":
                String[] originParts = value.split(",");
                if (originParts.length == 2) {
                    originX = Integer.parseInt(originParts[0].trim());
                    originY = Integer.parseInt(originParts[1].trim());
                }
                break;
        }

        return new int[]{width, height, originX, originY};
    }

    /**
     * Parse string header values. Returns [mapName, atlasName].
     */
    private static String[] parseHeaderLineStrings(String line,
                                                   String mapName, String atlasName) {

        String[] parts = line.split(":", 2);
        if (parts.length != 2) return new String[]{mapName, atlasName};

        String key = parts[0].trim();
        String value = parts[1].trim();

        switch (key) {
            case "name":
                mapName = value;
                break;
            case "tileset":
                atlasName = value;
                break;
        }

        return new String[]{mapName, atlasName};
    }

    // =========================================================================
    // Tile Parsing
    // =========================================================================

    /**
     * Parse a tile line and add to tiles array.
     * Format: x\ty\tsprite\tblocked
     * Returns false on fatal error.
     */
    private static boolean parseTileLine(String line, Tile[][] tiles,
                                         int width, int height, int originX, int originY, String filename) {

        String[] parts = line.split("\t");
        if (parts.length < 4) return true; // Skip malformed lines

        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int atlasIdx = Integer.parseInt(parts[2].trim());
            boolean blocked = parts[3].trim().equals("1");

            // Convert world coordinates to array indices
            int ax = x - originX;
            int ay = y - originY;

            if (ax >= 0 && ax < width && ay >= 0 && ay < height) {
                tiles[ay][ax] = new Tile((short) atlasIdx, -1, blocked);
            }
            // Out of bounds tiles are silently ignored

        } catch (NumberFormatException nfe) {
            LogFatalExcpAndExit(ErrStrCorruptData(filename, line), nfe);
            return false;
        }

        return true;
    }

    // =========================================================================
    // Examine Section Parsing
    // =========================================================================

    /**
     * Parse an examine line and apply to existing tile.
     * Format: x\ty\texamine_text
     */
    private static void parseExamineLine(String line, Tile[][] tiles,
                                         int width, int height, int originX, int originY) {

        String[] parts = line.split("\t", 3);
        if (parts.length < 3) return;

        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            String examineText = parts[2]; // Don't trim - preserve intentional whitespace

            // Convert world coordinates to array indices
            int ax = x - originX;
            int ay = y - originY;

            if (ax >= 0 && ax < width && ay >= 0 && ay < height) {
                Tile tile = tiles[ay][ax];
                if (tile != null) {
                    tile.setExamine(examineText);
                }
            }

        } catch (NumberFormatException ignored) {
            // Skip malformed examine lines
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

    private static final String ERR_STR_FAILED_PARSE_SPAWN = CLASS + "failed " +
            "to parse a spawn.\n";
}