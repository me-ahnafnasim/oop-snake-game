package game;

import entity.Enemy;
import entity.GameObject;
import entity.Snake;
import entity.Weapon;
import exception.GameException;
import interfaces.Collidable;
import interfaces.Movable;
import interfaces.Updatable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Core simulation engine. JavaFX is used only by render(), not by advance().
 *
 * <p>Simulation type: a fixed time-step (discrete-time) simulation. Time moves forward in equal
 * ticks, and every call to {@link #advance()} runs one tick in a fixed order. Inside a tick, every
 * interaction is recorded as an {@link Event}, and the rule engine reacts to those events. This is
 * not a discrete-event simulation, because there is no event queue that jumps time to the next
 * event. The seeded random makes every run repeatable.
 */
public class SimulationEngine {
    private final SimulationState state;
    private final RuleEngine ruleEngine = new RuleEngine();
    private final RuleRegistry<Rule> ruleRegistry = new RuleRegistry<>();
    private List<String> ruleProblems = List.of();
    private boolean weaponRequested;
    private String lastError = "";
    private String weaponNotice = "";
    private long weaponNoticeUntilTick;
    /** How many ticks a shot stays visible on screen. */
    private static final int SHOT_TICKS = 4;
    private Weapon.Type shotType;
    private double shotOriginX;
    private double shotOriginY;
    private List<double[]> shotTargets = List.of();
    private long shotUntilTick;

    public SimulationEngine(Scenario scenario) {
        state = new SimulationState(scenario);
        // Start on a grid cell near the centre so the snake lines up with every other object.
        Snake snake = new Snake(GameObject.COLUMNS / 2 * GameObject.CELL, GameObject.ROWS / 2 * GameObject.CELL,
                scenario.getInitialHealth());
        state.setSnake(snake);
        try {
            initializeEntities();
        } catch (GameException e) {
            lastError = e.getMessage();
            state.endScenario("LOSS", "Scenario setup failed: " + e.getMessage());
        }
    }

    /** Loads the scenario's rule files (every enabled file if it names none) and returns what failed to load. */
    public List<String> loadRules(Path directory) {
        ruleProblems = ruleEngine.loadDirectory(directory, state.getScenario().getRuleFiles());
        return ruleProblems;
    }

    /** Replaces the player rules with the files on disk; Java extensions registered in code are kept. */
    public List<String> reloadRules(Path directory) {
        ruleEngine.clear();
        for (Rule extension : ruleRegistry.getExtensions()) ruleEngine.register(extension);
        return loadRules(directory);
    }

    public void registerRule(Rule rule) {
        ruleRegistry.register(rule);
        ruleEngine.register(rule);
    }

    private void initializeEntities() throws GameException {
        Scenario scenario = state.getScenario();
        for (Scenario.Cell wall : scenario.getWalls()) state.addWall(wall);
        for (int i = 0; i < scenario.getFoodCount(); i++) state.spawnFood();
        List<Enemy.Kind> kinds = scenario.getEnemyKinds();
        for (int i = 0; i < scenario.getEnemyCount(); i++) state.spawn(kinds.get(i % kinds.size()));
        state.addObstacles(scenario.getObstacleCount());
        for (int i = 0; i < scenario.getWeaponCount(); i++) {
            state.spawn(i % 2 == 0 ? Weapon.Type.BLASTER : Weapon.Type.BOMB);
        }
    }

    public void advance() {
        if (state.isGameOver() || state.isWon()) return;
        try {
            state.advanceTick();
            state.addEvent(new Event(Event.Type.TICK));
            state.getSnake().advanceEffects();
            state.getSnake().move();
            if (outsideBoard() || state.getSnake().collidesWithSelf()) {
                state.endScenario("LOSS", outsideBoard() ? "You hit the wall" : "You hit yourself");
                ruleEngine.process(state);
                return;
            }

            for (GameObject entity : new ArrayList<>(state.getEntities())) {
                if (entity instanceof Updatable updatable) updatable.update(state);
            }
            fireRequestedWeapon();
            checkInteractions();
            state.updateEffects();
            state.checkObjective();
            ruleEngine.process(state);
        } catch (GameException e) {
            lastError = e.getMessage();
            state.endScenario("LOSS", "Technical error: " + e.getMessage());
        }
    }

    private boolean outsideBoard() {
        double x = state.getSnake().getX();
        double y = state.getSnake().getY();
        return x < 0 || x >= GameObject.WIDTH || y < 0 || y >= GameObject.HEIGHT;
    }

    /** Each Collidable decides what touching the snake does; the engine only detects the touch. */
    private void checkInteractions() throws GameException {
        for (GameObject entity : new ArrayList<>(state.getEntities())) {
            if (entity instanceof Collidable collidable && collidable.collidesWith(state.getSnake())) {
                collidable.onCollision(state);
            }
        }
    }

    /** Fires the last collected weapon; a shot that hits nothing gives the charge back. */
    private void fireRequestedWeapon() {
        if (!weaponRequested) return;
        weaponRequested = false;
        Weapon.Type weapon = state.getSnake().takeWeapon();
        if (weapon == null) {
            showWeaponNotice("No weapon - walk over W or B first");
            return;
        }
        List<GameObject> enemiesBefore = state.getEntities().stream()
                .filter(Enemy.class::isInstance).toList();
        int destroyed = weapon.fire(state);
        recordShot(weapon, enemiesBefore);
        if (destroyed == 0) {
            state.getSnake().returnWeapon(weapon);
            showWeaponNotice(weapon.name() + " fired - missed, charge kept");
        } else {
            state.addEvent(new Event(Event.Type.WEAPON_USED, null, weapon.name()));
            showWeaponNotice(weapon.name() + " fired - " + destroyed + " enemy destroyed!");
        }
    }

    /** Remembers where the shot came from and which enemies it removed, so render() can draw it. */
    private void recordShot(Weapon.Type weapon, List<GameObject> enemiesBefore) {
        shotType = weapon;
        shotOriginX = state.getSnake().getX();
        shotOriginY = state.getSnake().getY();
        shotTargets = enemiesBefore.stream()
                .filter(enemy -> !state.getEntities().contains(enemy))
                .map(enemy -> new double[]{enemy.getX(), enemy.getY()})
                .toList();
        shotUntilTick = state.getTick() + SHOT_TICKS;
    }

    private void renderShot(GraphicsContext gc) {
        long remaining = shotUntilTick - state.getTick();
        if (shotType == null || remaining <= 0) return;
        double half = GameObject.CELL / 2;
        double headX = shotOriginX + half;
        double headY = shotOriginY + half;
        Color color = shotType == Weapon.Type.BLASTER ? Color.DODGERBLUE : Color.ORANGERED;
        gc.save();
        gc.setGlobalAlpha(Math.min(1.0, remaining / (double) SHOT_TICKS));
        if (shotType == Weapon.Type.BOMB) {
            int radius = state.getScenario().getTuning().bombRadius();
            double size = (2 * radius + 1) * GameObject.CELL;
            double left = shotOriginX - radius * GameObject.CELL;
            double top = shotOriginY - radius * GameObject.CELL;
            gc.setFill(Color.ORANGERED.deriveColor(0, 1, 1, 0.35));
            gc.fillRect(left, top, size, size);
            gc.setStroke(Color.ORANGE);
            gc.setLineWidth(3);
            gc.strokeRect(left, top, size, size);
            gc.setFill(Color.YELLOW);
            gc.fillOval(headX - half, headY - half, GameObject.CELL, GameObject.CELL);
        } else {
            for (double[] target : shotTargets) {
                gc.setStroke(color);
                gc.setLineWidth(8);
                gc.strokeLine(headX, headY, target[0] + half, target[1] + half);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(3);
                gc.strokeLine(headX, headY, target[0] + half, target[1] + half);
            }
            gc.setStroke(color);
            gc.setLineWidth(3);
            gc.strokeOval(headX - half, headY - half, GameObject.CELL, GameObject.CELL);
        }
        for (double[] target : shotTargets) {
            gc.setFill(Color.YELLOW);
            gc.fillOval(target[0] + 4, target[1] + 4, GameObject.CELL - 8, GameObject.CELL - 8);
            gc.setStroke(color);
            gc.setLineWidth(4);
            gc.strokeOval(target[0] - 4, target[1] - 4, GameObject.CELL + 8, GameObject.CELL + 8);
        }
        gc.restore();
    }

    private void showWeaponNotice(String message) {
        weaponNotice = message;
        weaponNoticeUntilTick = state.getTick() + 12;
    }

    public SimulationState getState() { return state; }
    public Snake getSnake() { return state.getSnake(); }
    public int getScore() { return state.getScore(); }
    public int getHealth() { return state.getHealth(); }
    public int getSpeedLevel() { return state.getLevel(); }
    public boolean isGameOver() { return state.isGameOver(); }
    public boolean isWon() { return state.isWon(); }
    public String getStatusMessage() { return state.getStatusMessage(); }
    public String getLastError() { return lastError; }
    public List<GameObject> getEntities() { return state.getEntities(); }
    public List<Rule> getRules() { return ruleEngine.getRules(); }
    public List<Rule> getRegisteredExtensions() { return ruleRegistry.getExtensions(); }
    public List<String> getRuleProblems() { return ruleProblems; }

    public void setDirection(Direction direction) {
        Movable player = state.getSnake();
        player.setMovementDirection(direction);
    }

    /** Input is queued like a turn and handled at a fixed point in the next tick. */
    public void useWeapon() {
        weaponRequested = true;
    }

    public void render(GraphicsContext gc) {
        for (GameObject object : state.getEntities()) object.render(gc);
        renderShot(gc);
        Scenario scenario = state.getScenario();
        List<Weapon.Type> weapons = state.getSnake().getWeapons();
        gc.setFill(Color.WHITE);
        gc.fillText("Score: " + getScore(), 10, 40);
        gc.fillText("Health: " + getHealth(), 10, 65);
        gc.fillText("Level: " + state.getLevel() + "/" + scenario.getLevels(), 10, 90);
        gc.fillText("Goal: " + scenario.getObjective().progress(state) + "/" + state.levelGoal(state.getLevel()), 10, 115);
        gc.fillText("Weapons: " + (weapons.isEmpty() ? "none" : weapons), 10, 140);
        gc.fillText("Scenario: " + scenario.getName(), 10, 165);
        gc.fillText("Rules: " + getRules().size(), 10, 190);
        if (!ruleProblems.isEmpty()) {
            gc.setFill(Color.ORANGERED);
            gc.fillText(ruleProblems.size() + " rule file(s) not loaded: press F2", 10, 215);
        }
        if (!weaponNotice.isEmpty() && state.getTick() < weaponNoticeUntilTick) {
            gc.setFill(Color.YELLOW);
            gc.fillText(weaponNotice, 10, 240);
        }
    }
}
