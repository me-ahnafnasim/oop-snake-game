package interfaces;

import entity.Enemy;
import exception.GameException;
import game.SimulationState;

/** How an object reacts when an enemy moves into its cell. Both methods default to "no effect". */
public interface EnemyContact {
    /** True when the enemy may not step into this object's cell. */
    default boolean blocksEnemy(Enemy enemy) {
        return false;
    }

    /** Called after an enemy has stepped into this object's cell. */
    default void onEnemyEnter(Enemy enemy, SimulationState state) throws GameException {
    }
}
