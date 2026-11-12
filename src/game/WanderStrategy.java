package game;

import entity.Enemy;
import entity.Snake;

/** Picks a random direction every few ticks. It uses the scenario's seeded random, so runs stay repeatable. */
public final class WanderStrategy implements MovementStrategy {
    private static final int TICKS_PER_TURN = 3;

    @Override
    public Direction chooseDirection(Enemy enemy, Snake snake, SimulationState state) {
        if (state.getTick() % TICKS_PER_TURN != 0) return null;
        Direction[] all = Direction.values();
        return all[state.getRandom().nextInt(all.length)];
    }
}
