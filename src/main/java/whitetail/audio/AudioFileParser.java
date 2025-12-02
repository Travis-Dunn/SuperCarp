package whitetail.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;

public class AudioFileParser {
    private static final String AUDIO_DIR = "audio";

    /* may return null! */
    public static AudioBuffer FromFile(String filename, float volume,
            boolean loop) {
        String path = "/" + AUDIO_DIR + "/" + filename;
        InputStream stream = AudioFileParser.class.getResourceAsStream(path);
        if (stream == null) {
            LogFatalAndExit(ErrStrFailedParseFile(filename));
            return null;
        }
        return FromStream(stream, filename, volume, loop);
    }

    private static AudioBuffer FromStream(InputStream stream, String filename,
            float volume, boolean loop) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(stream));
            AudioFormat format = ais.getFormat();

            byte data[] = new byte[ais.available()];
            ais.read(data);
            ais.close();

            ByteBuffer b = BufferUtils.createByteBuffer(data.length);
            b.put(data);
            b.flip();

            int alFormat;
            if (format.getChannels() == 1) {
                alFormat = format.getSampleSizeInBits() == 16 ?
                        AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_MONO8;
                /* Mono and stereo only - no surround sound. */
            } else {
                alFormat = format.getSampleSizeInBits() == 16 ?
                        AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_STEREO8;
            }
            int id = AL10.alGenBuffers();
            AL10.alBufferData(id, alFormat, b,
                    (int)format.getSampleRate());

            return new AudioBuffer(filename, id, volume, loop);
        } catch (Exception e) {
            LogFatalExcpAndExit(ErrStrFailedParseFileExcp(filename), e);
            return null;
        }
   }

    public static final String CLASS = AudioFileParser.class.getSimpleName();
    private static String ErrStrFailedParseFile(String filename) {
        return String.format("The [%s] failed to parse [%s].\n", CLASS,
                filename);
    }
    private static String ErrStrFailedParseFileExcp(String filename) {
        return String.format("The [%s] failed to parse [%s] because an" +
                " exception was encountered.\n", CLASS, filename);
    }
}
