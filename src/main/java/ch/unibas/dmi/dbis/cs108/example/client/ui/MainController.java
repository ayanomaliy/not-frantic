package ch.unibas.dmi.dbis.cs108.example.client.ui;

import animatefx.animation.FadeIn;
import animatefx.animation.FadeInUp;
import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.client.net.FxNetworkClient;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import ch.unibas.dmi.dbis.cs108.example.client.CardTextFormatter;

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

    private ListChangeListener<String> handCardsListener;

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
        this.networkClient.setGameEndListener(() -> {
            showWinnerView();
        });

        this.networkClient.setGameStartListener(() -> {
            if (stage.getScene() == null || !(stage.getScene().getRoot() instanceof GameView)) {
                showGameView();
            }
        });
    }

    /**
     * Shows the connect screen with default values.
     */
    public void showConnectView() {
        showConnectView("localhost", "5555", System.getProperty("user.name", "Player"));
    }

    /**
     * Shows the connect screen with prefilled values.
     *
     * @param initialHost the prefilled host
     * @param initialPort the prefilled port
     * @param initialUsername the prefilled username
     */
    public void showConnectView(String initialHost, String initialPort, String initialUsername) {
        ConnectView view = new ConnectView(initialHost, initialPort, initialUsername);
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

        installSelfHighlight(view.getLobbyPlayersList());
        installSelfHighlight(view.getAllPlayersList());
        installCurrentLobbyHighlight(view.getLobbiesList());

        state.currentLobbyProperty().addListener((obs, oldValue, newValue) ->
                view.getLobbiesList().refresh()
        );

        view.getLeaveLobbyButton().managedProperty().bind(
                view.getLeaveLobbyButton().visibleProperty()
        );
        view.getLeaveLobbyButton().visibleProperty().bind(
                Bindings.isNotEmpty(state.getPlayers())
        );

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

        // Join Lobby button: the displayed entry contains status and player-count
        // metadata (e.g. "LobbyName (WAITING) 2/5"), so the pure lobby name must
        // be extracted before sending the join request to the server.
        view.getJoinLobbyButton().setOnAction(e -> {
            String selectedLobby = view.getLobbiesList().getSelectionModel().getSelectedItem();

            if (selectedLobby != null && !selectedLobby.isBlank()) {
                networkClient.joinLobby(extractLobbyName(selectedLobby));
            }
        });

        // Double-clicking a lobby entry also triggers a join, using the same
        // lobby-name extraction logic as the Join button.
        view.getLobbiesList().setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedLobby = view.getLobbiesList().getSelectionModel().getSelectedItem();

                if (selectedLobby != null && !selectedLobby.isBlank()) {
                    networkClient.joinLobby(extractLobbyName(selectedLobby));
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
                    networkClient.createLobby(trimmed);
                }
            });
        });

        view.getLeaveLobbyButton().setOnAction(e -> leaveCurrentLobbyAndShowLobbyView());

        view.getStartButton().setOnAction(e -> networkClient.startGame());

        view.getDisconnectButton().setOnAction(e -> {
            networkClient.disconnect();
            showConnectView();
        });

        Scene scene = createStyledScene(view, 1280, 800);
        stage.setScene(scene);
        new FadeIn(view).play();
    }

    /**
     * Shows the game screen.
     *
     * <p>This view is driven by server-backed GUI state. It no longer creates a
     * fake local game or fake demo hand.</p>
     */
    public void showGameView() {
        GameView view = new GameView();

        view.getPlayersList().setItems(state.getPlayers());
        installSelfHighlight(view.getPlayersList());
        view.getGameInfoList().setItems(state.getGameMessages());

        view.getCurrentPlayerLabel().textProperty().bind(
                Bindings.concat("Current Player: ", state.currentPlayerProperty())
        );
        view.getPhaseLabel().textProperty().bind(
                Bindings.concat("Phase: ", state.currentPhaseProperty())
        );
        view.getDiscardTopLabel().textProperty().bind(
                Bindings.concat("Top Card: ", state.topCardTextProperty())
        );

        if (handCardsListener != null) {
            state.getCurrentHandCards().removeListener(handCardsListener);
        }

        handCardsListener = change -> renderHand(view);
        state.getCurrentHandCards().addListener(handCardsListener);
        renderHand(view);

        updateDisplayedChat(view);

        view.getChatModeButton().textProperty().bind(state.chatModeProperty());

        view.getChatModeButton().setOnAction(e -> {
            switch (state.getChatMode()) {
                case "Global" -> state.setChatMode("Lobby");
                case "Lobby" -> state.setChatMode("Whisper");
                default -> state.setChatMode("Global");
            }

            updateDisplayedChat(view);
        });

        view.getSendButton().setOnAction(e -> sendChat(view));
        view.getChatInput().setOnAction(e -> sendChat(view));

        view.getCommandButton().setOnAction(e -> sendCommand(view));
        view.getCommandInput().setOnAction(e -> sendCommand(view));

        view.getDrawButton().setOnAction(e -> networkClient.drawCard());
        view.getEndTurnButton().setOnAction(e -> networkClient.endTurn());

        view.getLeaveButton().setOnAction(e -> leaveCurrentLobbyAndShowLobbyView());

        Scene scene = createStyledScene(view, 1280, 800);
        stage.setScene(scene);
        new FadeIn(view).play();

        networkClient.requestHand();
    }

    /**
     * Leaves the current lobby using the structured protocol and returns to the lobby view.
     *
     * <p>No extra refresh requests are sent here because the server already
     * pushes the relevant list updates.</p>
     */
    private void leaveCurrentLobbyAndShowLobbyView() {
        networkClient.leaveLobby();
        showLobbyView();
    }

    /**
     * Renders the current hand from {@link ClientState#getCurrentHandCards()}.
     *
     * @param view the game view whose hand area should be refreshed
     */
    private void renderHand(GameView view) {
        view.getPlayerHandPane().getChildren().clear();

        for (String cardIdText : state.getCurrentHandCards()) {
            int cardId;
            try {
                cardId = Integer.parseInt(cardIdText);
            } catch (NumberFormatException e) {
                continue;
            }

            Button cardButton = new Button(CardTextFormatter.formatCardLabelWithId(cardId));
            cardButton.getStyleClass().add("game-card-button");
            cardButton.setPrefSize(90, 130);
            cardButton.setMinSize(90, 130);
            cardButton.setMaxSize(90, 130);
            cardButton.setFocusTraversable(false);
            cardButton.setWrapText(true);
            cardButton.setOnAction(e -> networkClient.playCard(cardId));

            view.getPlayerHandPane().getChildren().add(cardButton);
        }
    }

    /**
     * Updates the chat list view to display the message list of the currently selected chat mode.
     *
     * @param view the lobby view containing the chat list
     */
    private void updateDisplayedChat(LobbyView view) {
        switch (state.getChatMode()) {
            case "Lobby" -> {
                view.getChatList().setItems(state.getLobbyChatMessages());
                view.getChatInput().setPromptText("Type a lobby message...");
            }
            case "Whisper" -> {
                view.getChatList().setItems(state.getWhisperChatMessages());
                view.getChatInput().setPromptText("player: message");
            }
            default -> {
                view.getChatList().setItems(state.getGlobalChatMessages());
                view.getChatInput().setPromptText("Type a global message...");
            }
        }
    }

    /**
     * Updates the game-view chat list to display the message list of the
     * currently selected chat mode.
     *
     * @param view the game view containing the chat list
     */
    private void updateDisplayedChat(GameView view) {
        switch (state.getChatMode()) {
            case "Lobby" -> {
                view.getChatList().setItems(state.getLobbyChatMessages());
                view.getChatInput().setPromptText("Type a lobby message...");
            }
            case "Whisper" -> {
                view.getChatList().setItems(state.getWhisperChatMessages());
                view.getChatInput().setPromptText("player: message");
            }
            default -> {
                view.getChatList().setItems(state.getGlobalChatMessages());
                view.getChatInput().setPromptText("Type a global message...");
            }
        }
    }

    /**
     * Sends the content of the chat input field using the currently selected chat mode.
     *
     * @param view the lobby view containing the chat input
     */
    private void sendChat(LobbyView view) {
        String text = view.getChatInput().getText().trim();
        if (text.isBlank()) {
            return;
        }

        if ("Whisper".equals(state.getChatMode())) {
            sendWhisperMessage(text);
        } else if ("Lobby".equals(state.getChatMode())) {
            networkClient.sendLobbyChat(text);
        } else {
            networkClient.sendGlobalChat(text);
        }

        view.getChatInput().clear();
    }

    /**
     * Sends the content of the chat input field using the currently selected
     * chat mode from the game view.
     *
     * @param view the game view containing the chat input
     */
    private void sendChat(GameView view) {
        String text = view.getChatInput().getText().trim();
        if (text.isBlank()) {
            return;
        }

        if ("Whisper".equals(state.getChatMode())) {
            sendWhisperMessage(text);
        } else if ("Lobby".equals(state.getChatMode())) {
            networkClient.sendLobbyChat(text);
        } else {
            networkClient.sendGlobalChat(text);
        }

        view.getChatInput().clear();
    }

    /**
     * Sends a whisper message using structured protocol sending instead of slash commands.
     *
     * <p>The GUI whisper format is {@code player: message}.</p>
     *
     * @param rawText the raw text entered in whisper mode
     */
    private void sendWhisperMessage(String rawText) {
        String[] parts = rawText.split(":", 2);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            state.getGameMessages().add("[CLIENT] Whisper format: player: message");
            return;
        }

        String target = parts[0].trim();
        String message = parts[1].trim();
        networkClient.sendWhisperChat(target, message);
    }

    /**
     * Executes the content of the command field as a terminal-style client command.
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
     * Executes the content of the command field as a terminal-style client
     * command from the game view.
     *
     * @param view the game view containing the command input
     */
    private void sendCommand(GameView view) {
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
     * Creates a scene and attaches the shared CSS theme if the resource is available.
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

    /**
     * Installs a custom cell factory that highlights the current client's own name
     * using the same background color as the hover state.
     *
     * @param listView the player list view to decorate
     */
    private void installSelfHighlight(ListView<String> listView) {
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().remove("self-player-cell");

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(item);

                if (item.equals(state.getUsername())) {
                    getStyleClass().add("self-player-cell");
                }
            }
        });
    }

    /**
     * Installs a custom cell factory that highlights the lobby the client is
     * currently in using the hover background color.
     *
     * <p>The displayed lobby entries contain additional metadata such as status
     * and player count, for example {@code FunRoom (WAITING) 2/5}. Therefore,
     * the pure lobby name must be extracted before comparing it with the
     * currently joined lobby stored in the shared client state.</p>
     *
     * @param listView the lobby list view to decorate
     */
    private void installCurrentLobbyHighlight(ListView<String> listView) {
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().remove("current-lobby-cell");

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(item);

                String currentLobby = state.getCurrentLobby();
                if (!currentLobby.isBlank() && extractLobbyName(item).equals(currentLobby)) {
                    getStyleClass().add("current-lobby-cell");
                }
            }
        });
    }

    /**
     * Extracts the pure lobby name from a formatted lobby list entry.
     *
     * <p>The lobby list in the GUI shows entries in a human-readable format such as
     * {@code LobbyName (WAITING) 2/5}. This method removes the appended status and
     * player-count section and returns only the original lobby name.</p>
     *
     * <p>If the expected {@code " ("} separator is not found, the full trimmed
     * string is returned unchanged.</p>
     *
     * @param displayedLobby the formatted lobby entry shown in the GUI
     * @return the extracted pure lobby name
     */
    private String extractLobbyName(String displayedLobby) {
        int statusStart = displayedLobby.indexOf(" (");
        if (statusStart >= 0) {
            return displayedLobby.substring(0, statusStart).trim();
        }
        return displayedLobby.trim();
    }


    public void showWinnerView() {
        GameEndView view = new GameEndView();

        view.setWinner(state.getWinnerName());
        view.setRanking(state.getFinalScoreRows());

        view.getLeaveLobbyButton().setOnAction(e -> leaveCurrentLobbyAndShowLobbyView());

        Scene scene = createStyledScene(view, 900, 700);
        stage.setScene(scene);
        new FadeIn(view).play();
    }
}