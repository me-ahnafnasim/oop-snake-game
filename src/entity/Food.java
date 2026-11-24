package entity;

import exception.GameException;
import game.Event;
import game.SimulationState;
import interfaces.Collidable;
import interfaces.Collectible;
import interfaces.EnemyContact;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Food extends GameObject implements Collidable, Collectible, EnemyContact {
    private final double radius = CELL / 2 - 4;
    private boolean collected = false;

    public Food(double x, double y) {
        super(x, y);
    }

    @Override
    public boolean isCollected() {
        return collected;
    }

    @Override
    public void collect() {
        collected = true;
    }

    @Override
    public void render(GraphicsContext gc) {
        if (collected) return;

        double cx = getX() + CELL / 2;
        double cy = getY() + CELL / 2;

        gc.setFill(Color.RED);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        gc.setFill(Color.GREEN);
        gc.fillRoundRect(cx - 3, cy - radius - 6, 6, 8, 4, 4);
        drawLabel(gc, "FOOD");
    }

    @Override
    public boolean collidesWith(Snake snake) {
        return !collected && sameCell(snake.getX(), snake.getY());
    }

    @Override
    public void onCollision(SimulationState state) throws GameException {
        collect();
        state.growSnake();
        state.addScore(state.getScenario().getTuning().foodPoints());
        state.addEvent(new Event(Event.Type.FOOD_COLLECTED, this, "Food collected"));
        state.removeEntity(this);
        state.spawnFood();
    }

    /** A food-eating enemy (a chaser) takes the food, and a new one appears elsewhere. */
    @Override
    public void onEnemyEnter(Enemy enemy, SimulationState state) throws GameException {
        if (collected || !enemy.getKind().eatsFood()) return;
        collect();
        state.removeEntity(this);
        state.addEvent(new Event(Event.Type.FOOD_STOLEN, this, enemy.getKind().name()));
        state.spawnFood();
    }
}
