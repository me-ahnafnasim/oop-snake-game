package app;

import entity.GameObject;
import gui.GameController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        GameController controller = new GameController();
        Pane root = controller.getRoot();
        root.setFocusTraversable(true);
        Scene scene = new Scene(root, GameObject.WIDTH, GameObject.HEIGHT);
        controller.initKeyListeners(scene);
        root.requestFocus();

        primaryStage.setTitle("Snake Game");
        primaryStage.setScene(scene);
        primaryStage.show();
        root.requestFocus();

        controller.startGameLoop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
