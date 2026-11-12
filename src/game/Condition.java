package game;

import exception.InvalidRuleException;

@FunctionalInterface
public interface Condition {
    boolean test(SimulationState state, Event event) throws InvalidRuleException;
}
