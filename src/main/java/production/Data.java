package production;

import production.monster.MonsterSpawn;
import production.scene.SceneGame;
import production.sprite.SpriteAtlas;
import production.sprite.SpriteCamera;
import production.sprite.SpritePalette;
import production.tilemap.TileMap;
import production.ui.FontAtlas;
import whitetail.audio.Audio;
import whitetail.audio.AudioBuffer;
import whitetail.loaders.config.ConfigEntry;

import java.util.ArrayList;
import java.util.HashMap;

public final class Data {
    /* game attribs */
    public static final int SPRITE_SIZE = 16;
    public static final int MAP_ATLAS = 0;
    public static final int PLAYER_ATLAS = 1;
    public static final int MAP_PALETTE = 0;
    public static final int BLACK = 0xFF160D13;
    /**
     * This combo of framebuffer size and pixel scale yields a 1920x1080 window.
     * TODO: make a module that reads monitor settings and sets framebuffer,
     * pixel scale, and window resolution appropriately. This will need to take
     * setting such as 'fullscreen' into account. These will be read from the
     * config file, which will be editable via in game menus in the future.
     *
     * We also want to render the game viewport to a subsection of the window,
     * since there is no need to draw it underneath all the ui. This means the
     * introduction of a few new variables.
     */
    public static final int FB_W = 480;
    public static final int FB_H = 270;
    public static final int PIXEL_SCALE = 4;
    public static final int WINDOW_W = PIXEL_SCALE * FB_W;
    public static final int WINDOW_H = PIXEL_SCALE * FB_H;
    public static final int SPRITE_SYS_CAP = 2048;
    public static final String TEST_ATLAS_FILENAME = "test_atlas.png";
    public static final String TEST_PALETTE_FILENAME = "mystic-16-mod-17.png";
    public static final String TEST_ATLAS_ANIM_FILENAME = "test_atlas_anims.png";

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

    public static MonsterSpawn testSpawn;

    public static int clearColor = BLACK;

    public static FontAtlas fontAtlas;

    public static int screenMouseX;
    public static int screenMouseY;
}
