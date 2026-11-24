package entity;

import game.Event;
import game.SimulationState;
import interfaces.Collidable;
import interfaces.Collectible;
import interfaces.EffectSource;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Bonus extends GameObject implements Collidable, Collectible, EffectSource {
    public enum Type { SHIELD, POISON, SPEED }

    private final Type type;
    private boolean collected;
    private int effectTicks;

    public Bonus(double x, double y, Type type) {
        super(x, y);
        this.type = type;
    }

    public Type getType() { return type; }
    public int getEffectTicks() { return effectTicks; }

    @Override public boolean isCollected() { return collected; }

    @Override
    public void collect() {
        collected = true;
    }

    @Override
    public void applyEffect(Snake snake) {
        switch (type) {
            case SHIELD -> snake.addShieldTicks(effectTicks);
            case POISON -> snake.changeHealth(-1);
            case SPEED -> snake.addSpeedBoostTicks(effectTicks);
        }
    }

    @Override
    public void updateEffect(Snake snake) {
        if (effectTicks > 0) effectTicks--;
        if (effectTicks == 0 && type == Type.SPEED) snake.setSpeedBoostTicks(0);
    }

    @Override public boolean isExpired() { return collected && effectTicks == 0; }
    @Override public String effectName() { return type.name(); }

    @Override
    public void render(GraphicsContext gc) {
        if (collected) return;
        Color color = switch (type) {
            case SHIELD -> Color.DODGERBLUE;
            case POISON -> Color.DARKMAGENTA;
            case SPEED -> Color.YELLOW;
        };
        gc.setFill(color);
        gc.fillOval(getX() + 5, getY() + 5, CELL - 10, CELL - 10);
        gc.setFill(type == Type.POISON ? Color.WHITE : Color.BLACK);
        gc.fillText(type.name().substring(0, 1), getX() + 16, getY() + 25);
        drawLabel(gc, type.name());
    }

    @Override
    public boolean collidesWith(Snake snake) {
        return !collected && sameCell(snake.getX(), snake.getY());
    }

    /** Leaves the board but stays active in the state until its effect expires. */
    @Override
    public void onCollision(SimulationState state) {
        collect();
        effectTicks = state.getScenario().getTuning().bonusTicks();
        state.addScore(25);
        applyEffect(state.getSnake());
        state.checkHealth();
        state.addEvent(new Event(Event.Type.BONUS_COLLECTED, this, type.name()));
        state.removeEntity(this);
        state.addActiveEffect(this);
    }
}
