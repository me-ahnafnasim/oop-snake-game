package game;

import exception.RuleExecutionException;
import java.util.List;

/** Rule created from one external .rule file. */
public final class PlayerRule implements Rule {
    private final String name;
    private final int priority;
    private final Condition condition;
    private final List<RuleAction> actions;

    public PlayerRule(String name, int priority, Condition condition, List<RuleAction> actions) {
        this.name = name;
        this.priority = priority;
        this.condition = condition;
        this.actions = List.copyOf(actions);
    }

    @Override public String name() { return name; }
    @Override public int priority() { return priority; }

    @Override
    public boolean applies(SimulationState state, Event event) {
        try {
            return condition.test(state, event);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void execute(SimulationState state) throws RuleExecutionException {
        try {
            for (RuleAction action : actions) action.execute(state);
        } catch (Exception e) {
            throw new RuleExecutionException("Rule '" + name + "' failed: " + e.getMessage(), e);
        }
    }
}
