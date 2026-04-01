package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Example JavaFX application kept for reference.
 *
 * <p>This class is not used by the actual Frantic^-1 GUI client.
 * The real GUI is launched via {@link GuiMain} and implemented in
 * {@link FranticFxApp}.</p>
 */
public class GUI extends Application {

    /**
     * Launches the example JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Starts the example JavaFX application window.
     *
     * @param stage the primary JavaFX stage
     */
    @Override
    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Label label = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
        Scene scene = new Scene(new StackPane(label), 640, 480);
        stage.setScene(scene);
        stage.show();
    }
}