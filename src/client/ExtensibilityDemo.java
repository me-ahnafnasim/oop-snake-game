package client;

import entity.Enemy;
import entity.GameObject;
import entity.Snake;
import game.MovementStrategies;
import game.MovementStrategy;
import game.RuleEngine;
import game.Scenario;
import game.ScenarioManager;
import game.SimulationState;
import java.nio.file.Path;
import java.util.List;

/**
 * Client-side demonstration of extensibility and customization.
 *
 * <p>The FLEE strategy did not exist when the engine was written: it is ONE new class in the
 * client package ({@link FleeStrategy}) registered here through
 * {@code MovementStrategies.register}. The rule files and the scenario file were written by
 * hand afterwards. No engine or entity class was modified to make any of them work (report
 * Sections "Customization" and "Extensibility").
 */
public final class ExtensibilityDemo {

    public static void main(String[] args) throws Exception {
        SimulationState state = new SimulationState(Scenario.standard());
        Snake snake = new Snake(GameObject.COLUMNS / 2 * GameObject.CELL,
                                GameObject.ROWS / 2 * GameObject.CELL, 5);
        state.setSnake(snake);

        // 1. EXTENSIBILITY: the brand-new FLEE strategy, created by the documented extension path.
        MovementStrategies.register("FLEE", FleeStrategy::new);
        System.out.println("1) New strategy FLEE registered from the client (no engine class edited):");
        // Two identical enemies at the same cell: one hunts (old behaviour), one flees (new behaviour).
        Enemy chaser = new Enemy(13 * GameObject.CELL, 7 * GameObject.CELL,
                Enemy.Kind.CHASER, MovementStrategy.named("CHASE"));
        Enemy fleer  = new Enemy(13 * GameObject.CELL, 7 * GameObject.CELL,
                Enemy.Kind.PATROLLER, MovementStrategy.named("FLEE"));
        state.addEntity(chaser);
        state.addEntity(fleer);
        System.out.println("   start      : CHASER at col " + (int) (chaser.getX() / GameObject.CELL)
                + ", FLEER at col " + (int) (fleer.getX() / GameObject.CELL)
                + "   (snake at col 10)");
        for (int i = 0; i < 3; i++) {
            state.advanceTick();
            chaser.update(state);
            fleer.update(state);
        }
        System.out.println("   after 3 ticks: CHASER at col " + (int) (chaser.getX() / GameObject.CELL)
                + " (approaching), FLEER at col " + (int) (fleer.getX() / GameObject.CELL) + " (escaping)");

        // 2. CUSTOMIZATION: player-authored .rule files interpreted at runtime.
        System.out.println();
        System.out.println("2) Player-authored rules loaded from the rules/ folder:");
        RuleEngine ruleEngine = new RuleEngine();
        List<String> problems = ruleEngine.loadDirectory(Path.of("rules"));
        System.out.println("   loaded: " + ruleEngine.getRules().size()
                + " file(s), problems: " + problems.size()
                + (problems.isEmpty() ? " (none)" : " " + problems));
        ruleEngine.getRules().forEach(r ->
                System.out.println("   - RULE " + r.name() + " (PRIORITY " + r.priority() + ")"));

        // 3. CUSTOMIZATION: player-authored scenario file loaded at runtime.
        System.out.println();
        System.out.println("3) Player-authored scenario loaded from scenarios/blitz.scenario:");
        Scenario blitz = Scenario.fromFile(Path.of("scenarios/blitz.scenario"));
        System.out.println("   name=" + blitz.getName() + ", objective=" + blitz.getObjective()
                + ", goal=" + blitz.getGoal() + ", levels=" + blitz.getLevels()
                + ", enemies=" + blitz.getEnemyCount() + ", foodPoints=" + blitz.getTuning().foodPoints());
        System.out.println("   also discoverable automatically by the F3 Scenario Manager:");
        ScenarioManager.loadDirectory(Path.of("scenarios"))
                .forEach(s -> System.out.println("   - " + s.getName() + " (" + s.getObjective() + ")"));
    }
}
