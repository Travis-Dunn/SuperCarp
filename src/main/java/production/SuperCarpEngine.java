package production;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import production.save.SaveData;
import production.save.SaveManager;
import production.sprite.*;
import production.tiledmap.TileMapFileParser;
import production.tiledmap.TileMapLoader;
import whitetail.core.GameEngine;
import whitetail.event.*;
import whitetail.utility.FramerateManager;

public class SuperCarpEngine extends GameEngine implements EventListener {

    public SuperCarpEngine() { super(); }

    @Override
    protected void onProcessInput() {
        while (Keyboard.next()) {
            boolean keyDown = Keyboard.getEventKeyState();
            int keyCode = Keyboard.getEventKey();
            char keyChar = Keyboard.getEventCharacter();
            boolean repeat = Keyboard.isRepeatEvent();

            EventType eventType = keyDown ? EventType.KEYDOWN : EventType.KEYUP;
            KeyboardEvent keyEvent = new KeyboardEvent(eventType, keyCode,
                    keyChar, repeat);
            eventManager.fireEvent(keyEvent);
        }

        while (Mouse.next()) {
            boolean buttonDown = Mouse.getEventButtonState();
            int button = Mouse.getEventButton();

            if (button >= 0) {  // -1 means mouse move, no button
                int x = Mouse.getEventX();
                int y = Mouse.getEventY();
                EventType eventType = buttonDown ? EventType.MOUSE_DOWN : EventType.MOUSE_UP;
                MouseEvent mouseEvent = new MouseEvent(eventType, x, y, button);
                eventManager.fireEvent(mouseEvent);
            }
        }
    }

    @Override
    protected boolean onInit() {

        eventManager.addEventListener(this);

        SpriteSys.Init(Data.SPRITE_SYS_CAP);
        SpriteRenderer.Init(Data.FB_W, Data.FB_H);
        SpriteBackend.Init(Data.FB_W, Data.FB_H);

        Data.sCam = new SpriteCamera();
        Data.sCam.init(Data.FB_W, Data.FB_H, Data.SPRITE_SIZE);
        SpriteRenderer.SetCamera(Data.sCam);


        Data.sp = SpritePaletteFileParser.FromFile(Data.TEST_PALETTE_FILENAME);
        if (Data.sp == null) return false;
        Data.sa = SpriteAtlasFileParser.FromFile(Data.TEST_ATLAS_FILENAME,
            Data.SPRITE_SIZE, Data.sp);
        if (Data.sa == null) return false;
        Data.sa_player = SpriteAtlasFileParser.FromFile(
                Data.TEST_ATLAS_PLAYER_FILENAME, Data.SPRITE_SIZE, Data.sp);

        Data.atlasIdsByFilename.put(Data.TEST_ATLAS_FILENAME, Data.MAP_ATLAS);
        Data.paletteIdsByFilename.put(Data.TEST_PALETTE_FILENAME,
                Data.MAP_PALETTE);
        Data.atlasIdsByFilename.put(Data.TEST_ATLAS_PLAYER_FILENAME,
                Data.PLAYER_ATLAS);

        SpriteRenderer.paletteArr[Data.MAP_PALETTE] = Data.sp;
        SpriteRenderer.atlasArr[Data.MAP_ATLAS] = Data.sa;
        SpriteRenderer.atlasArr[Data.PLAYER_ATLAS] = Data.sa_player;

        Data.tileMap = TileMapFileParser.FromFile("test_map.map");
        if (Data.tileMap == null) return false;

        TileMapLoader.Load(Data.tileMap, Data.atlasIdsByFilename,
                Data.paletteIdsByFilename);

        SpriteAnimSys.Init();

        SpriteAnimDef playerIdle = new SpriteAnimDef(
                new short[] { 0, 1, 2 },  // frame sequence in atlas
                (short) 200,                  // 200ms per frame
                true                          // loops
        );

        Player.cam = Data.sCam;
        /* If you set the tile coords only, then Player.Render will animate
        movement from prevTileX to tileX, and since in this case prevTileX is
        0 (default), this makes the camera animate from focusing on tile [0, 0]
        to tileX, in this case -4. I feel like this might be handy to remember.
        */
        Player.tileX = -4;
        Player.tileY = 3;
        Player.prevTileX = -4;
        Player.prevTileY = 3;
        int playerSpriteHandle = SpriteSys.Create(
                Player.tileX * Data.SPRITE_SIZE,
                Player.tileY * Data.SPRITE_SIZE,
                Data.PLAYER_ATLAS,
                0, 0, Data.MAP_PALETTE, false, false,
                true);
        SpriteAnim playerAnimHandle = SpriteAnimSys.Create(playerSpriteHandle,
                playerIdle);

        Player.spriteHandle = playerSpriteHandle;
        Player.anim = playerAnimHandle;

        if (!SaveManager.Init()) return false;
        SaveData loaded = SaveManager.Load();
        if (loaded != null) {
            Player.tileX = loaded.playerTileX;
            Player.tileY = loaded.playerTileY;
            Player.prevTileX = loaded.playerTileX;
            Player.prevTileY = loaded.playerTileY;
            System.out.println("Loaded save: player at " + loaded.playerTileX + ", " + loaded.playerTileY);
        }

        Data.cursor = new Cursor(Data.sCam, Data.tileMap, Data.SPRITE_SIZE,
                1280, 960, Data.FB_W, Data.FB_H);
        eventManager.addEventListener(Data.cursor);

        return true;
    }

