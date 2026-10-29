import java.awt.Color;

/** Les sept tétriminos, chacun défini par sa forme de départ et sa couleur. */
public enum PieceType {
    I(new int[][]{{1, 1, 1, 1}}, new Color(65, 220, 230)),
    O(new int[][]{{1, 1}, {1, 1}}, new Color(245, 215, 65)),
    T(new int[][]{{0, 1, 0}, {1, 1, 1}}, new Color(175, 90, 220)),
    S(new int[][]{{0, 1, 1}, {1, 1, 0}}, new Color(90, 205, 110)),
    Z(new int[][]{{1, 1, 0}, {0, 1, 1}}, new Color(225, 75, 75)),
    J(new int[][]{{1, 0, 0}, {1, 1, 1}}, new Color(75, 115, 225)),
    L(new int[][]{{0, 0, 1}, {1, 1, 1}}, new Color(235, 155, 65));

    /** Matrice de la forme de départ. */
    private final int[][] shape;
    /** Couleur d'affichage de la pièce. */
    private final Color color;

    PieceType(int[][] shape, Color color) {
        this.shape = shape;
        this.color = color;
    }

    /** Renvoie une copie de la forme afin de protéger la matrice de l'énumération. */
    public int[][] getShape() {
        int[][] copy = new int[shape.length][];
        for (int row = 0; row < shape.length; row++) {
            copy[row] = shape[row].clone();
        }
        return copy;
    }

    /** Renvoie la couleur de la pièce. */
    public Color getColor() {
        return color;
    }
}
