package whitetail.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.vector.Vector3f;
import whitetail.utility.logging.LogLevel;

import java.util.*;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public class AudioContext {
    private static boolean init;
    private static Map<String, AudioBuffer> buffers;
    private static Set<Audio> active;
    private static float globalVolume;
    private static Vector3f pos;

    private AudioContext() {}

    public static boolean Init() {
        assert(!init);

        try {
            /* TODO: More explicit error handling */
            AL.create();
            buffers = new HashMap<String, AudioBuffer>();
            active = new HashSet<Audio>();
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

    public static Audio Make(float volume, String filename, AudioCategory cat) {
        assert(init);

        Audio audio;

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

        audio = new Audio(id, volume, buffer.loop, cat);
        active.add(audio);

        return audio;
    }

    public static void Update() {
        assert (init);

        Iterator<Audio> it = active.iterator();
        while (it.hasNext()) {
            Audio audio = it.next();

            /* Don't auto-destroy looping sounds */
            if (audio.loops) continue;

            int state = AL10.alGetSourcei(audio.id, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_STOPPED) {
                AL10.alDeleteSources(audio.id);
                int error = AL10.alGetError();
                if (error != AL10.AL_NO_ERROR) {
                    LogSession(LogLevel.WARNING, CLASS
                            + " error auto-destroying source [" + audio.id
                            + "]: 0x" + Integer.toHexString(error));
                }
                it.remove();
            }
        }
    }

    public static void StopByCategory(AudioCategory category) {
        assert(init);

        for (Audio audio : active) {
            if (audio.cat == category) {
                AL10.alSourceStop(audio.id);
            }
        }
        LogSession(LogLevel.DEBUG, CLASS + " stopped all " + category + " audio");
    }

    public static void DestroyByCategory(AudioCategory category) {
        assert(init);

        Iterator<Audio> it = active.iterator();
        while (it.hasNext()) {
            Audio audio = it.next();
            if (audio.cat == category) {
                AL10.alSourceStop(audio.id);
                /* TODO: put the error handling in here! */
                AL10.alDeleteSources(audio.id);
                it.remove();
            }
        }
        LogSession(LogLevel.DEBUG, CLASS + " destroyed all " + category + " audio");
    }

    public static void PauseByCategory(AudioCategory category) {
        assert(init);

        for (Audio audio : active) {
            if (audio.cat == category) {
                AL10.alSourcePause(audio.id);
            }
        }
    }

    public static void ResumeByCategory(AudioCategory category) {
        assert(init);

        for (Audio audio : active) {
            if (audio.cat == category) {
                int state = AL10.alGetSourcei(audio.id, AL10.AL_SOURCE_STATE);
                if (state == AL10.AL_PAUSED) {
                    AL10.alSourcePlay(audio.id);
                }
            }
        }
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

    public static void Destroy(Audio audio) {
        assert(init);

        if (!active.contains(audio)) {
            LogFatalAndExit(ErrStrDestroyUntracked(audio.id));
            return;
        }

        AL10.alSourceStop(audio.id);
        int error = AL10.alGetError();
        if (error == AL10.AL_INVALID_NAME) {
            LogFatalAndExit(ErrStrSourceVanished(audio.id));
            return;
        } else if (error != AL10.AL_NO_ERROR) {
            LogFatalAndExit(ErrStrUnexpectedALError("alSourceStop", audio.id, error));
            return;
        }

        AL10.alDeleteSources(audio.id);
        error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            LogFatalAndExit(ErrStrUnexpectedALError("alDeleteSources", audio.id, error));
            return;
        }

        active.remove(audio);
        LogSession(LogLevel.DEBUG, CLASS + " destroyed Audio [" + audio.id + "]");
    }

    public static void Shutdown() {
        assert(init);

        LogSession(LogLevel.DEBUG, CLASS + " beginning shutdown...\n");

        int err = AL10.alGetError();
        if (err != AL10.AL_NO_ERROR) {
            LogSession(LogLevel.WARNING, CLASS +
                    " pre-existing AL error on shutdown: 0x"
                    + Integer.toHexString(err) + "\n");
        }

        LogSession(LogLevel.DEBUG, CLASS + " stopping and deleting ["
                + active.size() + "] sources...\n");

        for (Audio audio : active) {
            AL10.alSourceStop(audio.id);
            err= AL10.alGetError();
            if (err== AL10.AL_INVALID_NAME) {
                LogSession(LogLevel.WARNING, CLASS + " source ["
                        + audio.id + "] already gone during shutdown");
            } else if (err!= AL10.AL_NO_ERROR) {
                LogSession(LogLevel.WARNING, CLASS +
                        " unexpected error stopping source [" + audio.id
                        + "]: 0x" + Integer.toHexString(err));
            }

            AL10.alDeleteSources(audio.id);
            err= AL10.alGetError();
            if (err!= AL10.AL_NO_ERROR) {
                LogSession(LogLevel.WARNING, CLASS +
                        " error deleting source [" + audio.id + "]: 0x"
                        + Integer.toHexString(err));
            }
        }

        active.clear();
        active = null;
        LogSession(LogLevel.DEBUG, CLASS + " all sources cleaned up.\n");

        /* Delete buffers */
        LogSession(LogLevel.DEBUG, CLASS + " deleting [" + buffers.size()
                + "] buffers...\n");
        for (AudioBuffer buffer : buffers.values()) {
            AL10.alDeleteBuffers(buffer.id);
            err = AL10.alGetError();
            if (err != AL10.AL_NO_ERROR) {
                LogSession(LogLevel.WARNING, CLASS +
                        " failed to delete buffer [" + buffer.filename + "]: 0x"
                        + Integer.toHexString(err) + "\n");
            }
        }
        buffers.clear();
        buffers = null;
        LogSession(LogLevel.DEBUG, CLASS + " all buffers cleaned up.\n");

        /* Destroy the context */
        LogSession(LogLevel.DEBUG, CLASS + " destroying AL context...\n");
        try {
            AL.destroy();
            LogSession(LogLevel.DEBUG, CLASS + " AL context destroyed.\n");
        } catch (Exception e) {
            LogSession(LogLevel.WARNING, CLASS +
                    " exception during AL.destroy(): " + e.getMessage() + "\n");
        }

        pos = null;
        init = false;

        LogSession(LogLevel.DEBUG, CLASS + " shutdown complete.\n");
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
    private static String ErrStrDestroyUntracked(int id) {
        return String.format("%s attempted to destroy an Audio [%d] that is " +
                "not tracked. This indicates a double-destroy or use-after-free"
                + " bug.\n", CLASS, id);
    }
    private static String ErrStrSourceVanished(int id) {
        return String.format("%s found that OpenAL source [%d] no longer " +
                "exists. This indicates the audio backend died or a severe "
                + "internal error.\n", CLASS, id);
    }
    private static String ErrStrUnexpectedALError(String func, int id,
            int error) {
        return String.format("%s encountered unexpected OpenAL error [0x%X] " +
                "during %s for source [%d].\n", CLASS, error, func, id);
    }
}
