package production.tilemap;

import production.sprite.SpriteAtlas;
import production.sprite.SpritePalette;
import production.sprite.SpriteRenderer;
import production.sprite.SpritePool;

import java.util.HashMap;
import java.util.Map;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public final class TileMapLoader {
    public static Map<String, SpriteAtlas> atlases = new HashMap<>();
    public static Map<String, SpritePalette> palettes = new HashMap<>();

    public static void Load(TileMap map, HashMap<String, Integer> atlasIds,
                            HashMap<String, Integer> paletteIds, String paletteFileName) {
        int atlasId = atlasIds.get(map.atlasFilename);
        int paletteId = paletteIds.get(paletteFileName);
        SpriteAtlas atlas = SpriteRenderer.atlasArr[atlasId];
        /* TODO: fix this! */
        SpritePalette palette = SpriteRenderer.paletteArr[paletteId];
        int y, x;
        Tile t;
        if (atlas == null) {
            LogFatalAndExit("error");
            return;
        }

        if (palette == null) {
            LogFatalAndExit("error");
            return;
        }

        for (y = 0; y < map.height; ++y) {
            for (x = 0; x < map.width; ++x) {
                short tx = (short)(x + map.originOffsetX);
                short ty = (short)(y + map.originOffsetY);
                if ((t = map.getTile(tx, ty)) != null) {
                    t.spriteHandle = SpritePool.Create(tx * atlas.spriteSize,
                            ty * atlas.spriteSize, atlasId, t.spriteIdx,
                            7, paletteId, false, false, true);
                }
            }
        }
    }
}
