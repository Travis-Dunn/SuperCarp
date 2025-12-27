package production;

import production.sprite.SpriteAtlas;
import production.sprite.SpriteCamera;
import production.sprite.SpritePalette;
import production.tiledmap.TileMap;
import whitetail.graphics.Shader;
import whitetail.graphics.Sprite;
import whitetail.graphics.Texture;
import whitetail.graphics.cameras.Camera;
import whitetail.graphics.materials.MaterialD;

import java.util.HashMap;
import java.util.Map;

public class Data {
    /* game attribs */
    public static final int SPRITE_SIZE = 16;
    public static final int MAP_ATLAS = 0;
    public static final int MAP_PALETTE = 0;
    public static final int BLACK = 0xFF160D13;

    public static Texture charTex;
    public static Shader charShader;
    public static MaterialD charMaterial;
    public static Sprite charSprite;
    public static Camera cam;
    public static SpriteCamera sCam;
    public static byte[] fb;

    public static HashMap<String, Integer> atlasIdsByFilename = new HashMap<>();
    public static HashMap<String, Integer> paletteIdsByFilename = new HashMap<>();

    public static SpritePalette sp;
    public static SpriteAtlas sa;

    public static TileMap tileMap;
}
