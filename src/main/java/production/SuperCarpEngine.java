package production;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import production.character.CharRegistry;
import production.dialogue.DialogueManager;
import production.dialogue.warehouse.BilboDialogue;
import production.monster.MonsterRegistry;
import production.monster.MonsterSpawn;
import production.save.SaveData;
import production.save.SaveManager;
import production.scene.SceneGame;
import production.sprite.*;
import production.tilemap.TileMapFileParser;
import production.tilemap.TileMapLoader;
import production.ui.*;
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

        if (!SpriteSys.Init(Data.SPRITE_SYS_CAP, DisplayConfig.GetEmulatedW(),
                DisplayConfig.GetEmulatedH(), Data.BPP)) {
            LogFatalAndExit(ERR_STR_FAILED_INIT_SPRITE_SYS);
            return false;
        }

        Data.sCam = new SpriteCamera();
        Data.sCam.init(DisplayConfig.GetViewportW(),
                DisplayConfig.GetViewportH(), Data.SPRITE_SIZE);
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

        SpriteAnimSys.Init();

        Data.tileMap = TileMapFileParser.FromFile("test_map.map");
        if (Data.tileMap == null) return false;

        TileMapLoader.Load(Data.tileMap, Data.atlasIdsByFilename,
                Data.paletteIdsByFilename,Data.TEST_PALETTE_FILENAME);

        Data.clearColor = Data.tileMap.clearColor;


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
        Player.tileX = 0;
        Player.tileY = -1;
        Player.prevTileX = 0;
        Player.prevTileY = -1;
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
                DisplayConfig.GetWindowW(), DisplayConfig.GetWindowH(),
                DisplayConfig.GetEmulatedW(), DisplayConfig.GetEmulatedH());

        /* audio */
        Data.testMusicBuf = AudioFileParser.FromFile(
                "03 - Definitely Our Town.wav", 0.5f, true);
        /* TODO: add descriptive log messages for all of these! */
        if (Data.testMusicBuf == null) return false;
        AudioContext.RegisterBuffer(Data.testMusicBuf);
        Data.testMusic = AudioContext.Make(0.5f,
                "03 - Definitely Our Town.wav", AudioCategory.MUSIC);
        if (Data.testMusic == null) return false;

        AudioContext.SetVolume(Data.testMusic, CFGData.fGlobalVolume / 100.0f);

        AudioContext.Play(Data.testMusic);

        Data.sceneGame = new SceneGame();
        SceneManager.Init();
        SceneManager.RegisterScene(SceneType.GAME, Data.sceneGame);
        SceneManager.TransitionTo(SceneType.GAME);

        Data.testSpawn = new MonsterSpawn(3, -5, MonsterRegistry.BAT, 16);

        /* load time */
        /* the sprite handle is of type int, and is only created when the
        * map in which the character resides is loaded, I.E., when the player
        * enters that area. I distinguish between load time (player enters area)
        * and init time. Init time only ever happens once per session. */

        Data.fontAtlas = FontAtlasFileParser.FromFile(
                "16_rs_mono_freetype.fnt", Data.sp, 11);

        ChatBox.Init(Data.fontAtlas);
        ChatBox.AddMsg("Welcome to SuperCarp.");

        DialogueManager.Init(Data.fontAtlas, Data.sp);
        CharRegistry.BILBO.dialogueRoot = BilboDialogue.GREETING;

        Renderer.Init(Data.BPP, SpriteSys.GetFramebuffer(),
                SpriteSys.GetFramebufferWidth(),
                SpriteSys.GetFramebufferHeight());
        BitmapRegistry.Init(Data.sp);

        GameFrame.Init();

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

                /* Lord forgive me for this nonsense */
                Data.screenMouseX = x;
                Data.screenMouseY = y;

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
        /*
        Data.testSpawn.update();
        spawns are now managed by the map
         */

        Data.tileMap.update();
        Player.Update(dt);
        SaveManager.RequestSave(SaveData.Capture());
    }

    @Override
    protected void onRender() {
        SceneManager.Render();
        SpriteRenderer.Clear(Data.BLACK);
        Player.Render();
        SpriteRenderer.ClearViewport(Data.clearColor);
        SpriteRenderer.RenderNew();

        if (DialogueManager.isActive()) {
            // need mouse position in FB coords for hover effects

            int fbX = (Data.screenMouseX * DisplayConfig.GetEmulatedW())
                    / DisplayConfig.GetWindowW();
            int fbY = ((DisplayConfig.GetWindowH() - Data.screenMouseY)
                    * DisplayConfig.GetEmulatedH()) / DisplayConfig.GetWindowH();
            DialogueManager.draw(SpriteSys.GetFramebuffer(),
                    DisplayConfig.GetEmulatedW(), DisplayConfig.GetEmulatedH(),
                    fbX, fbY);
        } else {
            ChatBox.Draw();
        }

        GameFrame.Draw();

        SpriteBackend.Present(SpriteSys.GetFramebuffer());
        window.swapBuffers();
    }

    @Override
    protected void onShutdown() {
        SceneManager.Shutdown();
        SaveManager.Shutdown();
        SpriteSys.Shutdown();
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