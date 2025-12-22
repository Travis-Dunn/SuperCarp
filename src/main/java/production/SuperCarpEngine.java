package production;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector3f;
import production.sprite.*;
import production.tiledmap.Tile;
import production.tiledmap.TileMapFileParser;
import production.tiledmap.TileMapLoader;
import whitetail.core.GameEngine;
import whitetail.event.Event;
import whitetail.event.EventListener;
import whitetail.event.EventType;
import whitetail.event.KeyboardEvent;
import whitetail.graphics.Shader;
import whitetail.graphics.Sprite;
import whitetail.graphics.cameras.Camera;
import whitetail.graphics.materials.MaterialD;
import whitetail.loaders.TextureFileParser;

public class SuperCarpEngine extends GameEngine implements EventListener {

    private int playerSprite;
    private float playerX = 100.0f;
    private float playerY = 100.0f;
    private static final float MOVE_SPEED = 120.0f;  // pixels per second

    public SuperCarpEngine() { super(); }

    @Override
    protected void onProcessInput() {
        while (Keyboard.next()) {
            boolean keyDown = Keyboard.getEventKeyState();
            int keyCode = Keyboard.getEventKey();
            char keyChar = Keyboard.getEventCharacter();
            boolean repeat = Keyboard.isRepeatEvent();

            /* only handle keydown for now */
            EventType eventType = keyDown ? EventType.KEYDOWN : EventType.KEYUP;
            KeyboardEvent keyEvent = new KeyboardEvent(eventType, keyCode,
                    keyChar, repeat);
            eventManager.fireEvent(keyEvent);
        }
    }

    @Override
    protected boolean onInit() {

        eventManager.addEventListener(this);

        Data.charTex = TextureFileParser.FromFile("character.png");
        Data.charTex.upload();
        Data.charTex.freeData();

        Shader.AttribLoc locs[] = {
                new Shader.AttribLoc(0, "position"),
                new Shader.AttribLoc(1, "texCoord"),
                new Shader.AttribLoc(2, "normal"),
        };

        Data.charShader = new Shader("example_vert.glsl",
                "example_frag.glsl", locs);

        Data.charMaterial = new MaterialD(Data.charShader, Data.charTex);

        Data.charSprite = new Sprite(new Vector3f(16, 1, 16),
                new Vector3f(-90, 0, 0),
                new Vector3f(400.0f, 300.0f, -1.0f), Data.charMaterial);

        Data.charSprite.setRenderStatus(true);

        Data.cam = Camera.MakeMenu(800.0f, 600.0f, 0.1f, 10f);

        final int FB_WIDTH = 320;
        final int FB_HEIGHT = 240;
        final int SPRITE_SIZE = 16;
        final int ATLAS_SIZE = 32;
        SpriteSysOld.Init(1024);
        SpriteSys.Init(1024);
        SpriteAtlasOld.Init(ATLAS_SIZE, ATLAS_SIZE, SPRITE_SIZE);
        SpriteRenderer.Init(FB_WIDTH, FB_HEIGHT);
        SpriteBackend.Init(FB_WIDTH, FB_HEIGHT);

        // Create camera
        Data.sCam = new SpriteCamera();
        Data.sCam.init(FB_WIDTH, FB_HEIGHT);
        SpriteRenderer.SetCamera(Data.sCam);

        // Create test palette
        int[] palette = new int[256];
        palette[0] = 0x00000000;  // transparent
        palette[1] = 0xFFFF0000;  // red
        palette[2] = 0xFF00FF00;  // green
        palette[3] = 0xFF0000FF;  // blue
        palette[4] = 0xFFFFFF00;  // yellow
        SpriteAtlasOld.SetPalette(palette);

        // Create test atlas: 32x32 pixels, 4 sprites (2x2 grid)
// Sprite 0 (top-left): red square with transparent border
// Sprite 1 (top-right): green square with transparent border
// Sprite 2 (bottom-left): blue square with transparent border
// Sprite 3 (bottom-right): yellow diagonal pattern
        byte[] atlasPixels = new byte[ATLAS_SIZE * ATLAS_SIZE];
        for (int y = 0; y < ATLAS_SIZE; y++) {
            for (int x = 0; x < ATLAS_SIZE; x++) {
                int spriteX = x / SPRITE_SIZE;  // 0 or 1
                int spriteY = y / SPRITE_SIZE;  // 0 or 1
                int localX = x % SPRITE_SIZE;
                int localY = y % SPRITE_SIZE;
                int idx = y * ATLAS_SIZE + x;

                // 2-pixel transparent border
                if (localX < 2 || localX >= 14 || localY < 2 || localY >= 14) {
                    atlasPixels[idx] = 0;  // transparent
                } else if (spriteX == 0 && spriteY == 0) {
                    atlasPixels[idx] = 1;  // red
                } else if (spriteX == 1 && spriteY == 0) {
                    atlasPixels[idx] = 2;  // green
                } else if (spriteX == 0 && spriteY == 1) {
                    atlasPixels[idx] = 3;  // blue
                } else {
                    // diagonal pattern for sprite 3
                    atlasPixels[idx] = (byte)(((localX + localY) % 2 == 0) ? 4 : 1);
                }
            }
        }
        SpriteAtlasOld.SetPixels(atlasPixels);

        /*
        Data.fb = new byte[320 * 240 * 4];
        for (int y = 0; y < 240; y++) {
            for (int x = 0; x < 320; x++) {
                int i = (y * 320 + x) * 4;
                Data.fb[i]     = (byte)((x * 255) / 319);  // R
                Data.fb[i + 1] = (byte)((y * 255) / 239);  // G
                Data.fb[i + 2] = 0;                         // B
                Data.fb[i + 3] = (byte)0xFF;                // A (opaque)
            }
        }
         */
        // Create some test sprites
        int s0 = SpriteSysOld.Create(50, 50, (byte)0, (short)0);    // red, front layer
        int s1 = SpriteSysOld.Create(60, 60, (byte)1, (short)1);    // green, behind red
        int s2 = SpriteSysOld.Create(100, 50, (byte)0, (short)2);   // blue
        playerSprite = SpriteSysOld.Create((int)playerX, (int)playerY, (byte)0, (short)3);   // yellow diagonal
        int s4 = SpriteSysOld.Create(200, 50, (byte)0, (short)3);   // same, flipped H
        SpriteSysOld.SetFlipH(s4, true);
        int s5 = SpriteSysOld.Create(250, 50, (byte)0, (short)3);   // same, flipped V
        SpriteSysOld.SetFlipV(s5, true);

        String atlasFilename = "test_atlas.png";
        String paletteFilename = "mystic-16.png";

        Data.sp = SpritePaletteFileParser.FromFile(paletteFilename);
        if (Data.sp == null) return false;
        Data.sa = SpriteAtlasFileParser.FromFile(atlasFilename,
            Data.SPRITE_SIZE, Data.sp);
        if (Data.sa == null) return false;

        Data.atlasIdsByFilename.put(atlasFilename, Data.MAP_ATLAS);
        Data.paletteIdsByFilename.put(paletteFilename, Data.MAP_PALETTE);

        SpriteRenderer.paletteArr[Data.MAP_PALETTE] = Data.sp;
        SpriteRenderer.atlasArr[Data.MAP_ATLAS] = Data.sa;

        Data.tileMap = TileMapFileParser.FromFile("test_map.map");
        if (Data.tileMap == null) return false;

        TileMapLoader.Load(Data.tileMap, Data.atlasIdsByFilename, Data.paletteIdsByFilename);

        return true;
    }

