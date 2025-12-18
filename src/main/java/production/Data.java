package production;

import production.sprite.SpriteAtlas1;
import production.sprite.SpriteCamera;
import production.sprite.SpritePalette;
import whitetail.graphics.Shader;
import whitetail.graphics.Sprite;
import whitetail.graphics.Texture;
import whitetail.graphics.cameras.Camera;
import whitetail.graphics.materials.MaterialD;

public class Data {
    public static Texture charTex;
    public static Shader charShader;
    public static MaterialD charMaterial;
    public static Sprite charSprite;
    public static Camera cam;
    public static SpriteCamera sCam;
    public static byte[] fb;

    public static SpritePalette sp;
    public static SpriteAtlas1 sa;
}
