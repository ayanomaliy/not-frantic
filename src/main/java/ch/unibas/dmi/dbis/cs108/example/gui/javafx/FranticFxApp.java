package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import ch.unibas.dmi.dbis.cs108.example.client.net.FxNetworkClient;
import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.client.ui.MainController;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main JavaFX application for the Frantic^-1 GUI client.
 *
 * <p>This class initializes the shared client state and network connection,
 * creates the main GUI controller, and displays the initial connect view.
 * It also ensures that the network connection is closed when the application
 * shuts down.</p>
 */
public class FranticFxApp extends Application {
    /** Shared GUI state containing connection, lobby, and chat data. */
    private final ClientState state = new ClientState();

    /** Network client responsible for communication with the server. */
    private final FxNetworkClient networkClient = new FxNetworkClient(state);


    /**
     * Starts the JavaFX application and shows the initial connect screen.
     *
     * <p>The method creates the main controller, displays the connect view,
     * and configures the primary application window.</p>
     *
     * @param stage the primary JavaFX stage
     */
    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(stage, state, networkClient);
        controller.showConnectView();

        stage.setTitle("Frantic^-1");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();
    }

    /**
     * Stops the JavaFX application and disconnects from the server.
     *
     * <p>This method is called automatically by the JavaFX runtime when
     * the application is closed.</p>
     */
    @Override
    public void stop() {
        networkClient.disconnect();
    }
}