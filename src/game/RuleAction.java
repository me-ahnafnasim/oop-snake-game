package game;

import exception.InvalidActionException;

@FunctionalInterface
public interface RuleAction {
    void execute(SimulationState state) throws InvalidActionException;
}
