package game;

import entity.Enemy;
import entity.Snake;

public final class ChaseStrategy implements MovementStrategy {
    @Override
    public Direction chooseDirection(Enemy enemy, Snake snake, SimulationState state) {
        double dx = snake.getX() - enemy.getX();
        double dy = snake.getY() - enemy.getY();
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx >= 0 ? Direction.RIGHT : Direction.LEFT;
        }
        return dy >= 0 ? Direction.DOWN : Direction.UP;
    }
}
