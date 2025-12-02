package whitetail.audio;

public class AudioBuffer {
    public final String filename;
    public final int id;
    public final float volume;
    public final boolean loop;

    AudioBuffer(String filename, int id, float volume, boolean loop) {
        this.filename = filename;
        this.id = id;
        this.volume = volume;
        this.loop = loop;
    }

    public static final String CLASS = AudioBuffer.class.getSimpleName();
}
