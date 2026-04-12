package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.client.net.FxNetworkClient;
import ch.unibas.dmi.dbis.cs108.example.client.ui.MainController;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.List;

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
     * @param stage the primary JavaFX stage
     */
    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(stage, state, networkClient);

        PrefillData prefill = parsePrefillArguments(getParameters().getRaw());
        controller.showConnectView(prefill.host(), prefill.port(), prefill.username());

        stage.setTitle("Frantic^-1");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        loadWindowIcon(stage);
        stage.show();
    }

    /**
     * Stops the JavaFX application and disconnects from the server.
     */
    @Override
    public void stop() {
        networkClient.disconnect();
    }

    /**
     * Parses command-line arguments forwarded to the GUI client.
     *
     * <p>Supported formats:</p>
     * <ul>
     *   <li>no arguments → localhost / 5555 / system username</li>
     *   <li>{@code host:port}</li>
     *   <li>{@code host:port username}</li>
     * </ul>
     *
     * <p>Invalid values fall back to defaults rather than aborting the GUI.</p>
     *
     * @param rawArgs raw JavaFX application arguments
     * @return parsed prefill data
     */
    private PrefillData parsePrefillArguments(List<String> rawArgs) {
        String host = "localhost";
        String port = "5555";
        String username = System.getProperty("user.name", "Player");

        if (rawArgs.size() >= 1) {
            String hostPort = rawArgs.get(0).trim();
            String[] parts = hostPort.split(":", 2);

            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                host = parts[0].trim();

                try {
                    int parsedPort = Integer.parseInt(parts[1].trim());
                    if (parsedPort >= 1 && parsedPort <= 65535) {
                        port = String.valueOf(parsedPort);
                    }
                } catch (NumberFormatException ignored) {
                    // Keep default port if invalid.
                }
            }
        }

        if (rawArgs.size() >= 2) {
            String candidateUsername = rawArgs.get(1).trim();
            if (!candidateUsername.isBlank()) {
                username = candidateUsername;
            }
        }

        return new PrefillData(host, port, username);
    }

    /**
     * Attempts to load a window icon from the application resources.
     *
     * @param stage the application stage
     */
    private void loadWindowIcon(Stage stage) {
        try (InputStream in = getClass().getResourceAsStream("/images/app/icon.png")) {
            if (in != null) {
                stage.getIcons().add(new Image(in));
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Small immutable holder for connect-screen prefill values.
     *
     * @param host the host text
     * @param port the port text
     * @param username the username text
     */
    private record PrefillData(String host, String port, String username) {
    }
}