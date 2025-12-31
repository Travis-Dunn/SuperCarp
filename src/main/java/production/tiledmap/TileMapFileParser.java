package production.tiledmap;

import production.sprite.SpriteAtlas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        String line, key, value, parts[], originParts[];
        String mapName = "Untitled", atlasName = null;
        int width = 0, height = 0, originX = 0, originY = 0;
        int x, y, ax, ay, atlasIdx;
        boolean inTiles = false, headerParsed = false;
        Tile[][] tiles = null;
        SpriteAtlas atlas = null;
        boolean blocked = false;

        try {
            reader = new BufferedReader(new InputStreamReader(s));

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.equals("---")) {
                    if (width <= 0 || height <= 0) {
                        LogFatalAndExit(ErrStrInvalidHeader(f));
                        return null;
                    }
                    tiles = new Tile[height][width];
                    inTiles = true;
                    headerParsed = true;
                    continue;
                }

                if (!inTiles) {
                    // Header Parsing
                    parts = line.split(":", 2);
                    if (parts.length != 2) continue;

                    key = parts[0].trim();
                    value = parts[1].trim();

                    switch (key) {
                        case "name":
                            mapName = value;
                            break;
                        case "width":
                            width = Integer.parseInt(value);
                            break;
                        case "height":
                            height = Integer.parseInt(value);
                            break;
                        case "tileset":
                            atlasName = value;
                            // TODO: Load the actual SpriteAtlas object here using your AssetManager
                            // atlas = AssetManager.LoadAtlas(atlasName);
                            break;
                        case "origin":
                            originParts = value.split(",");
                            if (originParts.length == 2) {
                                originX = Integer.parseInt(originParts[0].trim());
                                originY = Integer.parseInt(originParts[1].trim());
                            }
                            break;
                    }
                } else {
                    // Body Parsing
                    parts = line.split(",");
                    if (parts.length < 3) continue;

                    try {
                        x = Integer.parseInt(parts[0].trim());
                        y = Integer.parseInt(parts[1].trim());
                        atlasIdx = Integer.parseInt(parts[2].trim());
                        blocked = parts[3].trim().equals("1");

                        // Convert World Coordinates to Array Indices
                        ax = x - originX;
                        ay = y - originY;

                        if (ax >= 0 && ax < width && ay >= 0 && ay < height) {
                            tiles[ay][ax] = new Tile((short) atlasIdx, -1
                            , blocked);
                        } else {
                            // Optionally log warning for tiles outside defined bounds
                        }

                    } catch (NumberFormatException nfe) {
                        LogFatalExcpAndExit(ErrStrCorruptData(f, line), nfe);
                        return null;
                    }
                }
            }

            if (!headerParsed) {
                LogFatalAndExit(ErrStrMissingSeparator(f));
                return null;
            }

            return new TileMap(mapName, width, height, atlasName, originX, originY, tiles);

        } catch (IOException e) {
            LogFatalExcpAndExit(ErrStrFailedLoad(f), e);
            return null;
        }
    }

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
}