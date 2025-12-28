package production;

import production.sprite.SpriteAtlas;
import production.sprite.SpriteCamera;
import production.sprite.SpritePalette;
import production.tiledmap.TileMap;

import java.util.HashMap;

public class Data {
    /* game attribs */
    public static final int SPRITE_SIZE = 16;
    public static final int MAP_ATLAS = 0;
    public static final int MAP_PALETTE = 0;
    public static final int BLACK = 0xFF160D13;
    public static final int FB_W = 320;
    public static final int FB_H = 240;
    public static final int SPRITE_SYS_CAP = 1024;
    public static final String TEST_ATLAS_FILENAME = "test_atlas.png";
    public static final String TEST_PALETTE_FILENAME = "mystic-16.png";

    public static SpriteCamera sCam;

    public static HashMap<String, Integer> atlasIdsByFilename = new HashMap<>();
    public static HashMap<String, Integer> paletteIdsByFilename = new HashMap<>();

    public static SpritePalette sp;
    public static SpriteAtlas sa;

    public static TileMap tileMap;
}
