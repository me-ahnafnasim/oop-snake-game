package entity;

import game.Event;
import game.SimulationState;
import interfaces.Collidable;
import interfaces.EnemyContact;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class Obstacle extends GameObject implements Collidable, EnemyContact {
    private final boolean hazardous;

    public Obstacle(double x, double y, boolean hazardous) {
        super(x, y);
        this.hazardous = hazardous;
    }

    public boolean isHazardous() { return hazardous; }

    /** Walls and hazards stop enemies as well as the snake. */
    @Override
    public boolean blocksEnemy(Enemy enemy) {
        return true;
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(hazardous ? Color.DARKRED : Color.DARKGRAY);
        gc.fillRect(getX() + 2, getY() + 2, CELL - 4, CELL - 4);
        if (hazardous) {
            gc.setFill(Color.ORANGE);
            gc.fillText("!", getX() + 15, getY() + 25);
        }
    }

    @Override
    public boolean collidesWith(Snake snake) {
        return sameCell(snake.getX(), snake.getY());
    }

    @Override
    public void onCollision(SimulationState state) {
        state.addEvent(new Event(Event.Type.OBSTACLE_COLLISION, this, hazardous ? "hazard" : "solid"));
        if (hazardous) state.changeHealth(-1);
        else state.endScenario("LOSS", "You hit an obstacle");
    }
}
