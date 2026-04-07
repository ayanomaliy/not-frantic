package ch.unibas.dmi.dbis.cs108.example.client.ui;

import animatefx.animation.FadeIn;
import animatefx.animation.FadeInUp;
import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.client.net.FxNetworkClient;
import ch.unibas.dmi.dbis.cs108.example.model.game.Card;
import ch.unibas.dmi.dbis.cs108.example.model.game.GameInitializer;
import ch.unibas.dmi.dbis.cs108.example.model.game.GameState;
import ch.unibas.dmi.dbis.cs108.example.model.game.TurnEngine;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.Random;

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

    private GameState localGameState;

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

        this.networkClient.setGameStartListener(() -> {
            if (!(stage.getScene().getRoot() instanceof GameView)) {
                showGameView();
            }
        });
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

        view.getStartButton().setOnAction(e -> networkClient.startGame());

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

    /**
     * Shows the game screen.
     */
    public void showGameView() {
        GameView view = new GameView();

        localGameState = GameInitializer.initialize(
                List.of("User 1", "User 2"),
                1,
                Map.of(),
                new Random()
        );

        TurnEngine.startTurn(localGameState);

        state.setCurrentPlayer(localGameState.getCurrentPlayer().getPlayerName());
        state.setCurrentPhase(localGameState.getPhase().name());

        var topCard = localGameState.peekDiscardPile();
        state.setTopCardText(cardToText(topCard));

        refreshGameLabels(view);

        view.getPlayersList().setItems(state.getPlayers());
        view.getGameInfoList().setItems(state.getGameMessages());
        view.getChatList().setItems(state.getGlobalChatMessages());

        view.getDrawButton().setOnAction(e -> {
            int nextCardNumber = view.getPlayerHandPane().getChildren().size() + 1;

            Button newCard = new Button("RED " + nextCardNumber);
            newCard.setPrefSize(80, 120);
            newCard.setFocusTraversable(false);

            newCard.setOnAction(cardEvent -> {
                state.setTopCardText("RED " + nextCardNumber);
                state.getGameMessages().add("[CLIENT] Played card: RED " + nextCardNumber);

                refreshGameLabels(view);
                view.getPlayerHandPane().getChildren().remove(newCard);
                view.getGameInfoList().scrollTo(state.getGameMessages().size() - 1);
            });

            view.getPlayerHandPane().getChildren().add(newCard);
            state.getGameMessages().add("[CLIENT] Drew card: RED " + nextCardNumber);
            view.getGameInfoList().scrollTo(state.getGameMessages().size() - 1);
        });

        view.getEndTurnButton().setOnAction(e -> {
            state.getGameMessages().add("[CLIENT] End turn clicked.");
            state.setCurrentPlayer("Next Player");

            refreshGameLabels(view);
            view.getGameInfoList().scrollTo(state.getGameMessages().size() - 1);
        });

        view.getBackToLobbyButton().setOnAction(e -> showLobbyView());
        view.getLeaveButton().setOnAction(e -> showLobbyView());

        view.getSendButton().setOnAction(e -> {
            String text = view.getChatInput().getText().trim();
            if (!text.isBlank()) {
                networkClient.sendGlobalChat(text);
                view.getChatInput().clear();
            }
        });

        Scene scene = createStyledScene(view, 1280, 800);
        stage.setScene(scene);
        new FadeIn(view).play();

        // Initial fake hand
        for (int i = 1; i <= 7; i++) {
            final int cardNumber = i;

            Button cardBtn = new Button("RED " + cardNumber);
            cardBtn.setPrefSize(80, 120);
            cardBtn.setFocusTraversable(false);

            cardBtn.setOnAction(e -> {
                state.setTopCardText("RED " + cardNumber);
                state.getGameMessages().add("[CLIENT] Played card: RED " + cardNumber);

                refreshGameLabels(view);
                view.getPlayerHandPane().getChildren().remove(cardBtn);
                view.getGameInfoList().scrollTo(state.getGameMessages().size() - 1);
            });

            view.getPlayerHandPane().getChildren().add(cardBtn);
        }
    }

    /**
     * Refreshes the game labels from the shared client state.
     *
     * @param view the game view to update
     */
    private void refreshGameLabels(GameView view) {
        view.getCurrentPlayerLabel().setText("Current Player: " + state.getCurrentPlayer());
        view.getPhaseLabel().setText("Phase: " + state.getCurrentPhase());
        view.getDiscardTopLabel().setText("Top Card: " + state.getTopCardText());
    }

    private String cardToText(Card card) {
        if (card == null) {
            return "-";
        }

        return switch (card.type()) {
            case COLOR -> card.color() + " " + card.value();
            case BLACK -> "BLACK " + card.value();
            case SPECIAL_SINGLE -> card.color() + " " + card.effect();
            case SPECIAL_FOUR -> String.valueOf(card.effect());
            case FUCK_YOU -> "FUCK YOU";
            case EVENT -> "EVENT " + card.id();
        };
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
}