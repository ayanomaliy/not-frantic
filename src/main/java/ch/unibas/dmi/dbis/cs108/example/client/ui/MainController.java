package ch.unibas.dmi.dbis.cs108.example.client.ui;

import animatefx.animation.FadeIn;
import animatefx.animation.FadeInUp;
import ch.unibas.dmi.dbis.cs108.example.client.net.FxNetworkClient;
import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextInputDialog;

/**
 * Coordinates JavaFX view changes and user interaction for the graphical
 * Frantic^-1 client.
 *
 * <p>This controller connects the visual components to the client state and
 * the network layer. It also applies the shared stylesheet and simple
 * AnimateFX transitions whenever a new screen is shown.</p>
 */
public class MainController {

    private static final String THEME_STYLESHEET = "/css/frantic-theme.css";

    private final Stage stage;
    private final ClientState state;
    private final FxNetworkClient networkClient;

    /**
     * Creates a new main controller.
     *
     * @param stage the primary stage
     * @param state the shared client state
     * @param networkClient the network client used for server communication
     */
    public MainController(Stage stage, ClientState state, FxNetworkClient networkClient) {
        this.stage = stage;
        this.state = state;
        this.networkClient = networkClient;
    }

    /**
     * Shows the connect screen.
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

        Scene scene = createStyledScene(view, 640, 420);
        stage.setScene(scene);
        new FadeInUp(view).play();
    }

    /**
     * Shows the lobby screen.
     */
    public void showLobbyView() {
        LobbyView view = new LobbyView();

        view.getLobbyPlayersList().setItems(state.getPlayers());
        view.getAllPlayersList().setItems(state.getAllPlayers());
        view.getLobbiesList().setItems(state.getLobbies());
        view.getInfoList().setItems(state.getGameMessages());

        updateDisplayedChat(view);

        view.getSendButton().setOnAction(e -> sendChat(view));
        view.getChatInput().setOnAction(e -> sendChat(view));

        view.getCommandButton().setOnAction(e -> sendCommand(view));
        view.getCommandInput().setOnAction(e -> sendCommand(view));

        view.getRefreshPlayersButton().setOnAction(e -> networkClient.requestPlayers());
        view.getRefreshLobbiesButton().setOnAction(e -> networkClient.requestLobbies());

        view.getChatModeButton().textProperty().bind(state.chatModeProperty());

        view.getChatModeButton().setOnAction(e -> {
            switch (state.getChatMode()) {
                case "Global" -> state.setChatMode("Lobby");
                case "Lobby" -> state.setChatMode("Whisper");
                default -> state.setChatMode("Global");
            }

            updateDisplayedChat(view);
        });

        view.getJoinLobbyButton().setOnAction(e -> {
            String selectedLobby = view.getLobbiesList().getSelectionModel().getSelectedItem();
            if (selectedLobby != null && !selectedLobby.isBlank()) {
                networkClient.sendCommand("/join " + selectedLobby);
            }
        });

        view.getLobbiesList().setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedLobby = view.getLobbiesList().getSelectionModel().getSelectedItem();
                if (selectedLobby != null && !selectedLobby.isBlank()) {
                    networkClient.sendCommand("/join " + selectedLobby);
                }
            }
        });

        view.getCreateLobbyButton().setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Create Lobby");
            dialog.setHeaderText("Create a new lobby");
            dialog.setContentText("Lobby name:");

            dialog.showAndWait().ifPresent(name -> {
                String trimmed = name.trim();
                if (!trimmed.isBlank()) {
                    networkClient.sendCommand("/create " + trimmed);
                }
            });
        });

        // view.getStartButton().setOnAction(e -> networkClient.startGame());
        view.getStartButton().setOnAction(e -> {
            networkClient.startGame();
            showGameView();
        });
        view.getDisconnectButton().setOnAction(e -> {
            networkClient.disconnect();
            showConnectView();
        });

        networkClient.requestPlayers();
        networkClient.requestAllPlayers();
        networkClient.requestLobbies();

        Scene scene = createStyledScene(view, 1280, 800);
        stage.setScene(scene);
        new FadeIn(view).play();
    }

    public void showGameView() {
        GameView view = new GameView();

        view.getCurrentPlayerLabel().setText("Current Player: HP");
        view.getPhaseLabel().setText("Phase: AWAITING_PLAY");
        view.getDiscardTopLabel().setText("Top Card: RED 5");

        view.getDrawButton().setOnAction(e ->
                state.getGameMessages().add("[CLIENT] Draw button clicked.")
        );

        view.getEndTurnButton().setOnAction(e ->
                state.getGameMessages().add("[CLIENT] End turn clicked.")
        );

        view.getBackToLobbyButton().setOnAction(e -> showLobbyView());

        Scene scene = createStyledScene(view, 1280, 800);
        stage.setScene(scene);
        new FadeIn(view).play();
    }

    /**
     * Updates the chat list view to display the message list of the currently selected chat mode.
     *
     * @param view the lobby view containing the chat list
     */
    private void updateDisplayedChat(LobbyView view) {
        switch (state.getChatMode()) {
            case "Lobby" -> view.getChatList().setItems(state.getLobbyChatMessages());
            case "Whisper" -> view.getChatList().setItems(state.getWhisperChatMessages());
            default -> view.getChatList().setItems(state.getGlobalChatMessages());
        }
    }

    /**
     * Sends the content of the chat input field using the currently selected chat mode.
     *
     * <p>If the current mode is Whisper, the input must be a whisper command such as
     * {@code /w Alice hello}. In Global or Lobby mode, normal text is sent to the
     * corresponding chat channel.</p>
     *
     * @param view the lobby view containing the chat input
     */
    private void sendChat(LobbyView view) {
        String text = view.getChatInput().getText().trim();
        if (text.isBlank()) {
            return;
        }

        String lower = text.toLowerCase();

        if ("Whisper".equals(state.getChatMode())) {
            if (lower.startsWith("/w ")
                    || lower.startsWith("/whisper ")
                    || lower.startsWith("/msg ")
                    || lower.startsWith("/tell ")) {
                networkClient.sendCommand(text);
            } else {
                state.getGameMessages().add("[CLIENT] Whisper mode expects: /w <player> <message>");
                return;
            }
        } else if ("Lobby".equals(state.getChatMode())) {
            networkClient.sendLobbyChat(text);
        } else {
            networkClient.sendGlobalChat(text);
        }

        view.getChatInput().clear();
    }

    /**
     * Executes the content of the command field as a terminal-style client
     * command.
     *
     * <p>If the command disconnects the client, the connect screen is shown
     * again.</p>
     *
     * @param view the lobby view containing the command input
     */
    private void sendCommand(LobbyView view) {
        String command = view.getCommandInput().getText().trim();
        if (command.isBlank()) {
            return;
        }

        boolean disconnected = networkClient.sendCommand(command);
        view.getCommandInput().clear();

        if (disconnected) {
            showConnectView();
        }
    }

    /**
     * Creates a scene and attaches the shared CSS theme if the resource is
     * available.
     *
     * @param root the root node of the scene
     * @param width the initial scene width
     * @param height the initial scene height
     * @return the configured scene
     */
    private Scene createStyledScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);

        scene.setFill(javafx.scene.paint.Paint.valueOf("#bfc5ff"));

        var stylesheet = getClass().getResource(THEME_STYLESHEET);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        return scene;
    }
}