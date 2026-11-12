package interfaces;

import exception.GameException;
import game.SimulationState;

/** A capability for objects whose behaviour is advanced by a simulation tick. */
public interface Updatable {
    void update(SimulationState state) throws GameException;
}
