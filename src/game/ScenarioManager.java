package game;

import exception.InvalidScenarioException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ScenarioManager {
    private ScenarioManager() { }

    public static List<Scenario> loadDirectory(Path directory) throws InvalidScenarioException {
        List<Scenario> scenarios = new ArrayList<>();
        if (!Files.isDirectory(directory)) return scenarios;
        try (var paths = Files.list(directory)) {
            paths.filter(path -> path.toString().endsWith(".scenario"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            scenarios.add(Scenario.fromFile(path));
                        } catch (InvalidScenarioException e) {
                            throw new ScenarioLoadingFailure(e);
                        }
                    });
        } catch (ScenarioLoadingFailure e) {
            throw e.exception;
        } catch (Exception e) {
            throw new InvalidScenarioException("Cannot load scenarios: " + e.getMessage());
        }
        return scenarios;
    }

    private static final class ScenarioLoadingFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final InvalidScenarioException exception;
        private ScenarioLoadingFailure(InvalidScenarioException exception) { this.exception = exception; }
    }
}
