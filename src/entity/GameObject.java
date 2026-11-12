package entity;

import javafx.scene.canvas.GraphicsContext;

/** Anything that occupies one board cell and can draw itself. */
public abstract class GameObject {

    public static final double CELL = 40;
    public static final double WIDTH = 800;
    public static final double HEIGHT = 600;
    public static final int COLUMNS = (int) (WIDTH / CELL);
    public static final int ROWS = (int) (HEIGHT / CELL);

    private double x;
    private double y;

    public GameObject(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    protected void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    protected boolean sameCell(double otherX, double otherY) {
        return getX() == otherX && getY() == otherY;
    }

    /** Distance in whole cells, counting diagonal steps as one (Chebyshev distance). */
    public int cellDistance(GameObject other) {
        int dx = (int) Math.abs(getX() - other.getX()) / (int) CELL;
        int dy = (int) Math.abs(getY() - other.getY()) / (int) CELL;
        return Math.max(dx, dy);
    }

    public abstract void render(GraphicsContext gc);
}
