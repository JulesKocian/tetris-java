import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/**
 * Lecteur de musique de fond en boucle.
 * - .mp3 : décodé avec la librairie JLayer (jlayer-1.0.1.4.jar)
 * - .wav : lu avec javax.sound.sampled (aucune librairie)
 */
public class MusicPlayer {
    // État du décodeur MP3 exécuté dans un fil séparé.
    private Thread mp3Thread;
    private volatile boolean running;
    private boolean paused;
    private volatile SourceDataLine line;
    private final Object pauseLock = new Object();

    // État du lecteur WAV fourni par Java.
    private Clip clip;
    private long clipPausePosition;

    /** Lance le fichier audio en boucle, au format MP3 ou WAV. */
    public void play(String path) {
        stop();
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("Fichier audio introuvable : " + file.getAbsolutePath());
            return;
        }
        if (path.toLowerCase().endsWith(".mp3")) {
            playMp3(file);
        } else {
            playWav(file);
        }
    }

    /** Recherche les fichiers MP3 et WAV d'un dossier et les trie par nom. */
    public static List<File> findTracks(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            System.err.println("Dossier de musique introuvable : " + directory.getAbsolutePath());
            return List.of();
        }

        File[] files = directory.listFiles(file ->
                file.isFile() && (file.getName().toLowerCase(Locale.ROOT).endsWith(".mp3")
                        || file.getName().toLowerCase(Locale.ROOT).endsWith(".wav")));
        if (files == null) {
            System.err.println("Impossible de lire le dossier de musique : " + directory.getAbsolutePath());
            return List.of();
        }

        List<File> tracks = new java.util.ArrayList<>(List.of(files));
        tracks.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(tracks);
    }

    /** Met le lecteur audio en pause sans perdre sa position. */
    public void pause() {
        synchronized (pauseLock) {
            paused = true;
        }
        SourceDataLine l = line;
        if (l != null) {
            l.stop();
        }
        if (clip != null && clip.isRunning()) {
            clipPausePosition = clip.getMicrosecondPosition();
            clip.stop();
        }
    }

    /** Reprend la lecture à la position où elle avait été interrompue. */
    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        SourceDataLine l = line;
        if (l != null) {
            l.start();
        }
        if (clip != null && !clip.isRunning()) {
            clip.setMicrosecondPosition(clipPausePosition);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    /** Arrête la lecture et libère les ressources audio utilisées. */
    public void stop() {
        running = false;
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        SourceDataLine l = line;
        if (l != null) {
            l.stop();
            l.flush();
            l.close();
        }
        if (mp3Thread != null) {
            mp3Thread.interrupt();
            mp3Thread = null;
        }
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    // ------------------------------------------------------------------
    // Lecture des fichiers WAV avec les outils audio intégrés à Java.
    // ------------------------------------------------------------------

    /** Ouvre le WAV et le lit en continu. */
    private void playWav(File file) {
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(file)) {
            clip = AudioSystem.getClip();
            clip.open(audio);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Impossible de lire le WAV : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Décodage MP3 avec la bibliothèque JLayer.
    // ------------------------------------------------------------------

    private void playMp3(File file) {
        running = true;
        mp3Thread = new Thread(() -> {
            try {
                while (running) {
                    decodeOnce(file);
                    closeLine();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                if (running) {
                    System.err.println("Impossible de lire le MP3 " + file.getName() + " : " + e.getMessage());
                    running = false;
                }
            } finally {
                closeLine();
            }
        }, "music-player");
        mp3Thread.setDaemon(true); // Le lecteur ne bloque pas la fermeture du jeu.
        mp3Thread.start();
    }

    /** Décode et joue le fichier MP3 une fois, du début à la fin. */
    private void decodeOnce(File file) throws Exception {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            Bitstream bitstream = new Bitstream(in);
            Decoder decoder = new Decoder();
            try {
                Header header;
                while (running && (header = bitstream.readFrame()) != null) {
                    SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    if (line == null) {
                        openLine(decoder.getOutputFrequency(), decoder.getOutputChannels());
                    }
                    awaitResume();
                    if (!running) {
                        break;
                    }
                    byte[] bytes = toBytes(samples.getBuffer(), samples.getBufferLength());
                    line.write(bytes, 0, bytes.length);
                    bitstream.closeFrame();
                }
            } finally {
                bitstream.close();
            }
        }
    }

    /** Attend la reprise si la lecture est en pause. */
    private void awaitResume() throws InterruptedException {
        synchronized (pauseLock) {
            while (running && paused) {
                pauseLock.wait();
            }
        }
    }

    /** Ouvre la sortie audio avec la fréquence et le nombre de canaux du MP3. */
    private void openLine(int sampleRate, int channels) throws Exception {
        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        SourceDataLine l = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
        l.open(format);
        l.start();
        line = l;
    }

    /** Ferme la sortie audio si elle est ouverte. */
    private void closeLine() {
        SourceDataLine l = line;
        line = null;
        if (l != null) {
            l.close();
        }
    }

    /** Convertit les échantillons 16 bits en octets au format petit-boutiste. */
    private byte[] toBytes(short[] samples, int length) {
        byte[] bytes = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            bytes[2 * i] = (byte) (samples[i] & 0xFF);
            bytes[2 * i + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }
        return bytes;
    }
}
