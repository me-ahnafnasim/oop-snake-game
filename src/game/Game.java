package game;

import java.nio.file.Path;

/** Compatibility facade used by the JavaFX controller. */
public final class Game extends SimulationEngine {
    public Game() {
        this(Scenario.standard());
    }

    /** Problems with rule files are kept in getRuleProblems() and shown on screen, never dropped. */
    public Game(Scenario scenario) {
        super(scenario);
        loadRules(Path.of("rules"));
    }

    public void update() {
        advance();
    }
}
