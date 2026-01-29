package production.tilemap;

import production.Data;
import production.character.Char;
import production.character.CharRegistry;
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

        /* TODO: The char should know what atlas/palette it uses! */
        /* TODO: The other thing we really need to do here is check the GameCtx
        to see if there are any relevant persistent state changes to apply */
        for (Char c : map.charsByPos.values()) {
            c.setSpriteHandle(SpritePool.Create(
                    c.tileX * Data.SPRITE_SIZE,
                    c.tileY * Data.SPRITE_SIZE,
                    Data.PLAYER_ATLAS,
                    0,
                    1,
                    Data.MAP_PALETTE,
                    false,
                    false,
                    true));

            t = map.getTile((short)c.tileX, (short)c.tileY);
            if (t == null) {
                LogFatalAndExit(ErrStrFailedUpdateCharPosTileNull(c.tileX,
                        c.tileY, c.name, map.name));
            } else {
                t.setBlocked(true);
            }
        }
    }

    public static final String CLASS = TileMapLoader.class.getSimpleName();
    private static String ErrStrFailedUpdateCharPosTileNull(int x, int y,
            String name, String mapName) {
        return String.format("%s failed to update Char [%s] position to [%d, " +
                "%d] because the map [%s] did not have a tile with those " +
                "coordinates.\n", CLASS, name, x, y, mapName);
    }
}
