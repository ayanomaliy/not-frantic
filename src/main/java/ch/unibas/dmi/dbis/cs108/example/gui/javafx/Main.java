package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import javafx.application.Application;

/**
 * Wrapper class for launching the example JavaFX GUI.
 *
 * <p>This class exists because launching a JavaFX application
 * directly from the GUI class may not work reliably on all
 * platforms.</p>
 */
public class Main {

    /**
     * Launches the example JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Application.launch(GUI.class, args);
    }
}
