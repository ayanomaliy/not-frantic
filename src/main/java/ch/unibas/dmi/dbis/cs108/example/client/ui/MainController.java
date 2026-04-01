package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.net.FxNetworkClient;
import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main GUI controller for the Frantic^-1 JavaFX client.
 *
 * <p>This controller is responsible for switching between the connect
 * screen and the lobby screen, wiring user interface events to the
 * network client, and binding the shared {@link ClientState} to the
 * corresponding JavaFX views.</p>
 */
public class MainController {

    /** Primary application window. */
    private final Stage stage;

    /** Shared client state used by the GUI. */
    private final ClientState state;

    /** Network client handling communication with the server. */
    private final FxNetworkClient networkClient;

    /**
     * Creates a new main controller.
     *
     * @param stage the primary JavaFX stage
     * @param state the shared client state
     * @param networkClient the network client used for server communication
     */
    public MainController(Stage stage, ClientState state, FxNetworkClient networkClient) {
        this.stage = stage;
        this.state = state;
        this.networkClient = networkClient;
    }

    /**
     * Displays the initial connect view.
     *
     * <p>This method creates a {@link ConnectView}, binds its status label
     * to the shared client state, and configures the connect button to
     * establish a server connection. On success, the lobby view is shown.</p>
     */
    public void showConnectView() {
        ConnectView view = new ConnectView();
        view.getStatusLabel().textProperty().bind(state.statusTextProperty());

        view.getConnectButton().setOnAction(e -> {
            try {
                String host = view.getHostField().getText().trim();
                int port = Integer.parseInt(view.getPortField().getText().trim());
                String username = view.getUsernameField().getText().trim();

                networkClient.connect(host, port, username);
                showLobbyView();
            } catch (Exception ex) {
                state.setStatusText("Connection failed: " + ex.getMessage());
            }
        });

        stage.setScene(new Scene(view, 500, 300));
    }

    /**
     * Displays the lobby view after a successful connection.
     *
     * <p>This method creates a {@link LobbyView}, binds its lists to the
     * shared client state, and wires its buttons and input field to the
     * appropriate network actions such as sending chat messages, refreshing
     * the player list, starting the game, or disconnecting.</p>
     */
    public void showLobbyView() {
        LobbyView view = new LobbyView();

        view.getPlayersList().setItems(state.getPlayers());
        view.getChatList().setItems(state.getChatMessages());
        view.getInfoList().setItems(state.getGameMessages());

        view.getSendButton().setOnAction(e -> sendChat(view));
        view.getChatInput().setOnAction(e -> sendChat(view));

        view.getRefreshPlayersButton().setOnAction(e -> networkClient.requestPlayers());
        view.getStartButton().setOnAction(e -> networkClient.startGame());
        view.getDisconnectButton().setOnAction(e -> {
            networkClient.disconnect();
            showConnectView();
        });

        stage.setScene(new Scene(view, 1000, 700));
    }

    /**
     * Sends the current chat input to the server if it is not blank.
     *
     * <p>After sending the message, the chat input field is cleared.</p>
     *
     * @param view the lobby view containing the chat input field
     */
    private void sendChat(LobbyView view) {
        String text = view.getChatInput().getText().trim();
        if (!text.isBlank()) {
            networkClient.sendChat(text);
            view.getChatInput().clear();
        }
    }
}