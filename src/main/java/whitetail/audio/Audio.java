package whitetail.audio;

public class Audio {
    public final int id;
    float volume;

    Audio(int id, float volume) {
        this.id = id;
        this.volume = volume;
    }

    public float getVolume() { return volume; }

    public static final String CLASS = Audio.class.getSimpleName();
}
