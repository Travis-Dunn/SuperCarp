package production;

import org.lwjgl.input.Keyboard;
import production.sprite.*;
import production.tiledmap.TileMapFileParser;
import production.tiledmap.TileMapLoader;
import whitetail.core.GameEngine;
import whitetail.event.Event;
import whitetail.event.EventListener;
import whitetail.event.EventType;
import whitetail.event.KeyboardEvent;

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
    }

    @Override
    protected boolean onInit() {

        eventManager.addEventListener(this);

        SpriteSys.Init(Data.SPRITE_SYS_CAP);
        SpriteRenderer.Init(Data.FB_W, Data.FB_H);
        SpriteBackend.Init(Data.FB_W, Data.FB_H);

        Data.sCam = new SpriteCamera();
        Data.sCam.init(Data.FB_W, Data.FB_H);
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

        int playerSpriteHandle = SpriteSys.Create(0, 0, Data.PLAYER_ATLAS,
                0, 0, Data.MAP_PALETTE, false, false,
                true);
        SpriteAnim playerAnimHandle = SpriteAnimSys.Create(playerSpriteHandle,
                playerIdle);

        return true;
    }

    @Override
    protected void onUpdate(double delta) {
        Data.sCam.update((float)delta);
        SpriteAnimSys.Update((float)delta);
    }

    @Override
    protected void onRender() {
        SpriteRenderer.Clear(Data.BLACK);
        SpriteRenderer.RenderNew();
        SpriteBackend.Present(SpriteRenderer.GetFramebuffer());
        window.swapBuffers();
    }

    @Override
    protected void onShutdown() {
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

            if (keyEvent.keyCode == Keyboard.KEY_LEFT)
                Data.sCam.setTranslatingLeft(true);
            if (keyEvent.keyCode == Keyboard.KEY_RIGHT)
                Data.sCam.setTranslatingRight(true);
            if (keyEvent.keyCode == Keyboard.KEY_UP)
                Data.sCam.setTranslatingUp(true);
            if (keyEvent.keyCode == Keyboard.KEY_DOWN)
                Data.sCam.setTranslatingDown(true);
        }

        if (event.getType() == EventType.KEYUP) {
            KeyboardEvent keyEvent = (KeyboardEvent) event;

            if (keyEvent.keyCode == Keyboard.KEY_LEFT)
                Data.sCam.setTranslatingLeft(false);
            if (keyEvent.keyCode == Keyboard.KEY_RIGHT)
                Data.sCam.setTranslatingRight(false);
            if (keyEvent.keyCode == Keyboard.KEY_UP)
                Data.sCam.setTranslatingUp(false);
            if (keyEvent.keyCode == Keyboard.KEY_DOWN)
                Data.sCam.setTranslatingDown(false);
        }

        return false;
    }

    @Override
    public EventType[] getInterestedEventTypes() {
        return new EventType[] { EventType.KEYDOWN, EventType.KEYUP };
    }
}
