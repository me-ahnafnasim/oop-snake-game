package entity;

import game.Event;
import game.SimulationState;
import interfaces.Collidable;
import interfaces.Collectible;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.Comparator;
import java.util.List;

/** A collectible weapon charge. Each type fires differently. */
public final class Weapon extends GameObject implements Collidable, Collectible {

    public enum Type {
        /** Removes the single enemy closest to the snake's head. */
        BLASTER(Color.DODGERBLUE, "W") {
            @Override
            public int fire(SimulationState state) {
                Snake snake = state.getSnake();
                return enemies(state).stream()
                        .min(Comparator.comparingInt(snake::cellDistance))
                        .map(enemy -> destroy(state, List.of(enemy)))
                        .orElse(0);
            }
        },
        /** Removes every enemy within the scenario's bomb radius (default two cells) of the snake's head. */
        BOMB(Color.ORANGERED, "B") {
            @Override
            public int fire(SimulationState state) {
                Snake snake = state.getSnake();
                int radius = state.getScenario().getTuning().bombRadius();
                return destroy(state, enemies(state).stream()
                        .filter(enemy -> snake.cellDistance(enemy) <= radius)
                        .toList());
            }
        };

        private static final int SCORE_PER_ENEMY = 30;

        private final Color color;
        private final String symbol;

        Type(Color color, String symbol) {
            this.color = color;
            this.symbol = symbol;
        }

        /** Fires the weapon and returns how many enemies it destroyed. */
        public abstract int fire(SimulationState state);

        private static List<Enemy> enemies(SimulationState state) {
            return state.getEntities().stream()
                    .filter(Enemy.class::isInstance)
                    .map(Enemy.class::cast)
                    .toList();
        }

        private static int destroy(SimulationState state, List<Enemy> targets) {
            for (Enemy enemy : targets) {
                state.removeEntity(enemy);
                state.addScore(SCORE_PER_ENEMY);
            }
            return targets.size();
        }
    }

    private final Type type;
    private boolean collected;

    public Weapon(double x, double y, Type type) {
        super(x, y);
        this.type = type;
    }

    public Type getType() { return type; }
    @Override public boolean isCollected() { return collected; }
    @Override public void collect() { collected = true; }

    @Override
    public void render(GraphicsContext gc) {
        if (collected) return;
        gc.setFill(Color.SILVER);
        gc.fillRect(getX() + 8, getY() + 14, CELL - 16, 12);
        gc.setFill(type.color);
        gc.fillOval(getX() + 15, getY() + 10, 10, 20);
        drawLabel(gc, type.symbol + " " + type.name());
    }

    @Override
    public boolean collidesWith(Snake snake) {
        return !collected && sameCell(snake.getX(), snake.getY());
    }

    @Override
    public void onCollision(SimulationState state) {
        collect();
        state.getSnake().addWeapon(type);
        state.addScore(15);
        state.addEvent(new Event(Event.Type.WEAPON_COLLECTED, this, type.name()));
        state.removeEntity(this);
    }
}
