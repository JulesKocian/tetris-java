import java.util.EnumMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioSystem;

/** Produit les bruitages courts des actions de jeu sans fichier audio externe. */
public class SoundEffects implements AutoCloseable {
    private static final float SAMPLE_RATE = 22050;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private enum Effect {
        MOVE(420, 300, 45, 0.22),
        SOFT_DROP(300, 190, 55, 0.2),
        ROTATE(520, 880, 90, 0.24),
        HOLD(660, 440, 100, 0.25),
        DROP(180, 70, 150, 0.32),
        LAND(240, 130, 75, 0.25),
        LINE_CLEAR(620, 1180, 220, 0.28);

        private final double startFrequency;
        private final double endFrequency;
        private final int durationMs;
        private final double volume;

        Effect(double startFrequency, double endFrequency, int durationMs, double volume) {
            this.startFrequency = startFrequency;
            this.endFrequency = endFrequency;
            this.durationMs = durationMs;
            this.volume = volume;
        }
    }

    private final EnumMap<Effect, Clip> clips = new EnumMap<>(Effect.class);

    public SoundEffects() {
        for (Effect effect : Effect.values()) {
            try {
                Clip clip = AudioSystem.getClip();
                byte[] samples = createSamples(effect);
                clip.open(FORMAT, samples, 0, samples.length);
                clips.put(effect, clip);
            } catch (LineUnavailableException | IllegalArgumentException e) {
                System.err.println("Impossible de préparer le bruitage " + effect + " : " + e.getMessage());
            }
        }
    }

    public void playMove() {
        play(Effect.MOVE);
    }

    public void playSoftDrop() {
        play(Effect.SOFT_DROP);
    }

    public void playRotation() {
        play(Effect.ROTATE);
    }

    public void playHold() {
        play(Effect.HOLD);
    }

    public void playHardDrop() {
        play(Effect.DROP);
    }

    public void playLanding() {
        play(Effect.LAND);
    }

    public void playLineClear() {
        play(Effect.LINE_CLEAR);
    }

    private void play(Effect effect) {
        Clip clip = clips.get(effect);
        if (clip == null) {
            return;
        }
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    private static int sampleCount(Effect effect) {
        return Math.round(SAMPLE_RATE * effect.durationMs / 1000);
    }

    private static byte[] createSamples(Effect effect) {
        int samples = sampleCount(effect);
        byte[] bytes = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double progress = (double) i / samples;
            double frequency = effect.startFrequency
                    + (effect.endFrequency - effect.startFrequency) * progress;
            double envelope = Math.min(1.0, i / (SAMPLE_RATE * 0.008))
                    * Math.pow(1.0 - progress, 1.4);
            short sample = (short) (Math.sin(2.0 * Math.PI * frequency * i / SAMPLE_RATE)
                    * envelope * effect.volume * Short.MAX_VALUE);
            bytes[i * 2] = (byte) (sample & 0xFF);
            bytes[i * 2 + 1] = (byte) ((sample >>> 8) & 0xFF);
        }
        return bytes;
    }

    @Override
    public void close() {
        for (Clip clip : clips.values()) {
            clip.stop();
            clip.close();
        }
        clips.clear();
    }
}
