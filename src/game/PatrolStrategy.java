package game;

import entity.Enemy;
import entity.Snake;

public final class PatrolStrategy implements MovementStrategy {
    @Override
    public Direction chooseDirection(Enemy enemy, Snake snake, SimulationState state) {
        Direction[] directions = {Direction.LEFT, Direction.UP, Direction.RIGHT, Direction.DOWN};
        return directions[(int) ((state.getTick() / 4) % directions.length)];
    }
}
