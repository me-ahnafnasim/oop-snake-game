package game;

import entity.Bonus;
import entity.Enemy;
import entity.Food;
import entity.GameObject;
import entity.Obstacle;
import entity.Snake;
import entity.Weapon;
import exception.BoardFullException;
import exception.InvalidActionException;
import exception.InvalidScenarioException;
import interfaces.EffectSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Mutable domain state; GUI code only observes it through public methods. */
public final class SimulationState {
    private final Scenario scenario;
    private final Random random;
    private final List<GameObject> entities = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final List<EffectSource> activeEffects = new ArrayList<>();
    private final Map<String, Double> variables = new HashMap<>();
    private final Map<Enemy.Kind, String> strategyOverrides = new EnumMap<>(Enemy.Kind.class);
    private Snake snake;
    private long tick;
    private int score;
    private int level = 1;
    private boolean gameOver;
    private boolean won;
    private String statusMessage = "Running";

    public SimulationState(Scenario scenario) {
        this.scenario = scenario;
        this.random = new Random(scenario.getSeed());
    }

    public Scenario getScenario() { return scenario; }
    public Random getRandom() { return random; }
    public long getTick() { return tick; }
    public int getScore() { return score; }
    public boolean isGameOver() { return gameOver; }
    public boolean isWon() { return won; }
    public String getStatusMessage() { return statusMessage; }
    public Snake getSnake() { return snake; }
    public int getLevel() { return level; }
    public List<GameObject> getEntities() { return Collections.unmodifiableList(entities); }
    public List<Event> getEvents() { return Collections.unmodifiableList(events); }
    public List<EffectSource> getActiveEffects() { return Collections.unmodifiableList(activeEffects); }

    /** The snake owns its health; before the snake exists, the scenario's starting value applies. */
    public int getHealth() {
        return snake == null ? scenario.getInitialHealth() : snake.getHealth();
    }

    public void setSnake(Snake snake) {
        this.snake = snake;
        addEntity(snake);
    }

    public void addEntity(GameObject object) {
        if (object != null && !entities.contains(object)) entities.add(object);
    }

    public void removeEntity(GameObject object) {
        entities.remove(object);
    }

    /** Everything currently in the given cell, including the snake's head. */
    public List<GameObject> objectsAt(double x, double y) {
        return entities.stream().filter(entity -> entity.getX() == x && entity.getY() == y).toList();
    }

    public void advanceTick() {
        tick++;
        events.clear();
    }

    public void addEvent(Event event) {
        events.add(event);
    }

    public void addScore(int amount) {
        score = Math.max(0, score + amount);
    }

    public void changeHealth(int amount) {
        if (snake == null) return;
        snake.changeHealth(amount);
        checkHealth();
    }

    /** Ends the run when health has reached zero, whatever reduced it. */
    public void checkHealth() {
        if (getHealth() == 0) endScenario("LOSS", "Health reached zero");
    }

    /** The first outcome is final; later calls in the same run are ignored. */
    public void endScenario(String outcome, String message) {
        if (gameOver || won) return;
        if ("WIN".equalsIgnoreCase(outcome)) {
            won = true;
        } else {
            gameOver = true;
        }
        statusMessage = message;
    }

    /** Completes every level whose share of the goal has been reached; finishing the last level wins. */
    public void checkObjective() throws BoardFullException {
        long progress = scenario.getObjective().progress(this);
        while (!gameOver && !won && progress >= levelGoal(level)) {
            addEvent(new Event(Event.Type.LEVEL_COMPLETED, null, "level " + level));
            if (level == scenario.getLevels()) {
                endScenario("WIN", "Scenario objective completed");
            } else {
                level++;
                startLevel();
            }
        }
    }

    /** Progress needed to finish the given level: an equal share of the goal per level, rounded up. */
    public long levelGoal(int level) {
        int levels = scenario.getLevels();
        return ((long) scenario.getGoal() * level + levels - 1) / levels;
    }

