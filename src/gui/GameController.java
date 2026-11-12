package gui;

import entity.GameObject;
import game.Direction;
import game.Game;
import game.Scenario;
import game.ScenarioManager;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** JavaFX adapter; the simulation rules remain inside the engine. */
public class GameController {
    private static final Path RULE_DIRECTORY = Path.of("rules");
    private static final Path SCENARIO_DIRECTORY = Path.of("scenarios");
    /** Longest gap between two frames that is simulated; longer stalls (dialogs, window drags) are skipped. */
    private static final double MAX_FRAME_SECONDS = 0.25;

    private final Pane root;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private Game game;
    private Scenario scenario;
    private boolean paused;
    private boolean started;
    private double accumulator;
    private long lastFrameNanos;

    public GameController() {
        root = new Pane();
        canvas = new Canvas(GameObject.WIDTH, GameObject.HEIGHT);
        gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);
        scenario = Scenario.standard();
        game = new Game(scenario);
    }

    public Pane getRoot() { return root; }

    public void initKeyListeners(Scene scene) {
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP, W -> game.setDirection(Direction.UP);
                case DOWN, S -> game.setDirection(Direction.DOWN);
                case LEFT, A -> game.setDirection(Direction.LEFT);
                case RIGHT, D -> game.setDirection(Direction.RIGHT);
                case ENTER -> started = true;
                case P -> paused = !paused;
                case F2 -> openRuleManager();
                case F3 -> openScenarioManager();
                case SPACE -> game.useWeapon();
                case R -> restart();
            }
        });
    }

    public void startGameLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Measure real time so the game runs at the same speed on 60 Hz and 144 Hz screens.
                double elapsed = lastFrameNanos == 0 ? 0 : (now - lastFrameNanos) / 1e9;
                lastFrameNanos = now;
                if (!started) {
                    renderStartScreen();
                    return;
                }
                if (game.isGameOver() || game.isWon()) {
                    renderEndScreen();
                    return;
                }
                if (!paused) {
                    accumulator += Math.min(elapsed, MAX_FRAME_SECONDS);
                    double moveInterval = game.getSnake().getMoveInterval(game.getSpeedLevel());
                    while (accumulator >= moveInterval && !game.isGameOver() && !game.isWon()) {
                        accumulator -= moveInterval;
                        game.update();
                    }
                }
                render();
            }
        };
        timer.start();
    }

    private void restart() {
        game = new Game(scenario);
        paused = false;
        started = true;
        accumulator = 0;
    }

    private void openRuleManager() {
        boolean wasPaused = paused;
        paused = true;
        new RuleManagerDialog(RULE_DIRECTORY).showAndWait();
        List<String> problems = game.reloadRules(RULE_DIRECTORY);
        if (!problems.isEmpty()) {
            showMessage(Alert.AlertType.WARNING, "Some rules were not loaded", String.join("\n", problems));
        }
        paused = wasPaused;
    }

    private void openScenarioManager() {
        boolean wasPaused = paused;
        paused = true;
        List<Scenario> scenarios = new ArrayList<>();
        try {
            scenarios.addAll(ScenarioManager.loadDirectory(SCENARIO_DIRECTORY));
        } catch (Exception e) {
            showMessage(Alert.AlertType.ERROR, "Scenario error", e.getMessage());
        }
        if (scenarios.isEmpty()) {
            scenarios.add(Scenario.standard());
            scenarios.add(Scenario.challenge());
        }
        ChoiceDialog<Scenario> dialog = new ChoiceDialog<>(scenarios.get(0), scenarios);
        dialog.setTitle("Scenario Manager");
        dialog.setHeaderText("Choose a player-editable scenario");
        dialog.setContentText("Scenario:");
        dialog.showAndWait().ifPresentOrElse(selected -> {
            scenario = selected;
            restart();
        }, () -> paused = wasPaused);
    }

    private void showMessage(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "Unknown error" : message);
        alert.showAndWait();
    }

    private void render() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, GameObject.WIDTH, GameObject.HEIGHT);
        drawGrid();
        gc.setFont(Font.font("Arial", 14));
        game.render(gc);
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("F2: rules   F3: scenario   SPACE: fire weapon   P: pause", 10, GameObject.HEIGHT - 12);
        if (paused) {
            gc.setFill(Color.YELLOW);
            gc.setFont(Font.font("Arial", 30));
            gc.fillText("PAUSED", 330, 300);
        }
    }

    private void drawGrid() {
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(0.5);
        for (double x = 0; x < GameObject.WIDTH; x += GameObject.CELL) gc.strokeLine(x, 0, x, GameObject.HEIGHT);
        for (double y = 0; y < GameObject.HEIGHT; y += GameObject.CELL) gc.strokeLine(0, y, GameObject.WIDTH, y);
    }

    private void renderStartScreen() {
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(0, 0, GameObject.WIDTH, GameObject.HEIGHT);
        gc.setFill(Color.LIMEGREEN);
        gc.setFont(Font.font("Arial", 40));
        gc.fillText("SNAKE SIMULATOR", 220, 240);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 18));
        gc.fillText("Arrow Keys / WASD: move   SPACE: fire weapon", 200, 290);
        gc.fillText("ENTER: start   F2: rules   F3: scenario", 240, 330);
    }

    private void renderEndScreen() {
        gc.setFill(game.isWon() ? Color.DARKGREEN : Color.DARKRED);
        gc.fillRect(0, 0, GameObject.WIDTH, GameObject.HEIGHT);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 36));
        gc.fillText(game.isWon() ? "SCENARIO COMPLETE" : "GAME OVER", 230, 270);
        gc.setFont(Font.font("Arial", 18));
        gc.fillText(game.getStatusMessage(), 250, 315);
        gc.fillText("Press R to restart or F3 to choose a scenario", 210, 360);
    }
}
