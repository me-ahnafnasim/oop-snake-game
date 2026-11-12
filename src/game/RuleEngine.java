package game;

import entity.Bonus;
import entity.Enemy;
import entity.Weapon;
import exception.InvalidActionException;
import exception.InvalidRuleException;
import exception.RuleExecutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and executes player-authored rule files. */
public final class RuleEngine {
    private static final Pattern HEADER = Pattern.compile(
            "^RULE\\s+([A-Za-z][A-Za-z0-9_]*)\\s+PRIORITY\\s+(-?\\d+)\\s+WHEN\\s+(.+?)\\s+DO\\s+(.+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CONDITION = Pattern.compile(
            "^([A-Za-z][A-Za-z0-9_.]*)\\s*(==|!=|>=|<=|>|<)\\s*(.+)$");
    private static final Pattern ACTION = Pattern.compile("^([A-Za-z_]+)\\s*\\((.*)\\)$");

    private final List<Rule> rules = new ArrayList<>();

    public void register(Rule rule) {
        rules.add(rule);
        rules.sort(Comparator.comparingInt(Rule::priority).thenComparing(Rule::name));
    }

    public void clear() {
        rules.clear();
    }

    public List<Rule> getRules() {
        return List.copyOf(rules);
    }

    public List<String> loadDirectory(Path directory) {
        return loadDirectory(directory, List.of());
    }

