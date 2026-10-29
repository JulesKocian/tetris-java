import java.awt.Color;
import java.util.Arrays;

public class Board {
    /** Nombre de colonnes de la grille. */
    public static final int COLS = 10;
    /** Nombre de lignes de la grille. */
    public static final int ROWS = 20;

    /** Chaque case contient la couleur de son bloc, ou null si elle est vide. */
    private final Color[][] cells = new Color[ROWS][COLS];

    /** Vide toutes les cases de la grille. */
    public void clear() {
        for (Color[] row : cells) {
            Arrays.fill(row, null);
        }
    }

    /** Renvoie la couleur de la case indiquée. */
    public Color getCell(int row, int col) {
        return cells[row][col];
    }

    /**
     * Vérifie qu'une forme peut occuper la position demandée.
     * Les blocs au-dessus de la grille sont autorisés pendant l'apparition d'une pièce.
     */
    public boolean isValid(int[][] shape, int x, int y) {
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 0) {
                    continue;
                }
                int boardX = x + col;
                int boardY = y + row;
                if (boardX < 0 || boardX >= COLS || boardY >= ROWS) {
                    return false;
                }
                if (boardY >= 0 && cells[boardY][boardX] != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Fixe les blocs de la pièce dans la grille. */
    public void lock(Piece piece) {
        int[][] shape = piece.getShape();
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 0) {
                    continue;
                }
                int boardX = piece.getX() + col;
                int boardY = piece.getY() + row;
                if (boardY >= 0 && boardY < ROWS && boardX >= 0 && boardX < COLS) {
                    cells[boardY][boardX] = piece.getColor();
                }
            }
        }
    }

    /**
     * Supprime les lignes complètes, fait descendre celles qui les surplombent
     * et renvoie le nombre de lignes effacées.
     */
    public int clearLines() {
        int cleared = 0;
        for (int row = ROWS - 1; row >= 0; row--) {
            boolean full = true;
            for (int col = 0; col < COLS; col++) {
                if (cells[row][col] == null) {
                    full = false;
                    break;
                }
            }
            if (full) {
                for (int shiftRow = row; shiftRow > 0; shiftRow--) {
                    System.arraycopy(cells[shiftRow - 1], 0, cells[shiftRow], 0, COLS);
                }
                Arrays.fill(cells[0], null);
                cleared++;
                row++;
            }
        }
        return cleared;
    }
}
