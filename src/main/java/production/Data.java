package production;

import production.scenes.SceneGame;
import production.sprite.SpriteAtlas;
import production.sprite.SpriteCamera;
import production.sprite.SpritePalette;
import production.tilemap.TileMap;
import whitetail.audio.Audio;
import whitetail.audio.AudioBuffer;
import whitetail.loaders.config.ConfigEntry;

import java.util.ArrayList;
import java.util.HashMap;

public class Data {
    /* game attribs */
    public static final int SPRITE_SIZE = 16;
    public static final int MAP_ATLAS = 0;
    public static final int PLAYER_ATLAS = 1;
    public static final int MAP_PALETTE = 0;
    public static final int BLACK = 0xFF160D13;
    public static final int FB_W = 320;
    public static final int FB_H = 240;
    public static final int SPRITE_SYS_CAP = 1024;
    public static final String TEST_ATLAS_FILENAME = "test_atlas.png";
    public static final String TEST_PALETTE_FILENAME = "mystic-16-mod-17.png";
    public static final String TEST_ATLAS_PLAYER_FILENAME = "test_atlas_player_anim.png";

    public static final String STR_ATLAS_FILENAME_0 = "atlas_0.png";
    public static final String STR_ATLAS_FILENAME_1 = "atlas_1.png";
    public static final String STR_ATLAS_FILENAME_2 = "atlas_2.png";
    public static final String STR_ATLAS_FILENAME_3 = "atlas_3.png";
    public static final String STR_ATLAS_FILENAME_4 = "atlas_4.png";
    public static final String STR_ATLAS_FILENAME_5 = "atlas_5.png";
    public static final String STR_ATLAS_FILENAME_6 = "atlas_6.png";
    public static final String STR_ATLAS_FILENAME_7 = "atlas_7.png";
    public static final int IDX_ATLAS_MAP = 0;
    public static final int IDX_ATLAS_PROP = 1;
    public static final int IDX_ATLAS_CHAR = 2;
    public static final int IDX_ATLAS_ = 3;

    public static SpriteCamera sCam;

    public static HashMap<String, Integer> atlasIdsByFilename = new HashMap<>();
    public static HashMap<String, Integer> paletteIdsByFilename = new HashMap<>();

    public static SpritePalette sp;
    public static SpriteAtlas sa;
    public static SpriteAtlas sa_player;

    public static TileMap tileMap;

    public static Cursor cursor;

    public static AudioBuffer testMusicBuf;
    public static Audio testMusic;

    public static SceneGame sceneGame;

    public static ArrayList<ConfigEntry> cfgEntries;
}