    /**
     * Loads every valid enabled .rule file and returns one message per file that could not be loaded.
     * When {@code only} is not empty, just those file names are loaded. A bad file never blocks the others.
     */
    public List<String> loadDirectory(Path directory, List<String> only) {
        List<String> problems = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            if (!only.isEmpty()) problems.add("Rule folder not found: " + directory.toAbsolutePath());
            return problems;
        }
        List<Path> files;
        try (var listing = Files.list(directory)) {
            files = listing.filter(path -> path.getFileName().toString().endsWith(".rule")).sorted().toList();
        } catch (IOException e) {
            problems.add("Cannot list rules: " + e.getMessage());
            return problems;
        }
        if (!only.isEmpty()) {
            for (String name : only) {
                if (files.stream().noneMatch(path -> path.getFileName().toString().equals(name))) {
                    problems.add(name + ": used by the scenario but missing or disabled");
                }
            }
            files = files.stream().filter(path -> only.contains(path.getFileName().toString())).toList();
        }
        for (Path path : files) {
            String file = path.getFileName().toString();
            try {
                PlayerRule rule = parse(Files.readString(path));
                if (rules.stream().anyMatch(existing -> existing.name().equalsIgnoreCase(rule.name()))) {
                    problems.add(file + ": another rule is already named " + rule.name());
                } else {
                    register(rule);
                }
            } catch (IOException e) {
                problems.add(file + ": cannot be read");
            } catch (InvalidRuleException e) {
                problems.add(file + ": " + e.getMessage());
            }
        }
        return problems;
    }

    public PlayerRule parse(String raw) throws InvalidRuleException {
        String line = raw.lines()
                .map(String::trim)
                .filter(value -> !value.isEmpty() && !value.startsWith("#"))
                .findFirst()
                .orElseThrow(() -> new InvalidRuleException("Rule file is empty"));
        Matcher header = HEADER.matcher(line);
        if (!header.matches()) {
            throw new InvalidRuleException("Expected RULE <name> PRIORITY <number> WHEN <condition> DO <actions>");
        }
        String name = header.group(1);
        int priority;
        try {
            priority = Integer.parseInt(header.group(2));
        } catch (NumberFormatException e) {
            throw new InvalidRuleException("Rule priority must be an integer");
        }
        Condition condition = parseExpression(header.group(3).trim());
        List<RuleAction> actions;
        try {
            actions = Arrays.stream(header.group(4).split("\\s*;\\s*"))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(this::parseActionSafely)
                    .toList();
        } catch (RuleLoadingFailure e) {
            throw e.exception;
        }
        if (actions.isEmpty()) throw new InvalidRuleException("A rule must contain at least one action");
        return new PlayerRule(name, priority, condition, actions);
    }

    public void process(SimulationState state) throws RuleExecutionException {
        for (Event event : state.getEvents()) {
            for (Rule rule : rules) {
                if (rule.applies(state, event)) rule.execute(state);
            }
        }
    }

    private Condition parseExpression(String expression) throws InvalidRuleException {
        String[] orParts = expression.split("\\s+OR\\s+", -1);
        List<Condition> alternatives = new ArrayList<>();
        for (String orPart : orParts) {
            String[] andParts = orPart.trim().split("\\s+AND\\s+", -1);
            List<Condition> all = new ArrayList<>();
            for (String atom : andParts) all.add(parseCondition(atom.trim()));
            alternatives.add((state, event) -> {
                for (Condition condition : all) if (!condition.test(state, event)) return false;
                return true;
            });
        }
        return (state, event) -> {
            for (Condition alternative : alternatives) if (alternative.test(state, event)) return true;
            return false;
        };
    }

    private Condition parseCondition(String expression) throws InvalidRuleException {
        Matcher matcher = CONDITION.matcher(expression);
        if (!matcher.matches()) throw new InvalidRuleException("Invalid condition: " + expression);
        String field = matcher.group(1);
        String operator = matcher.group(2);
        String expected = clean(matcher.group(3));
        if ("event".equals(field) && !isEvent(expected)) {
            throw new InvalidRuleException("Unknown event: " + expected);
        }
        if (!"event".equals(field) && !isNumber(expected) && !(field.equals("event.detail"))) {
            throw new InvalidRuleException("Expected a numeric value for " + field);
        }
        return (state, event) -> compare(state, event, field, operator, expected);
    }

    private boolean compare(SimulationState state, Event event, String field, String operator, String expected)
            throws InvalidRuleException {
        Object actual = "event".equals(field) ? event.getType().name()
                : "event.detail".equals(field) ? event.getDetail() : state.valueOf(field);
        if (actual == null) throw new InvalidRuleException("Unknown state field: " + field);
        if (actual instanceof Number number) {
            double left = number.doubleValue();
            double right;
            try { right = Double.parseDouble(expected); }
            catch (NumberFormatException e) { throw new InvalidRuleException("Invalid numeric value: " + expected); }
            return switch (operator) {
                case "==" -> left == right;
                case "!=" -> left != right;
                case ">" -> left > right;
                case "<" -> left < right;
                case ">=" -> left >= right;
                case "<=" -> left <= right;
                default -> false;
            };
        }
        boolean equal = actual.toString().equalsIgnoreCase(expected);
        return "==".equals(operator) ? equal : "!=".equals(operator) && !equal;
    }

    private RuleAction parseActionSafely(String text) {
        try {
            return parseAction(text);
        } catch (InvalidRuleException e) {
            throw new RuleLoadingFailure(e);
        }
    }

    private RuleAction parseAction(String text) throws InvalidRuleException {
        Matcher matcher = ACTION.matcher(text);
        if (!matcher.matches()) throw new InvalidRuleException("Invalid action: " + text);
        String operation = matcher.group(1).toLowerCase(Locale.ROOT);
        String[] args = Arrays.stream(matcher.group(2).split("\\s*,\\s*"))
                .map(RuleEngine::clean).toArray(String[]::new);
        return switch (operation) {
            case "add_score" -> state -> state.addScore(integer(args, 0, operation));
            case "change_health" -> state -> state.changeHealth(integer(args, 0, operation));
            case "add_obstacle" -> state -> state.addObstacles(integer(args, 0, operation));
            case "grow_snake" -> state -> state.growSnake();
            case "spawn" -> spawnAction(args);
            case "set_strategy" -> strategyAction(args);
            case "end_scenario" -> endAction(args);
            case "set_variable" -> state -> state.setVariable(textArg(args, 0, operation), decimal(args, 1, operation));
            default -> throw new InvalidRuleException("Unknown action: " + operation);
        };
    }

    private RuleAction spawnAction(String[] args) throws InvalidRuleException {
        if (args.length != 2) throw new InvalidRuleException("spawn requires category and type");
        String type = args[1].toUpperCase(Locale.ROOT);
        return switch (args[0].toUpperCase(Locale.ROOT)) {
            case "ENEMY" -> {
                Enemy.Kind enemy = enumValue(Enemy.Kind.class, type, "enemy");
                yield state -> state.spawn(enemy);
            }
            case "BONUS" -> {
                Bonus.Type bonus = enumValue(Bonus.Type.class, type, "bonus");
                yield state -> state.spawn(bonus);
            }
            case "WEAPON" -> {
                Weapon.Type weapon = enumValue(Weapon.Type.class, type, "weapon");
                yield state -> state.spawn(weapon);
            }
            default -> throw new InvalidRuleException("spawn category must be ENEMY, BONUS, or WEAPON");
        };
    }

    /** set_strategy(KIND, STRATEGY): changes how one enemy kind moves from now on. */
    private RuleAction strategyAction(String[] args) throws InvalidRuleException {
        if (args.length != 2) throw new InvalidRuleException("set_strategy requires an enemy kind and a strategy");
        Enemy.Kind kind = enumValue(Enemy.Kind.class, args[0].toUpperCase(Locale.ROOT), "enemy");
        String strategy = args[1].toUpperCase(Locale.ROOT);
        try {
            MovementStrategy.named(strategy);
        } catch (IllegalArgumentException e) {
            throw new InvalidRuleException(e.getMessage());
        }
        return state -> state.setStrategy(kind, strategy);
    }

    /** Converts at parse time so an unknown type rejects the rule before it ever runs. */
    private static <E extends Enum<E>> E enumValue(Class<E> type, String name, String label) throws InvalidRuleException {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            throw new InvalidRuleException("Unknown " + label + " type: " + name);
        }
    }

    private RuleAction endAction(String[] args) throws InvalidRuleException {
        if (args.length != 1 || !(args[0].equalsIgnoreCase("WIN") || args[0].equalsIgnoreCase("LOSS"))) {
            throw new InvalidRuleException("end_scenario requires WIN or LOSS");
        }
        return state -> state.endScenario(args[0], "Rule ended the scenario");
    }

    private int integer(String[] args, int index, String operation) throws InvalidActionException {
        try { return Integer.parseInt(textArg(args, index, operation)); }
        catch (NumberFormatException e) { throw new InvalidActionException(operation + " requires an integer"); }
    }

    private double decimal(String[] args, int index, String operation) throws InvalidActionException {
        try { return Double.parseDouble(textArg(args, index, operation)); }
        catch (NumberFormatException e) { throw new InvalidActionException(operation + " requires a number"); }
    }

    private String textArg(String[] args, int index, String operation) throws InvalidActionException {
        if (index >= args.length || args[index].isBlank()) throw new InvalidActionException(operation + " has missing arguments");
        return args[index];
    }

    private static String clean(String value) {
        String result = value.trim();
        if (result.length() >= 2 && result.startsWith("\"") && result.endsWith("\"")) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }

    private static boolean isNumber(String value) {
        try { Double.parseDouble(value); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private static boolean isEvent(String value) {
        try { Event.Type.valueOf(value.toUpperCase(Locale.ROOT)); return true; }
        catch (IllegalArgumentException e) { return false; }
    }

    private static final class RuleLoadingFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final InvalidRuleException exception;
        private RuleLoadingFailure(InvalidRuleException exception) { this.exception = exception; }
    }
}
