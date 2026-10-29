/** Gère le score, les lignes effacées, le niveau et la vitesse de chute. */
public class ScoreManager {
    private int score;
    private int lines;
    private int level;
    private int startingLevel;

    /** Initialise le gestionnaire avec les valeurs par défaut. */
    public ScoreManager() {
        reset();
    }

    /** Réinitialise la partie au niveau 1. */
    public void reset() {
        reset(1);
    }

    /** Réinitialise le score et les lignes en conservant le niveau de départ choisi. */
    public void reset(int startingLevel) {
        if (startingLevel < 0) {
            throw new IllegalArgumentException("Starting level must not be negative");
        }
        score = 0;
        lines = 0;
        this.startingLevel = startingLevel;
        level = startingLevel;
    }

    /** Ajoute les points des lignes effacées et met à jour le niveau courant. */
    public void addLines(int cleared) {
        if (cleared <= 0) {
            return;
        }
        int[] points = {0, 100, 300, 500, 800};
        int basePoints = cleared < points.length ? points[cleared] : points[4] + (cleared - 4) * 300;
        score += basePoints * Math.max(1, level);
        lines += cleared;
        level = startingLevel + lines / 5;
    }

    /** Ajoute un point pour chaque case parcourue en descente rapide. */
    public void addSoftDrop() {
        score++;
    }

    /** Ajoute deux points par case parcourue lors d'une chute directe. */
    public void addHardDrop(int distance) {
        score += Math.max(0, distance) * 2;
    }

    /** Renvoie le score courant. */
    public int getScore() {
        return score;
    }

    /** Renvoie le nombre total de lignes effacées. */
    public int getLines() {
        return lines;
    }

    /** Renvoie le niveau courant. */
    public int getLevel() {
        return level;
    }

    /** Calcule l'intervalle de chute en millisecondes selon le niveau. */
    public int getDelay() {
        return Math.max(100, 600 - Math.max(0, level - 1) * 50);
    }
}
