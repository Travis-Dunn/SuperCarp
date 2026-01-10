package whitetail.audio;

public class Audio {
    public final int id;
    float volume;
    public final boolean loops;
    public final AudioCategory cat;

    Audio(int id, float volume, boolean loops, AudioCategory cat) {
        this.id = id;
        this.volume = volume;
        this.loops = loops;
        this.cat = cat;
    }

    public float getVolume() { return volume; }

    public static final String CLASS = Audio.class.getSimpleName();
}
