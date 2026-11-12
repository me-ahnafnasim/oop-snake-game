package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registry for Java Rule extensions supplied without changing SimulationEngine. */
public final class RuleRegistry<T extends Rule> {
    private final List<T> extensions = new ArrayList<>();

    public void register(T rule) {
        if (rule == null) {
            throw new IllegalArgumentException("A rule extension cannot be null");
        }
        extensions.add(rule);
    }

    public List<T> getExtensions() {
        return Collections.unmodifiableList(extensions);
    }
}
