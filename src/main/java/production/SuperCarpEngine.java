package production;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import production.monster.MonsterDefs;
import production.monster.MonsterSpawn;
import production.save.SaveData;
import production.save.SaveManager;
import production.scenes.SceneGame;
import production.sprite.*;
import production.tilemap.TileMapFileParser;
import production.tilemap.TileMapLoader;
import whitetail.audio.AudioCategory;
import whitetail.audio.AudioContext;
import whitetail.audio.AudioFileParser;
import whitetail.core.GameEngine;
import whitetail.event.*;
import whitetail.scene.SceneManager;
import whitetail.scene.SceneType;
import whitetail.utility.FramerateManager;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public class SuperCarpEngine extends GameEngine implements EventListener {

    public SuperCarpEngine() { super(); }

    @Override
    protected boolean onInit() {
        eventManager.addEventListener(this);

        if (!SpriteSys.Init(Data.SPRITE_SYS_CAP, Data.FB_W, Data.FB_H)) {
            LogFatalAndExit(ERR_STR_FAILED_INIT_SPRITE_SYS);
            return false;
        }

        Data.sCam = new SpriteCamera();
        Data.sCam.init(Data.FB_W, Data.FB_H, Data.SPRITE_SIZE);
        SpriteRenderer.SetCamera(Data.sCam);

        Data.sp = SpritePaletteFileParser.FromFile(Data.TEST_PALETTE_FILENAME);
        if (Data.sp == null) return false;
        Data.sa = SpriteAtlasFileParser.FromFile(Data.TEST_ATLAS_FILENAME,
            Data.SPRITE_SIZE, Data.sp);
        if (Data.sa == null) return false;
        Data.sa_player = SpriteAtlasFileParser.FromFile(
                Data.TEST_ATLAS_ANIM_FILENAME, Data.SPRITE_SIZE, Data.sp);

        Data.atlasIdsByFilename.put(Data.TEST_ATLAS_FILENAME, Data.MAP_ATLAS);
        Data.paletteIdsByFilename.put(Data.TEST_PALETTE_FILENAME,
                Data.MAP_PALETTE);
        Data.atlasIdsByFilename.put(Data.TEST_ATLAS_ANIM_FILENAME,
                Data.PLAYER_ATLAS);

        SpriteRenderer.paletteArr[Data.MAP_PALETTE] = Data.sp;
        SpriteRenderer.atlasArr[Data.MAP_ATLAS] = Data.sa;
        SpriteRenderer.atlasArr[Data.PLAYER_ATLAS] = Data.sa_player;

        Data.tileMap = TileMapFileParser.FromFile("test_map.map");
        if (Data.tileMap == null) return false;

        TileMapLoader.Load(Data.tileMap, Data.atlasIdsByFilename,
                Data.paletteIdsByFilename,Data.TEST_PALETTE_FILENAME);

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
        Player.tileX = -3;
        Player.tileY = 1;
        Player.prevTileX = -3;
        Player.prevTileY = 1;
        int playerSpriteHandle = SpritePool.Create(
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
                Data.WINDOW_W, Data.WINDOW_H, Data.FB_W, Data.FB_H);

        /* audio */
        Data.testMusicBuf = AudioFileParser.FromFile(
                "03 - Definitely Our Town.wav", 0.5f, true);
        /* TODO: add descriptive log messages for all of these! */
        if (Data.testMusicBuf == null) return false;
        AudioContext.RegisterBuffer(Data.testMusicBuf);
        Data.testMusic = AudioContext.Make(0.5f,
                "03 - Definitely Our Town.wav", AudioCategory.MUSIC);
        if (Data.testMusic == null) return false;

        AudioContext.SetVolume(Data.testMusic, CFGData.globalVolume / 100.0f);

        AudioContext.Play(Data.testMusic);

        Data.sceneGame = new SceneGame();
        SceneManager.Init();
        SceneManager.RegisterScene(SceneType.GAME, Data.sceneGame);
        SceneManager.TransitionTo(SceneType.GAME);

        Data.testSpawn = new MonsterSpawn(3, -5, MonsterDefs.BAT, 16);

        return true;
    }

    @Override
    protected void onProcessInput() {
        SceneManager.OnInput();
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
    protected void onUpdate(double delta) {
        float dt = (float)delta;
        SceneManager.Update(dt);
        /*
        Data.sCam.update((float)delta);
         */


        Data.sCam.setPos((float)Player.screenX, (float)Player.screenY);
        SpriteAnimSys.Update(dt);

        while (FramerateManager.Tick()) onTick(dt);
    }

    private void onTick(float dt) {
        Data.testSpawn.update();
        Player.Update(dt);
        SaveManager.RequestSave(SaveData.Capture());
    }

    @Override
    protected void onRender() {
        SceneManager.Render();
        SpriteRenderer.Clear(Data.BLACK);
        Player.Render();
        SpriteRenderer.RenderNew();
        SpriteBackend.Present(SpriteRenderer.GetFramebuffer());
        window.swapBuffers();
    }

    @Override
    protected void onShutdown() {
        SceneManager.Shutdown();
        SaveManager.Shutdown();
        SpriteBackend.Shutdown();
        SpriteRenderer.Shutdown();
        Data.sCam.shutdown();
    }

    @Override
    public boolean handleEvent(Event event) {
        KeyboardEvent keyEvent;

        if (SceneManager.HandleEvent(event)) {
            return true;
        } else {
            if (event.getType() == EventType.KEYDOWN) {
                keyEvent = (KeyboardEvent) event;

                if (keyEvent.keyCode == Keyboard.KEY_ESCAPE) {
                    stop();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    /* all events! */
    public EventType[] getInterestedEventTypes() {
        return new EventType[] { EventType.KEYDOWN, EventType.KEYUP,
                EventType.MOUSE_DOWN, EventType.MOUSE_UP };
    }

    public static final String CLASS = SuperCarpEngine.class.getSimpleName();
    private static final String ERR_STR_FAILED_INIT_SPRITE_SYS = " failed to " +
            "initialize because the sprite system failed to initialize.\n";
}