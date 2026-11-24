package entity;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** Anything that occupies one board cell and can draw itself. */
public abstract class GameObject {

    public static final double CELL = 40;
    public static final double WIDTH = 800;
    public static final double HEIGHT = 600;
    public static final int COLUMNS = (int) (WIDTH / CELL);
    public static final int ROWS = (int) (HEIGHT / CELL);
    private static final Font LABEL_FONT = Font.font("Arial", FontWeight.BOLD, 8);

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

    /** Writes a small name along the bottom of this object's cell so players can tell objects apart. */
    protected void drawLabel(GraphicsContext gc, String text) {
        gc.save();
        gc.setFill(Color.rgb(0, 0, 0, 0.65));
        gc.fillRect(getX() + 1, getY() + CELL - 11, CELL - 2, 10);
        gc.setFill(Color.WHITE);
        gc.setFont(LABEL_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        // The max width squeezes long names so they never spill into the next cell.
        gc.fillText(text, getX() + CELL / 2, getY() + CELL - 6, CELL - 4);
        gc.restore();
    }
}
