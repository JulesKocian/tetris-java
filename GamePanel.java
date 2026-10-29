import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Panneau principal : boucle de jeu (Timer), gestion du clavier et affichage.
 */
public class GamePanel extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    // Dimensions de la grille et du panneau d'informations.
    private static final int CELL = 30;          // taille d'une case en pixels
    private static final int SIDE_WIDTH = 170;   // largeur du panneau latéral
    private static final int BOARD_W = Board.COLS * CELL;
    private static final int BOARD_H = Board.ROWS * CELL;
    // Géométrie des boutons de difficulté dans le menu.
    private static final int MENU_BUTTON_X = 20;
    private static final int MENU_BUTTON_WIDTH = 260;
    private static final int MENU_BUTTON_HEIGHT = 72;
    private static final int MENU_BUTTON_GAP = 24;
    private static final int MENU_BUTTON_Y = 190;
    private static final String NO_MUSIC = "Aucune musique";
    private static final long LEVEL_UP_DISPLAY_MS = 1500;

    private enum Difficulty {
        EASY("Easy", 1, new Color(65, 165, 105)),
        MEDIUM("Medium", 4, new Color(205, 155, 55)),
        HARD("Hard", 8, new Color(190, 75, 75));

        private final String label;
        private final int startingLevel;
        private final Color color;

        Difficulty(String label, int startingLevel, Color color) {
            this.label = label;
            this.startingLevel = startingLevel;
            this.color = color;
        }
    }

    // État du jeu, affichage, musique et pièces disponibles.
    private final Board board = new Board();
    private final ScoreManager score = new ScoreManager();
    private final MusicPlayer music;
    private final SoundEffects sounds;
    private final Timer timer;
    private final List<PieceType> bag = new ArrayList<>(); // "sac" de 7 pièces pour un tirage équitable
    private final List<File> musicTracks;
    private final JComboBox<String> musicSelector;
    private final Font levelUpFont = loadLevelUpFont();

    private Piece current;
    private Piece next;
    private PieceType heldPiece;
    private boolean holdUsed;
    private boolean paused;
    private boolean gameOver;
    private boolean showingMenu = true;
    private long levelUpUntil;
    private Difficulty selectedDifficulty = Difficulty.EASY;

    /** Prépare la grille, les commandes clavier et les éléments du menu. */
    public GamePanel(MusicPlayer music, SoundEffects sounds) {
        this.music = music;
        this.sounds = sounds;
        musicTracks = MusicPlayer.findTracks("assets");

        setPreferredSize(new Dimension(BOARD_W + SIDE_WIDTH, BOARD_H));
        setBackground(new Color(18, 18, 28));
        setFocusable(true);
        setLayout(null);

        timer = new Timer(score.getDelay(), this);

        musicSelector = new JComboBox<>();
        musicSelector.addItem(NO_MUSIC);
        for (File track : musicTracks) {
            musicSelector.addItem(track.getName());
        }
        musicSelector.setBounds(BOARD_W + 10, 242, SIDE_WIDTH - 20, 30);
        musicSelector.setFont(levelUpFont.deriveFont(Font.BOLD, 12f));
        musicSelector.setVisible(true);
        add(musicSelector);

        bindKey(KeyEvent.VK_LEFT);
        bindKey(KeyEvent.VK_A);
        bindKey(KeyEvent.VK_RIGHT);
        bindKey(KeyEvent.VK_D);
        bindKey(KeyEvent.VK_DOWN);
        bindKey(KeyEvent.VK_S);
        bindKey(KeyEvent.VK_UP);
        bindKey(KeyEvent.VK_W);
        bindKey(KeyEvent.VK_X);
        bindKey(KeyEvent.VK_Z);
        bindKey(KeyEvent.VK_SPACE);
        bindKey(KeyEvent.VK_C);
        bindKey(KeyEvent.VK_P);
        bindKey(KeyEvent.VK_R);
        bindKey(KeyEvent.VK_M);
        bindKey(KeyEvent.VK_1);
        bindKey(KeyEvent.VK_2);
        bindKey(KeyEvent.VK_3);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                selectDifficultyAt(e.getX(), e.getY());
            }
        });
    }

    private void bindKey(int keyCode) {
        String actionName = "game-key-" + keyCode;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0), actionName);
        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleKey(keyCode);
            }
        });
    }

    // ------------------------------------------------------------------
    // Logique de jeu
    // ------------------------------------------------------------------

    /** Réinitialise la partie et la démarre avec la difficulté choisie. */
    private void startGame(Difficulty difficulty) {
        selectedDifficulty = difficulty;
        showingMenu = false;
        musicSelector.setVisible(false);
        board.clear();
        score.reset(difficulty.startingLevel);
        bag.clear();
        heldPiece = null;
        holdUsed = false;
        paused = false;
        gameOver = false;
        next = new Piece(nextType());
        spawnPiece();
        timer.setDelay(score.getDelay());
        timer.restart();
        playSelectedMusic();
        repaint();
    }

    /** Lance le morceau sélectionné, ou coupe la musique si aucun n'est choisi. */
    private void playSelectedMusic() {
        int selectedIndex = musicSelector.getSelectedIndex() - 1;
        if (selectedIndex >= 0 && selectedIndex < musicTracks.size()) {
            music.play(musicTracks.get(selectedIndex).getPath());
        } else {
            music.stop();
        }
    }

    /** Démarre la partie correspondant au bouton de difficulté cliqué. */
    private void selectDifficultyAt(int x, int y) {
        for (Difficulty difficulty : Difficulty.values()) {
            int buttonY = menuButtonY(difficulty);
            if (x >= MENU_BUTTON_X && x < MENU_BUTTON_X + MENU_BUTTON_WIDTH
                    && y >= buttonY && y < buttonY + MENU_BUTTON_HEIGHT) {
                startGame(difficulty);
                return;
            }
        }
    }

    /** Calcule la position verticale du bouton associé à une difficulté. */
    private int menuButtonY(Difficulty difficulty) {
        return MENU_BUTTON_Y + difficulty.ordinal() * (MENU_BUTTON_HEIGHT + MENU_BUTTON_GAP);
    }

    /** Tire la prochaine pièce dans un sac mélangé contenant les 7 types. */
    private PieceType nextType() {
        if (bag.isEmpty()) {
            Collections.addAll(bag, PieceType.values());
            Collections.shuffle(bag);
        }
        return bag.remove(bag.size() - 1);
    }

    /** Fait apparaître la pièce suivante et vérifie si la partie est terminée. */
    private void spawnPiece() {
        current = next;
        next = new Piece(nextType());
        holdUsed = false;
        if (!board.isValid(current.getShape(), current.getX(), current.getY())) {
            gameOver = true;
            timer.stop();
        }
    }

    /** Range la pièce courante ou l'échange avec celle déjà stockée. */
    private void holdCurrentPiece() {
        if (holdUsed) {
            return;
        }

        PieceType currentType = current.getType();
        if (heldPiece == null) {
            heldPiece = currentType;
            spawnPiece();
        } else {
            current = new Piece(heldPiece);
            heldPiece = currentType;
            if (!board.isValid(current.getShape(), current.getX(), current.getY())) {
                gameOver = true;
                timer.stop();
            }
        }
        holdUsed = true;
        sounds.playHold();
    }

    /** Tente de déplacer la pièce courante. Renvoie false si le mouvement est impossible. */
    private boolean tryMove(int dx, int dy) {
        if (board.isValid(current.getShape(), current.getX() + dx, current.getY() + dy)) {
            current.move(dx, dy);
            return true;
        }
        return false;
    }

    /** Essaie la rotation et de petits décalages si la pièce touche un mur. */
    private void tryRotate(boolean clockwise) {
        int[][] rotated = current.rotatedShape(clockwise);
        int[][] kicks = {{0, 0}, {-1, 0}, {1, 0}, {-2, 0}, {2, 0}, {0, -1}, {-1, -1}, {1, -1}};
        for (int[] kick : kicks) {
            int x = current.getX() + kick[0];
            int y = current.getY() + kick[1];
            if (board.isValid(rotated, x, y)) {
                current.setShape(rotated);
                current.setX(x);
                current.setY(y);
                sounds.playRotation();
                return;
            }
        }
    }

    /** Fait tomber la pièce directement, puis la fixe sur la grille. */
    private void hardDrop() {
        int distance = 0;
        while (tryMove(0, 1)) {
            distance++;
        }
        score.addHardDrop(distance);
        sounds.playHardDrop();
        lockCurrent();
    }

    /** Fixe la pièce, met à jour le score et prépare la pièce suivante. */
    private void lockCurrent() {
        board.lock(current);
        int cleared = board.clearLines();
        sounds.playLanding();
        if (cleared > 0) {
            sounds.playLineClear();
        }
        int previousLevel = score.getLevel();
        score.addLines(cleared);
        if (score.getLevel() > previousLevel) {
            levelUpUntil = System.currentTimeMillis() + LEVEL_UP_DISPLAY_MS;
        }
        timer.setDelay(score.getDelay());
        spawnPiece();
    }

    /** Gère une étape de la chute automatique déclenchée par le minuteur. */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused && !gameOver) {
            if (!tryMove(0, 1)) {
                lockCurrent();
            }
        }
        repaint();
    }

    /** Traite les touches de jeu, de pause, de reprise et de retour au menu. */
    private void handleKey(int code) {
        if (code == KeyEvent.VK_M) {
            showingMenu = true;
            paused = false;
            timer.stop();
            music.stop();
            musicSelector.setVisible(true);
            repaint();
            return;
        }
        if (showingMenu) {
            if (code == KeyEvent.VK_1) {
                startGame(Difficulty.EASY);
            } else if (code == KeyEvent.VK_2) {
                startGame(Difficulty.MEDIUM);
            } else if (code == KeyEvent.VK_3) {
                startGame(Difficulty.HARD);
            }
            return;
        }
        if (code == KeyEvent.VK_R) {
            startGame(selectedDifficulty);
            return;
        }
        if (gameOver) {
            return;
        }
        if (code == KeyEvent.VK_P) {
            paused = !paused;
            if (paused) {
                music.pause();
            } else {
                music.resume();
            }
            repaint();
            return;
        }
        if (paused) {
            return;
        }

        switch (code) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> {
                if (tryMove(-1, 0)) {
                    sounds.playMove();
                }
            }
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> {
                if (tryMove(1, 0)) {
                    sounds.playMove();
                }
            }
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                if (tryMove(0, 1)) {
                    score.addSoftDrop();
                    sounds.playSoftDrop();
                }
            }
            case KeyEvent.VK_UP, KeyEvent.VK_W, KeyEvent.VK_X -> tryRotate(true);
            case KeyEvent.VK_Z -> tryRotate(false);
            case KeyEvent.VK_SPACE -> hardDrop();
            case KeyEvent.VK_C -> holdCurrentPiece();
            default -> {
            }
        }
        repaint();
    }

    // ------------------------------------------------------------------
    // Affichage
    // ------------------------------------------------------------------

    /** Dessine le menu ou les éléments de la partie selon l'état courant. */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (showingMenu) {
            drawStartMenu(g2);
            drawMusicSelection(g2);
            g2.dispose();
            return;
        }

        drawBoard(g2);
        drawGhost(g2);
        drawCurrentPiece(g2);
        drawSidePanel(g2);
        drawLevelUp(g2);

        if (gameOver) {
            drawOverlay(g2, "GAME OVER", "R pour rejouer");
        } else if (paused) {
            drawOverlay(g2, "PAUSE", "P pour reprendre");
        }
        g2.dispose();
    }

    /** Dessine la grille, les blocs fixes et ses bordures. */
    private void drawBoard(Graphics2D g2) {
        // Fond + quadrillage
        g2.setColor(new Color(10, 10, 18));
        g2.fillRect(0, 0, BOARD_W, BOARD_H);
        g2.setColor(new Color(35, 35, 50));
        for (int c = 0; c <= Board.COLS; c++) {
            g2.drawLine(c * CELL, 0, c * CELL, BOARD_H);
        }
        for (int r = 0; r <= Board.ROWS; r++) {
            g2.drawLine(0, r * CELL, BOARD_W, r * CELL);
        }
        // Blocs posés
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Color color = board.getCell(r, c);
                if (color != null) {
                    drawBlock(g2, c * CELL, r * CELL, CELL, color);
                }
            }
        }
        // Bordure
        g2.setColor(new Color(120, 120, 150));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(0, 0, BOARD_W, BOARD_H);
    }

    /** Dessine la pièce actuellement contrôlée par le joueur. */
    private void drawCurrentPiece(Graphics2D g2) {
        if (gameOver) {
            return;
        }
        int[][] shape = current.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    drawBlock(g2, (current.getX() + c) * CELL, (current.getY() + r) * CELL, CELL, current.getColor());
                }
            }
        }
    }

    /** Silhouette translucide montrant où la pièce va atterrir. */
    private void drawGhost(Graphics2D g2) {
        if (gameOver) {
            return;
        }
        int gy = current.getY();
        while (board.isValid(current.getShape(), current.getX(), gy + 1)) {
            gy++;
        }
        Graphics2D gg = (Graphics2D) g2.create();
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        int[][] shape = current.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    drawBlock(gg, (current.getX() + c) * CELL, (gy + r) * CELL, CELL, current.getColor());
                }
            }
        }
        gg.dispose();
    }

    /** Dessine un bloc avec un relief simple pour améliorer sa lisibilité. */
    private void drawBlock(Graphics2D g2, int px, int py, int size, Color color) {
        g2.setColor(color);
        g2.fillRect(px + 1, py + 1, size - 2, size - 2);
        g2.setColor(color.brighter());
        g2.fillRect(px + 1, py + 1, size - 2, 4);
        g2.fillRect(px + 1, py + 1, 4, size - 2);
        g2.setColor(color.darker().darker());
        g2.drawRect(px + 1, py + 1, size - 3, size - 3);
    }

    /** Affiche le score, le niveau, les pièces à venir et les commandes. */
    private void drawSidePanel(Graphics2D g2) {
        int x = BOARD_W + 20;
        g2.setColor(Color.WHITE);

        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.drawString("TETRIS", x, 40);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(new Color(170, 170, 200));
        g2.drawString("MODE : " + selectedDifficulty.label, x, 70);
        g2.drawString("SCORE", x, 100);
        g2.drawString("NIVEAU", x, 150);
        g2.drawString("LIGNES", x, 200);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.drawString(String.valueOf(score.getScore()), x, 123);
        g2.drawString(String.valueOf(score.getLevel()), x, 173);
        g2.drawString(String.valueOf(score.getLines()), x, 223);

        // Aperçu de la pièce stockée
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(new Color(170, 170, 200));
        g2.drawString("RÉSERVE (C)", x, 245);
        drawPiecePreview(g2, heldPiece, x, 260);

        g2.drawString("SUIVANTE", x, 355);
        drawPiecePreview(g2, next.getType(), x, 370);

        // Aide des touches
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(150, 150, 180));
        String[] help = {
                "← → : déplacer",
                "↑ / X : rotation",
                "Z : rotation inverse",
                "↓ : descente rapide",
                "Espace : chute directe",
                "C : stocker / échanger",
                "M : menu principal",
                "P : pause",
                "R : recommencer"
        };
        int y = 430;
        for (String line : help) {
            g2.drawString(line, x, y);
            y += 19;
        }
    }

    /** Dessine l'aperçu d'une pièce dans la zone latérale. */
    private void drawPiecePreview(Graphics2D g2, PieceType type, int x, int y) {
        if (type == null) {
            return;
        }
        int preview = 24;
        int[][] shape = type.getShape();
        int offsetX = x + (4 * preview - shape[0].length * preview) / 2;
        int offsetY = y + (4 * preview - shape.length * preview) / 2;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    drawBlock(g2, offsetX + c * preview, offsetY + r * preview, preview, type.getColor());
                }
            }
        }
    }

    /** Affiche un voile de pause ou de fin de partie au-dessus de la grille. */
    private void drawOverlay(Graphics2D g2, String title, String subtitle) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, BOARD_W, BOARD_H);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 34));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (BOARD_W - fm.stringWidth(title)) / 2, BOARD_H / 2 - 10);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        fm = g2.getFontMetrics();
        g2.drawString(subtitle, (BOARD_W - fm.stringWidth(subtitle)) / 2, BOARD_H / 2 + 25);
    }

    /** Dessine le titre et les boutons de choix de difficulté. */
    private void drawStartMenu(Graphics2D g2) {
        g2.setColor(new Color(18, 18, 28));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(Color.WHITE);
        g2.setFont(levelUpFont.deriveFont(Font.BOLD, 44f));
        FontMetrics titleMetrics = g2.getFontMetrics();
        String title = "TETRIS";
        g2.drawString(title, (getWidth() - titleMetrics.stringWidth(title)) / 2, 95);

        g2.setFont(levelUpFont.deriveFont(Font.BOLD, 14f));
        String subtitle = "Choisissez votre niveau de départ";
        FontMetrics subtitleMetrics = g2.getFontMetrics();
        g2.setColor(new Color(190, 190, 215));
        g2.drawString(subtitle, (getWidth() - subtitleMetrics.stringWidth(subtitle)) / 2, 135);

        for (Difficulty difficulty : Difficulty.values()) {
            int y = menuButtonY(difficulty);
            g2.setColor(difficulty.color);
            g2.fillRoundRect(MENU_BUTTON_X, y, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(levelUpFont.deriveFont(Font.BOLD, 20f));
            g2.drawString((difficulty.ordinal() + 1) + " - " + difficulty.label,
                    MENU_BUTTON_X + 24, y + 31);
            g2.setFont(levelUpFont.deriveFont(Font.BOLD, 12f));
            g2.drawString("Commencer au niveau " + difficulty.startingLevel,
                    MENU_BUTTON_X + 24, y + 55);
        }
    }

    /** Affiche les informations associées au menu déroulant de musique. */
    private void drawMusicSelection(Graphics2D g2) {
        int x = BOARD_W + 10;
        g2.setColor(Color.WHITE);
        g2.setFont(levelUpFont.deriveFont(Font.BOLD, 16f));
        g2.drawString("MUSIQUE", x, 205);

        g2.setColor(new Color(170, 170, 200));
        g2.setFont(levelUpFont.deriveFont(Font.BOLD, 10f));
        g2.drawString(musicTracks.isEmpty() ? "Aucun fichier trouvé" : "Choisis un morceau",
                x, 230);
    }

    /** Affiche brièvement le message lorsqu'un nouveau niveau est atteint. */
    private void drawLevelUp(Graphics2D g2) {
        if (System.currentTimeMillis() >= levelUpUntil) {
            return;
        }

        String message = "Level Up";
        g2.setFont(levelUpFont.deriveFont(Font.BOLD, 48f));
        FontMetrics metrics = g2.getFontMetrics();
        int x = (BOARD_W - metrics.stringWidth(message)) / 2;
        int y = BOARD_H / 2;

        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRoundRect(x - 20, y - metrics.getAscent() - 10,
                metrics.stringWidth(message) + 40, metrics.getHeight() + 20, 20, 20);
        g2.setColor(new Color(255, 220, 65));
        g2.drawString(message, x, y);
    }

    /** Charge la police du jeu, avec une police système de secours si nécessaire. */
    private static Font loadLevelUpFont() {
        File fontFile = new File("megamax-jonathan-too-font/MegamaxJonathanToo-YqOq2.ttf");
        try {
            return Font.createFont(Font.TRUETYPE_FONT, fontFile);
        } catch (FontFormatException | IOException e) {
            System.err.println("Impossible de charger la police Level Up " + fontFile.getPath()
                    + " : " + e.getMessage());
            return new Font("SansSerif", Font.BOLD, 48);
        }
    }
}