package client;

import entity.Enemy;
import entity.Snake;
import game.Direction;
import game.MovementStrategy;
import game.SimulationState;

/** An extension written outside the engine: runs away from the snake, the opposite of CHASE. */
public final class FleeStrategy implements MovementStrategy {
    @Override
    public Direction chooseDirection(Enemy enemy, Snake snake, SimulationState state) {
        double dx = enemy.getX() - snake.getX();
        double dy = enemy.getY() - snake.getY();
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx >= 0 ? Direction.RIGHT : Direction.LEFT;
        }
        return dy >= 0 ? Direction.DOWN : Direction.UP;
    }
}
