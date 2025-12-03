package production;

import org.lwjgl.util.vector.Vector3f;
import whitetail.core.GameEngine;
import whitetail.event.Event;
import whitetail.event.EventListener;
import whitetail.event.EventType;
import whitetail.graphics.RenderContext;
import whitetail.graphics.Shader;
import whitetail.graphics.Sprite;
import whitetail.graphics.SpriteRenderer;
import whitetail.graphics.cameras.Camera;
import whitetail.graphics.materials.MaterialD;
import whitetail.loaders.TextureFileParser;

public class SuperCarpEngine extends GameEngine implements EventListener {

    public SuperCarpEngine() { super(); }

    @Override
    protected void onProcessInput() {

    }

    @Override
    protected boolean onInit() {
        RenderContext.ActivateSpriteRenderer();

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
        SpriteRenderer.Add(Data.charSprite);


        Data.cam = Camera.MakeMenu(800.0f, 600.0f, 0.1f, 10f);

        SpriteRenderer.SetCamera(Data.cam);

        return true;
    }

    @Override
    protected void onUpdate(double delta) {

    }

    @Override
    protected void onRender() {
        window.swapBuffers();
    }

    @Override
    protected void onShutdown() {

    }

    @Override
    public boolean handleEvent(Event event) {
        return false;
    }

    @Override
    public EventType[] getInterestedEventTypes() {
        return new EventType[0];
    }
}
