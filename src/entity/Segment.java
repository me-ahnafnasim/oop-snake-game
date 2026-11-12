package entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Segment extends GameObject {
    private Color color;

    public Segment(double x, double y, Color color) {
        super(x, y);
        this.color = color;
    }

    public Segment(double x, double y) {
        this(x, y, Color.LIMEGREEN);
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(color);
        gc.fillRoundRect(getX() + 2, getY() + 2, CELL - 4, CELL - 4, 12, 12);
    }
}
