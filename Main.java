import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    /** Point d'entrée : prépare la fenêtre et lance l'interface Swing. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tetris");
            MusicPlayer music = new MusicPlayer();
            SoundEffects sounds = new SoundEffects();
            GamePanel panel = new GamePanel(music, sounds);
            // Libère les ressources audio lorsque la fenêtre se ferme.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                music.stop();
                sounds.close();
            }));

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(panel);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            panel.requestFocusInWindow();
        });
    }
}