    @Override
    protected void onUpdate(double delta) {
        float dx = 0.0f;
        float dy = 0.0f;

        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT))  dx -= 1.0f;
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) dx += 1.0f;
        if (Keyboard.isKeyDown(Keyboard.KEY_UP))    dy -= 1.0f;
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))  dy += 1.0f;

        if (dx != 0.0f || dy != 0.0f) {
            playerX += dx * MOVE_SPEED * (float)delta;
            playerY += dy * MOVE_SPEED * (float)delta;
            SpriteSysOld.SetPos(playerSprite, (int)playerX, (int)playerY);
        }

        Data.sCam.update((float)delta);
    }

    @Override
    protected void onRender() {
        SpriteRenderer.Clear(0xFF202030);  // dark blue-gray background
        /*
        SpriteRenderer.Render();
         */
        SpriteRenderer.RenderNew();
        SpriteBackend.Present(SpriteRenderer.GetFramebuffer());
        window.swapBuffers();
    }

    @Override
    protected void onShutdown() {
        SpriteBackend.Shutdown();
        SpriteRenderer.Shutdown();
        SpriteAtlasOld.Shutdown();
        SpriteSysOld.Shutdown();
        Data.sCam.shutdown();
    }

    @Override
    public boolean handleEvent(Event event) {
        if (event.getType() == EventType.KEYDOWN) {
            KeyboardEvent keyEvent = (KeyboardEvent) event;

            if (keyEvent.keyCode == Keyboard.KEY_ESCAPE) {
                stop();
                return true;
            }

            if (keyEvent.keyCode == Keyboard.KEY_LEFT)  Data.sCam.setTranslatingLeft(true);
            if (keyEvent.keyCode == Keyboard.KEY_RIGHT) Data.sCam.setTranslatingRight(true);
            if (keyEvent.keyCode == Keyboard.KEY_UP)    Data.sCam.setTranslatingUp(true);
            if (keyEvent.keyCode == Keyboard.KEY_DOWN)  Data.sCam.setTranslatingDown(true);
        }

        if (event.getType() == EventType.KEYUP) {
            KeyboardEvent keyEvent = (KeyboardEvent) event;

            if (keyEvent.keyCode == Keyboard.KEY_LEFT)  Data.sCam.setTranslatingLeft(false);
            if (keyEvent.keyCode == Keyboard.KEY_RIGHT) Data.sCam.setTranslatingRight(false);
            if (keyEvent.keyCode == Keyboard.KEY_UP)    Data.sCam.setTranslatingUp(false);
            if (keyEvent.keyCode == Keyboard.KEY_DOWN)  Data.sCam.setTranslatingDown(false);
        }

        return false;
    }

    @Override
    public EventType[] getInterestedEventTypes() {
        return new EventType[] { EventType.KEYDOWN, EventType.KEYUP };
    }
}
