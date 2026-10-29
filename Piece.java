import java.awt.Color;

public class Piece {
    /** Type d'origine de la pièce, utilisé pour retrouver sa couleur et sa forme. */
    private final PieceType type;
    private int[][] shape;
    private int x;
    private int y;

    /** Crée une pièce centrée horizontalement, juste au-dessus de la grille. */
    public Piece(PieceType type) {
        this.type = type;
        shape = type.getShape();
        x = (Board.COLS - shape[0].length) / 2;
    }

    /** Renvoie la forme actuelle, représentée par une matrice de blocs. */
    public int[][] getShape() {
        return shape;
    }

    /** Remplace la forme actuelle, notamment après une rotation. */
    public void setShape(int[][] shape) {
        this.shape = shape;
    }

    /** Calcule la forme tournée sans modifier la pièce. */
    public int[][] rotatedShape(boolean clockwise) {
        int rows = shape.length;
        int cols = shape[0].length;
        int[][] rotated = new int[cols][rows];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (clockwise) {
                    rotated[col][rows - 1 - row] = shape[row][col];
                } else {
                    rotated[cols - 1 - col][row] = shape[row][col];
                }
            }
        }
        return rotated;
    }

    /** Renvoie la couleur associée au type de pièce. */
    public Color getColor() {
        return type.getColor();
    }

    /** Renvoie le type d'origine de la pièce. */
    public PieceType getType() {
        return type;
    }

    /** Coordonnée horizontale de la pièce dans la grille. */
    public int getX() {
        return x;
    }

    /** Modifie la coordonnée horizontale de la pièce. */
    public void setX(int x) {
        this.x = x;
    }

    /** Coordonnée verticale de la pièce dans la grille. */
    public int getY() {
        return y;
    }

    /** Modifie la coordonnée verticale de la pièce. */
    public void setY(int y) {
        this.y = y;
    }

    /** Déplace la pièce du nombre de cases indiqué. */
    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }
}
