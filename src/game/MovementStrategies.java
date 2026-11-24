package game;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry of named movement strategies. New algorithms are added from outside the engine
 * through {@link #register}, so the engine stays closed for modification.
 */
public final class MovementStrategies {
    private static final Map<String, Supplier<MovementStrategy>> FACTORIES = new LinkedHashMap<>();

    static {
        FACTORIES.put("CHASE", ChaseStrategy::new);
        FACTORIES.put("PATROL", PatrolStrategy::new);
        FACTORIES.put("WANDER", WanderStrategy::new);
    }

    private MovementStrategies() {
    }

    /** Adds a new strategy name usable by scenario files, rule files and spawned enemies. */
    public static void register(String name, Supplier<MovementStrategy> factory) {
        if (name == null || name.isBlank() || factory == null) {
            throw new IllegalArgumentException("A strategy needs a name and a factory");
        }
        String key = name.toUpperCase(Locale.ROOT);
        if (FACTORIES.containsKey(key)) {
            throw new IllegalArgumentException("Movement strategy " + key + " is already registered");
        }
        FACTORIES.put(key, factory);
    }

    /** Creates a fresh strategy for the given name. */
    public static MovementStrategy named(String name) {
        Supplier<MovementStrategy> factory = FACTORIES.get(name.toUpperCase(Locale.ROOT));
        if (factory == null) {
            throw new IllegalArgumentException(
                    "Unknown movement strategy " + name + " (use " + String.join(", ", FACTORIES.keySet()) + ")");
        }
        return factory.get();
    }
}