    /** Each new level makes the board harder: one more enemy and one more hazard. */
    private void startLevel() throws BoardFullException {
        List<Enemy.Kind> kinds = scenario.getEnemyKinds();
        spawn(kinds.get(level % kinds.size()));
        double[] position = freePosition();
        addEntity(new Obstacle(position[0], position[1], true));
    }

    public Object valueOf(String name) {
        return switch (name) {
            case "score" -> score;
            case "health" -> getHealth();
            case "level" -> level;
            case "tick" -> tick;
            case "snake.length" -> snake == null ? 0 : snake.getLength();
            case "snake.shield" -> snake == null ? 0 : snake.getShieldTicks();
            case "weapons" -> snake == null ? 0 : snake.getWeapons().size();
            case "enemies" -> entities.stream().filter(Enemy.class::isInstance).count();
            default -> variables.get(name);
        };
    }

    public void setVariable(String name, double value) {
        variables.put(name, value);
    }

    public void growSnake() {
        snake.grow();
    }

    public void addActiveEffect(EffectSource effect) {
        activeEffects.add(effect);
    }

    /** Advances every collected effect and reports each one that runs out. */
    public void updateEffects() {
        for (EffectSource effect : new ArrayList<>(activeEffects)) {
            effect.updateEffect(snake);
            if (effect.isExpired()) {
                activeEffects.remove(effect);
                addEvent(new Event(Event.Type.EFFECT_EXPIRED, null, effect.effectName()));
            }
        }
    }

    /** Places a solid wall from the scenario layout. */
    public void addWall(Scenario.Cell cell) throws InvalidScenarioException {
        double x = cell.column() * GameObject.CELL;
        double y = cell.row() * GameObject.CELL;
        boolean blocksStart = snake != null
                && (snake.contains(x, y) || (x == snake.getX() + GameObject.CELL && y == snake.getY()));
        if (blocksStart) {
            throw new InvalidScenarioException("Wall " + cell.column() + ":" + cell.row() + " blocks the snake's start");
        }
        addEntity(new Obstacle(x, y, false));
    }

    public void spawnFood() throws BoardFullException {
        double[] position = freePosition();
        addEntity(new Food(position[0], position[1]));
    }

    /** Overloaded spawn: the parameter type decides which entity is created. */
    public void spawn(Enemy.Kind kind) throws BoardFullException {
        double[] position = freePosition();
        String strategy = strategyOverrides.getOrDefault(kind, kind.defaultStrategy());
        addEntity(new Enemy(position[0], position[1], kind, MovementStrategy.named(strategy)));
    }

    /** Changes how every enemy of a kind moves, including enemies spawned later. */
    public void setStrategy(Enemy.Kind kind, String strategyName) {
        strategyOverrides.put(kind, strategyName);
        for (GameObject entity : entities) {
            if (entity instanceof Enemy enemy && enemy.getKind() == kind) {
                enemy.setStrategy(MovementStrategy.named(strategyName));
            }
        }
    }

    public void spawn(Bonus.Type type) throws BoardFullException {
        double[] position = freePosition();
        addEntity(new Bonus(position[0], position[1], type));
    }

    public void spawn(Weapon.Type type) throws BoardFullException {
        double[] position = freePosition();
        addEntity(new Weapon(position[0], position[1], type));
    }

    public void addObstacles(int count) throws InvalidActionException {
        if (count < 0) throw new InvalidActionException("Obstacle count cannot be negative");
        for (int i = 0; i < count; i++) {
            double[] position = freePosition();
            addEntity(new Obstacle(position[0], position[1], i % 2 == 1));
        }
    }

    private double[] freePosition() throws BoardFullException {
        for (int attempt = 0; attempt < 500; attempt++) {
            double x = random.nextInt(GameObject.COLUMNS) * GameObject.CELL;
            double y = random.nextInt(GameObject.ROWS) * GameObject.CELL;
            boolean occupied = snake != null && snake.contains(x, y);
            for (GameObject entity : entities) {
                if (entity.getX() == x && entity.getY() == y) occupied = true;
            }
            if (!occupied) return new double[]{x, y};
        }
        throw new BoardFullException("Board is full; no free cell is available");
    }
}
