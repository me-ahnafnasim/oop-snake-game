package game;

import entity.Enemy;
import entity.GameObject;
import exception.InvalidScenarioException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Configuration for a playable world. It contains data, not game behaviour. */
public final class Scenario {

    /** What the player must reach. Each constant measures progress in its own way. */
    public enum Objective {
        SCORE {
            @Override public long progress(SimulationState state) { return state.getScore(); }
            @Override public String describe(int goal) { return "score " + goal; }
        },
        SURVIVE {
            @Override public long progress(SimulationState state) { return state.getTick(); }
            @Override public String describe(int goal) { return "survive " + goal + " ticks"; }
        };

        public abstract long progress(SimulationState state);
        public abstract String describe(int goal);
    }

    /** One board cell, counted in columns and rows from the top-left corner. */
    public record Cell(int column, int row) { }

    /** Numbers that change how objects behave: points per food, bonus duration, bomb radius in cells. */
    public record Tuning(int foodPoints, int bonusTicks, int bombRadius) {
        public static final Tuning DEFAULT = new Tuning(10, 20, 2);
    }

    private static final List<Enemy.Kind> DEFAULT_ENEMIES = List.of(Enemy.Kind.CHASER, Enemy.Kind.PATROLLER);

    private static final Set<String> KEYS = Set.of("name", "seed", "food", "enemies", "obstacles", "weapons",
            "health", "objective", "targetScore", "surviveTicks", "levels", "walls", "rules",
            "enemyTypes", "foodPoints", "bonusTicks", "bombRadius");

    private final String name;
    private final long seed;
    private final int foodCount;
    private final int enemyCount;
    private final int obstacleCount;
    private final int weaponCount;
    private final Objective objective;
    private final int goal;
    private final int levels;
    private final int initialHealth;
    private final List<Cell> walls;
    private final List<String> ruleFiles;
    private final List<Enemy.Kind> enemyKinds;
    private final Tuning tuning;

    public Scenario(String name, long seed, int foodCount, int enemyCount, int obstacleCount, int weaponCount,
                    Objective objective, int goal, int levels, int initialHealth,
                    List<Cell> walls, List<String> ruleFiles, List<Enemy.Kind> enemyKinds, Tuning tuning) {
        this.name = name;
        this.seed = seed;
        this.foodCount = foodCount;
        this.enemyCount = enemyCount;
        this.obstacleCount = obstacleCount;
        this.weaponCount = weaponCount;
        this.objective = objective;
        this.goal = goal;
        this.levels = levels;
        this.initialHealth = initialHealth;
        this.walls = List.copyOf(walls);
        this.ruleFiles = List.copyOf(ruleFiles);
        this.enemyKinds = List.copyOf(enemyKinds);
        this.tuning = tuning;
        validate();
    }

    public static Scenario standard() {
        return new Scenario("Standard", 20260831L, 1, 2, 0, 2, Objective.SCORE, 100, 3, 3,
                List.of(), List.of(), DEFAULT_ENEMIES, Tuning.DEFAULT);
    }

    public static Scenario challenge() {
        return new Scenario("Challenge", 20260901L, 1, 2, 5, 2, Objective.SCORE, 150, 3, 3,
                List.of(), List.of(), DEFAULT_ENEMIES, Tuning.DEFAULT);
    }

