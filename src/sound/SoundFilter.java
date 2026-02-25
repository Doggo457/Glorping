package sound;

import javax.sound.sampled.*;
import java.io.*;

/**
 * A novel sound filter that applies a cave echo/reverb effect to WAV files.
 * This is achieved by mixing the original audio signal with one or more
 * delayed, attenuated copies of itself, simulating reflections off cave walls.
 *
 * The filter runs in its own thread (like the library's Sound class) so it
 * does not block the main game loop.
 */
public class SoundFilter extends Thread {

    /** Path to the WAV file to filter */
    private final String filename;
    /** Delay between echo repetitions in seconds */
    private final float delaySeconds;
    /** How much to attenuate each successive echo (0.0 - 1.0) */
    private final float decay;
    /** Number of echo repetitions */
    private final int numEchoes;

    /**
     * Creates a new SoundFilter for the given file with echo parameters.
     *
     * @param filename     Path to the WAV file
     * @param delaySeconds Delay between echoes in seconds (e.g. 0.15 for cave)
     * @param decay        Volume multiplier for each echo (e.g. 0.45)
     * @param numEchoes    Number of echo repetitions (e.g. 3)
     */
    public SoundFilter(String filename, float delaySeconds, float decay, int numEchoes) {
        this.filename = filename;
        this.delaySeconds = delaySeconds;
        this.decay = decay;
        this.numEchoes = numEchoes;
    }

    /**
     * Reads the WAV data, applies the echo algorithm, and plays back
     * the processed audio through a SourceDataLine.
     * Called automatically by Thread.start().
     */
    @Override
    public void run() {
        try {
            File file = new File(filename);
            if (!file.exists()) return;

            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            AudioFormat format = ais.getFormat();

            // Only process 16-bit signed PCM – fallback to direct play otherwise
            if (format.getSampleSizeInBits() != 16 ||
                format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                playDirect(filename);
                return;
            }

            // Read all raw audio bytes
            byte[] raw = ais.readAllBytes();
            ais.close();

            // Calculate delay in samples (each sample = 2 bytes for 16-bit mono/stereo)
            int channels   = format.getChannels();
            int frameSize  = format.getFrameSize(); // bytes per frame
            int delaySamples = (int)(delaySeconds * format.getSampleRate());
            int delayBytes   = delaySamples * frameSize;

            // Allocate output buffer large enough for original + all echoes
            int totalBytes = raw.length + delayBytes * numEchoes;
            byte[] output = new byte[totalBytes];
            System.arraycopy(raw, 0, output, 0, raw.length);

            // Apply successive echo passes
            float amplitude = decay;
            for (int echo = 1; echo <= numEchoes; echo++) {
                int offset = delayBytes * echo;
                for (int i = 0; i < raw.length; i += 2) {
                    int idx = i + offset;
                    if (idx + 1 >= totalBytes) break;

                    // Read 16-bit sample (little-endian)
                    short sample = (short)(((raw[i + 1] & 0xFF) << 8) | (raw[i] & 0xFF));
                    // Read existing output sample at this position
                    short existing = (short)(((output[idx + 1] & 0xFF) << 8) | (output[idx] & 0xFF));

                    // Mix and clamp to avoid overflow
                    int mixed = (int)(existing + sample * amplitude);
                    mixed = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixed));

                    // Write back little-endian
                    output[idx]     = (byte)(mixed & 0xFF);
                    output[idx + 1] = (byte)((mixed >> 8) & 0xFF);
                }
                amplitude *= decay;
            }

            // Extend AudioFormat for the full output length
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, 4096);
            line.start();
            line.write(output, 0, totalBytes);
            line.drain();
            line.close();

        } catch (Exception e) {
            // Fallback: play the original file without filtering
            playDirect(filename);
        }
    }

    /**
     * Falls back to direct playback using the library's Sound class approach.
     *
     * @param fname File path to play
     */
    private void playDirect(String fname) {
        try {
            File file = new File(fname);
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = stream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            clip.start();
            Thread.sleep(100);
            while (clip.isRunning()) Thread.sleep(100);
            clip.close();
        } catch (Exception ignored) {}
    }
}
