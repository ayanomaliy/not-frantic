package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import javafx.application.Application;
/**
 * Entry point for launching the JavaFX GUI client.
 *
 * <p>This class serves as a small wrapper around the JavaFX application
 * launcher. It starts {@link FranticFxApp}, which initializes the actual
 * graphical client for Frantic^-1.</p>
 */
public class GuiMain {
    /**
     * Launches the JavaFX GUI application.
     *
     * @param args command-line arguments passed to the JavaFX application
     */
    public static void main(String[] args) {
        Application.launch(FranticFxApp.class, args);
    }
}