    @Override
    protected void onUpdate(double delta) {
        /*
        Data.sCam.update((float)delta);
         */

        float dt = (float)delta;

        Data.sCam.setPos((float)Player.screenX, (float)Player.screenY);
        SpriteAnimSys.Update(dt);

        while (FramerateManager.Tick()) onTick(dt);
    }

    private void onTick(float dt) {
        Player.Update(dt);
        SaveManager.RequestSave(SaveData.Capture());
    }

    @Override
    protected void onRender() {
        SpriteRenderer.Clear(Data.BLACK);
        Player.Render();
        SpriteRenderer.RenderNew();
        SpriteBackend.Present(SpriteRenderer.GetFramebuffer());
        window.swapBuffers();
    }

    @Override
    protected void onShutdown() {
        SaveManager.Shutdown();
        SpriteBackend.Shutdown();
        SpriteRenderer.Shutdown();
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

            if (keyEvent.keyCode == Keyboard.KEY_LEFT) {
                /*
                Data.sCam.setTranslatingLeft(true);
                 */
                Player.holdingLeft = true;
                Player.queueMove(-1, 0);
            }
            if (keyEvent.keyCode == Keyboard.KEY_RIGHT) {
                /*
                Data.sCam.setTranslatingRight(true);
                 */
                Player.holdingRight = true;
                Player.queueMove(1, 0);
            }
            if (keyEvent.keyCode == Keyboard.KEY_UP) {
                /*
                Data.sCam.setTranslatingUp(true);
                 */
                Player.holdingUp = true;
                Player.queueMove(0, -1);
            }
            if (keyEvent.keyCode == Keyboard.KEY_DOWN) {
                /*
                Data.sCam.setTranslatingDown(true);
                 */
                Player.holdingDown = true;
                Player.queueMove(0, 1);
            }
        }

        if (event.getType() == EventType.KEYUP) {
            KeyboardEvent keyEvent = (KeyboardEvent) event;

            if (keyEvent.keyCode == Keyboard.KEY_LEFT)
                /*
                Data.sCam.setTranslatingLeft(false);
                 */
                Player.holdingLeft = false;
            if (keyEvent.keyCode == Keyboard.KEY_RIGHT)
                /*
                Data.sCam.setTranslatingRight(false);
                 */
                Player.holdingRight = false;
            if (keyEvent.keyCode == Keyboard.KEY_UP)
                /*
                Data.sCam.setTranslatingUp(false);
                 */
                Player.holdingUp = false;
            if (keyEvent.keyCode == Keyboard.KEY_DOWN)
                /*
                Data.sCam.setTranslatingDown(false);
                 */
                Player.holdingDown = false;
        }

        return false;
    }

    @Override
    public EventType[] getInterestedEventTypes() {
        return new EventType[] { EventType.KEYDOWN, EventType.KEYUP,
                /* EventType.MOUSE_DOWN, EventType.MOUSE_UP */ };
    }
}
