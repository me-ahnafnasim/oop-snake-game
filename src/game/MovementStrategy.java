package game;

import entity.Enemy;
import entity.Snake;

/** Selects an enemy direction without coupling the enemy to one algorithm. */
public interface MovementStrategy {
    /** Returns the preferred direction, or null to keep the current one. */
    Direction chooseDirection(Enemy enemy, Snake snake, SimulationState state);

    /** Creates a strategy from the name used in scenario and rule files. */
    static MovementStrategy named(String name) {
        return MovementStrategies.named(name);
    }
}
