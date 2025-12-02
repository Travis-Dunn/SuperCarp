package whitetail.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;
import static whitetail.utility.logging.Logger.LogFatal;

public class AudioContext {
    private static boolean init;
    private static Map<String, AudioBuffer> buffers;
    private static Map<String, Integer> sources;
    private static float globalVolume;
    private static Vector3f pos;

    private AudioContext() {}

    public static boolean Init() {
        assert(!init);

        try {
            AL.create();
            buffers = new HashMap<String, AudioBuffer>();
            sources = new HashMap<String, Integer>();
            globalVolume = 1.0f;
            pos = new Vector3f(0.0f, 0.0f, 0.0f);

            return init = true;
        } catch (Exception e) {
            LogFatalExcpAndExit(ERR_STR_FAILED_INIT_EXCEPTION, e);
            return init = false;
        }
    }

    public static void SetGlobalVolume(float volume) { globalVolume = volume; }

    public static void Update(Vector3f pos) {
        assert(init);

        /* check probably not worth it, but I don't know how expensive the lib
           call is */
        if (pos.equals(AudioContext.pos)) return;
        AL10.alListener3f(AL10.AL_POSITION, pos.x, pos.y, pos.z);
    }

    public static Audio Make(float volume, String filename) {
        assert(init);

        AudioBuffer buffer = buffers.get(filename);
        if (buffer == null) {
            LogFatalAndExit(ErrStrAudioBufferDoesNotExist(filename));
            return null;
        }
        int id = AL10.alGenSources();
        AL10.alSourcei(id, AL10.AL_BUFFER, buffer.id);
        AL10.alSourcef(id, AL10.AL_GAIN, volume * buffer.volume *
                globalVolume);
        AL10.alSourcei(id, AL10.AL_LOOPING,
                buffer.loop ? AL10.AL_TRUE : AL10.AL_FALSE);
        sources.put(filename, id);

        return new Audio(id, volume);
    }

    public static void RegisterBuffer(AudioBuffer audioBuffer) {
        assert(init);

        if (buffers.containsKey(audioBuffer.filename)) {
            LogFatalAndExit(ErrStrDuplicateBuffer(audioBuffer.filename));
            return;
        }

        buffers.put(audioBuffer.filename, audioBuffer);
    }

    public static void Play(Audio audio) {
        assert(init);

        AL10.alSourcePlay(audio.id);
    }

    public static void Stop(Audio audio) {
        assert(init);

        AL10.alSourceStop(audio.id);
    }

    public static void SetVolume(Audio audio, float volume) {
        assert(init);

        AL10.alSourcef(audio.id, AL10.AL_GAIN, volume);
        audio.volume = volume;
    }

    public static void Shutdown() {
        assert(init);

        for (int source : sources.values()) {
            AL10.alDeleteSources(source);
        }
        for (AudioBuffer buffer : buffers.values()) {
            AL10.alDeleteBuffers(buffer.id);
        }
        AL.destroy();
        sources.clear();
        sources = null;
        buffers.clear();
        buffers = null;
        pos = null;

        init = false;
    }

    public static final String CLASS = AudioContext.class.getSimpleName();
    private static final String ERR_STR_FAILED_INIT_EXCEPTION = String.format(
            "The [%s] failed to initialize because an exception was" +
            " encountered.\n", CLASS);
    private static String ErrStrDuplicateBuffer(String filename) {
        return String.format("The [%s] attempted to create a duplicate [%s]" +
                " for [%s], which is illegal.\n", CLASS, AudioBuffer.CLASS,
                filename);
    }
    private static String ErrStrAudioBufferDoesNotExist(String filename) {
        return String.format("The [%s] attempted to create an [%s] from [%s]," +
                " but no [%s] was ever created for that file.\n", CLASS,
                Audio.CLASS, filename, AudioBuffer.CLASS);
    }
}