    public static Scenario fromFile(Path path) throws InvalidScenarioException {
        try {
            Map<String, String> values = new HashMap<>();
            for (String raw : Files.readAllLines(path)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    throw new InvalidScenarioException("Invalid scenario line: " + line);
                }
                String key = line.substring(0, separator).trim();
                if (!KEYS.contains(key)) throw new InvalidScenarioException("Unknown scenario field: " + key);
                values.put(key, line.substring(separator + 1).trim());
            }
            Objective objective = named(Objective.class, values.getOrDefault("objective", "SCORE"), "objective");
            int goal = number(values, objective == Objective.SCORE ? "targetScore" : "surviveTicks");
            int levels = optionalNumber(values, "levels", 1);
            List<Enemy.Kind> kinds = new ArrayList<>();
            for (String kind : list(values.getOrDefault("enemyTypes", ""))) kinds.add(named(Enemy.Kind.class, kind, "enemy type"));
            Tuning tuning = new Tuning(
                    optionalNumber(values, "foodPoints", Tuning.DEFAULT.foodPoints()),
                    optionalNumber(values, "bonusTicks", Tuning.DEFAULT.bonusTicks()),
                    optionalNumber(values, "bombRadius", Tuning.DEFAULT.bombRadius()));
            return new Scenario(
                    required(values, "name"),
                    Long.parseLong(required(values, "seed")),
                    number(values, "food"),
                    number(values, "enemies"),
                    number(values, "obstacles"),
                    number(values, "weapons"),
                    objective,
                    goal,
                    levels,
                    number(values, "health"),
                    cells(values.getOrDefault("walls", "")),
                    list(values.getOrDefault("rules", "")),
                    kinds.isEmpty() ? DEFAULT_ENEMIES : kinds,
                    tuning);
        } catch (IOException | IllegalArgumentException e) {
            throw new InvalidScenarioException("Cannot read scenario " + path.getFileName() + ": " + e.getMessage());
        }
    }

    private static String required(Map<String, String> values, String key) throws InvalidScenarioException {
        String value = values.get(key);
        if (value == null || value.isBlank()) throw new InvalidScenarioException("Missing scenario field: " + key);
        return value;
    }

    private static int number(Map<String, String> values, String key) throws InvalidScenarioException {
        String value = required(values, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidScenarioException("Field " + key + " must be a whole number, not " + value);
        }
    }

    private static int optionalNumber(Map<String, String> values, String key, int fallback)
            throws InvalidScenarioException {
        return values.containsKey(key) ? number(values, key) : fallback;
    }

    private static <E extends Enum<E>> E named(Class<E> type, String name, String label) throws InvalidScenarioException {
        try {
            return Enum.valueOf(type, name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidScenarioException("Unknown " + label + " " + name + "; use one of "
                    + Arrays.toString(type.getEnumConstants()));
        }
    }

    /** Parses "column:row, column:row". */
    private static List<Cell> cells(String text) throws InvalidScenarioException {
        List<Cell> cells = new ArrayList<>();
        for (String part : list(text)) {
            String[] numbers = part.split(":");
            try {
                if (numbers.length != 2) throw new NumberFormatException();
                cells.add(new Cell(Integer.parseInt(numbers[0].trim()), Integer.parseInt(numbers[1].trim())));
            } catch (NumberFormatException e) {
                throw new InvalidScenarioException("Invalid wall cell '" + part + "'; use column:row");
            }
        }
        return cells;
    }

    private static List<String> list(String text) {
        return Arrays.stream(text.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    private void validate() {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("A scenario needs a name");
        if (objective == null) throw new IllegalArgumentException("A scenario needs an objective");
        if (foodCount < 0 || enemyCount < 0 || obstacleCount < 0 || weaponCount < 0) {
            throw new IllegalArgumentException("Entity counts cannot be negative");
        }
        if (goal <= 0 || initialHealth <= 0) throw new IllegalArgumentException("Goal and health must be positive");
        if (levels < 1 || levels > goal) throw new IllegalArgumentException("Levels must be between 1 and the goal");
        if (enemyKinds.isEmpty()) throw new IllegalArgumentException("enemyTypes needs at least one type");
        if (tuning == null || tuning.foodPoints() < 0 || tuning.bonusTicks() <= 0 || tuning.bombRadius() < 0) {
            throw new IllegalArgumentException("foodPoints and bombRadius cannot be negative; bonusTicks must be positive");
        }
        for (Cell wall : walls) {
            if (wall.column() < 0 || wall.column() >= GameObject.COLUMNS || wall.row() < 0 || wall.row() >= GameObject.ROWS) {
                throw new IllegalArgumentException("Wall " + wall.column() + ":" + wall.row() + " is outside the "
                        + GameObject.COLUMNS + "x" + GameObject.ROWS + " board");
            }
        }
        for (String file : ruleFiles) {
            if (!file.matches("[A-Za-z0-9_-]+\\.rule")) {
                throw new IllegalArgumentException("Rule file names must look like name.rule: " + file);
            }
        }
    }

    public String getName() { return name; }
    public long getSeed() { return seed; }
    public int getFoodCount() { return foodCount; }
    public int getEnemyCount() { return enemyCount; }
    public int getObstacleCount() { return obstacleCount; }
    public int getWeaponCount() { return weaponCount; }
    public Objective getObjective() { return objective; }
    /** Target score for SCORE scenarios, number of ticks for SURVIVE scenarios. */
    public int getGoal() { return goal; }
    public int getLevels() { return levels; }
    public int getInitialHealth() { return initialHealth; }
    public List<Cell> getWalls() { return walls; }
    /** Rule files this scenario uses; empty means every enabled rule file. */
    public List<String> getRuleFiles() { return ruleFiles; }
    /** Enemy kinds to place, used in turn (CHASER, PATROLLER by default). */
    public List<Enemy.Kind> getEnemyKinds() { return enemyKinds; }
    public Tuning getTuning() { return tuning; }

    @Override
    public String toString() {
        return name + " (" + objective.describe(goal) + ", " + levels + (levels == 1 ? " level)" : " levels)");
    }
}
