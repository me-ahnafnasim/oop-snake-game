package entity;

import exception.GameException;
import game.Direction;
import game.Event;
import game.MovementStrategy;
import game.SimulationState;
import interfaces.Collidable;
import interfaces.EnemyContact;
import interfaces.Movable;
import interfaces.Updatable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public final class Enemy extends GameObject implements Collidable, Movable, Updatable, EnemyContact {

    /** Each kind has its own colour, default movement, appetite and speed. */
    public enum Kind {
        /** Hunts the snake at half speed, so the player can escape, and eats food on its way. */
        CHASER(Color.PURPLE, "CHASE", true, 2),
        PATROLLER(Color.ORANGE, "PATROL", false, 1),
        WANDERER(Color.TEAL, "WANDER", false, 1);

        private final Color color;
        private final String defaultStrategy;
        private final boolean eatsFood;
        private final int ticksPerStep;

        Kind(Color color, String defaultStrategy, boolean eatsFood, int ticksPerStep) {
            this.color = color;
            this.defaultStrategy = defaultStrategy;
            this.eatsFood = eatsFood;
            this.ticksPerStep = ticksPerStep;
        }

        public String defaultStrategy() { return defaultStrategy; }
        public boolean eatsFood() { return eatsFood; }
    }

    private final Kind kind;
    private MovementStrategy strategy;
    private Direction direction = Direction.LEFT;

    public Enemy(double x, double y, Kind kind, MovementStrategy strategy) {
        super(x, y);
        this.kind = kind;
        this.strategy = strategy;
    }

    public Kind getKind() { return kind; }
    public MovementStrategy getStrategy() { return strategy; }

    /** Lets rules swap the movement algorithm while the game runs. */
    public void setStrategy(MovementStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void setMovementDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public Direction getMovementDirection() { return direction; }

    /**
     * Asks the strategy for a direction and steps that way. If that cell is blocked
     * (board edge, obstacle, another enemy), it tries the other directions in a fixed order.
     * Objects in the new cell then react through EnemyContact.
     */
    @Override
    public void update(SimulationState state) throws GameException {
        if (state.getTick() % kind.ticksPerStep != 0) return;
        Direction preferred = strategy.chooseDirection(this, state.getSnake(), state);
        if (preferred != null) setMovementDirection(preferred);
        for (Direction candidate : candidates(direction)) {
            double nextX = getX() + candidate.getDx() * CELL;
            double nextY = getY() + candidate.getDy() * CELL;
            if (canEnter(nextX, nextY, state)) {
                setMovementDirection(candidate);
                setPosition(nextX, nextY);
                for (GameObject other : state.objectsAt(nextX, nextY)) {
                    if (other != this && other instanceof EnemyContact contact) contact.onEnemyEnter(this, state);
                }
                return;
            }
        }
    }

    private static List<Direction> candidates(Direction first) {
        List<Direction> order = new ArrayList<>(List.of(first));
        for (Direction d : Direction.values()) {
            if (d != first) order.add(d);
        }
        return order;
    }

    private boolean canEnter(double x, double y, SimulationState state) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return false;
        for (GameObject other : state.objectsAt(x, y)) {
            if (other instanceof EnemyContact contact && contact.blocksEnemy(this)) return false;
        }
        return true;
    }

    /** Two enemies never share a cell. */
    @Override
    public boolean blocksEnemy(Enemy enemy) {
        return enemy != this;
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(kind.color);
        gc.fillOval(getX() + 4, getY() + 4, CELL - 8, CELL - 8);
        gc.setFill(Color.WHITE);
        gc.fillOval(getX() + 12, getY() + 12, 6, 6);
        gc.fillOval(getX() + 24, getY() + 12, 6, 6);
    }

    @Override
    public boolean collidesWith(Snake snake) {
        return sameCell(snake.getX(), snake.getY());
    }

    @Override
    public void onCollision(SimulationState state) {
        if (!state.getSnake().consumeShield()) state.changeHealth(-1);
        state.addEvent(new Event(Event.Type.ENEMY_COLLISION, this, kind.name()));
    }
}
