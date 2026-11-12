package game;

import exception.RuleExecutionException;

/** Public contract implemented by parsed player rules and Java extensions. */
public interface Rule {
    String name();
    int priority();
    boolean applies(SimulationState state, Event event);
    void execute(SimulationState state) throws RuleExecutionException;
}
