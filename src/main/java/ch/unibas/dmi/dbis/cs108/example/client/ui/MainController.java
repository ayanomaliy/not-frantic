package ch.unibas.dmi.dbis.cs108.example.client.ui;

import animatefx.animation.FadeIn;
import animatefx.animation.FadeInUp;
import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.client.net.FxNetworkClient;
import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import ch.unibas.dmi.dbis.cs108.example.client.assets.SoundManager;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

import java.util.ArrayDeque;
import java.util.Queue;


/**
 * Coordinates screen changes and user interaction in the Frantic^-1 JavaFX client.
 *
 * <p>This controller connects UI views to the shared {@link ClientState}, delegates user
 * actions to {@link FxNetworkClient}, and applies the shared theme and transition effects
 * when switching screens.</p>
 */
public class MainController {

    private static final String THEME_STYLESHEET = "/css/frantic-theme.css";

    private static final double FAN_SPREAD_DEGREES = 60.0;
    private static final double FAN_ARC_RADIUS    = 500.0;

    private final Stage stage;
    private final ClientState state;
    private final FxNetworkClient networkClient;
    private final AssetRegistry registry;
    private final SoundManager soundManager;

    private ListChangeListener<String> handCardsListener;
    private ListChangeListener<ClientState.PlayerInfo> playerInfoListener;
    private boolean peekActive = false;

    private boolean drawAnimationPending = false;
    private boolean drawAnimationRunning = false;
    private int handSizeBeforeDrawAnimation = -1;
    private String pendingOpponentDrawPlayer = null;
    private boolean opponentDrawAnimationRunning = false;

    private boolean playAnimationRunning = false;
    private boolean hiddenTransferAnimationRunning = false;

    private final Queue<String> opponentDrawAnimationQueue = new ArrayDeque<>();
    private final Queue<CardTransferAnimation> hiddenTransferAnimationQueue = new ArrayDeque<>();

    private static final Duration PLAY_ANIMATION_DURATION = Duration.millis(430);
    private static final Duration HIDDEN_TRANSFER_ANIMATION_DURATION = Duration.millis(430);

    private record CardTransferAnimation(String sourcePlayer, String targetPlayer, int count) {}

    private final Map<String, Integer> knownPublicHandSizes = new HashMap<>();

    private static final Duration DRAW_ANIMATION_DURATION = Duration.millis(430);


    private static final double HAND_VERTICAL_LIFT = 40.0;

    /**
     * Creates a new main controller using assets loaded from the classpath.
     *
     * @param stage the primary stage
     * @param state the shared client state
     * @param networkClient the network client used for server communication
     */
    public MainController(Stage stage, ClientState state, FxNetworkClient networkClient) {
        this(stage, state, networkClient, AssetRegistry.load());
    }

    /**
     * Creates a new main controller with a specific asset registry.
     *
     * @param stage the primary stage
     * @param state the shared client state
     * @param networkClient the network client used for server communication
     * @param registry the asset registry to use for icons, colors, and sounds
     */
    public MainController(Stage stage, ClientState state, FxNetworkClient networkClient, AssetRegistry registry) {
        this.stage = stage;
        this.state = state;
        this.networkClient = networkClient;
        this.registry = registry;
        this.soundManager = new SoundManager(registry);
        this.soundManager.playBackgroundMusic("background_music");

        this.networkClient.setGameEndListener(() -> {
            registry.getSoundId("GAME_ENDED").ifPresent(soundManager::play);
            showWinnerView();
        });

        this.networkClient.setGameStartListener(() -> {
            registry.getSoundId("GAME_STARTED").ifPresent(soundManager::play);
            if (stage.getScene() == null || !(stage.getScene().getRoot() instanceof GameView)) {
                showGameView();
            }
        });

        this.networkClient.setRoundEndListener(() -> {
            if (stage.getScene().getRoot() instanceof GameView gv) {
                gv.showRoundResults(state.getFinalScoreRows(),
                        networkClient::sendStartNextRound,
                        this::leaveCurrentLobbyAndShowLobbyView);
            }
        });

        this.networkClient.setNextRoundListener(() -> {
            if (stage.getScene().getRoot() instanceof GameView gv) {
                gv.hideRoundResults();
            }
        });
    }

    /** Shows the connect screen using the default host, port, and username values. */
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

    /** Shows the lobby screen and wires it to the current shared client state. */
    public void showLobbyView() {
        LobbyView view = new LobbyView();

        StackPane rootStack = new StackPane(view);

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
                case "Whisper" -> state.setChatMode("Game Info");
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

        view.getNameButton().setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(state.getUsername());
            dialog.setTitle("Change Name");
            dialog.setHeaderText("Choose a new player name");
            dialog.setContentText("New name");

            dialog.showAndWait().ifPresent(name -> {
                String trimmed = name.trim();

                if (!trimmed.isBlank()) {
                    networkClient.setName(trimmed);
                }
            });

        });


        view.getStartButton().setOnAction(e -> networkClient.startGame());

        view.getDisconnectButton().setOnAction(e -> {
            networkClient.disconnect();
            showConnectView();
        });

        view.getSettingsButton().setOnAction(e -> showSettingsOverlay(rootStack));

        Scene scene = createStyledScene(rootStack, 1280, 800);
        stage.setScene(scene);
        new FadeIn(view).play();
    }

    /**
     * Shows the game screen and binds it to the current game-related client state.
     *
     * <p>The rendered content is entirely driven by server-backed state rather than local
     * placeholder data.</p>
     */
    public void showGameView() {
        GameView view = new GameView();

        view.setInitialMusicVolume(soundManager.getMusicVolume());
        view.setMusicVolumeHandler(soundManager::setMusicVolume);

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

        handCardsListener = change -> {
            if (drawAnimationPending) {
                int newHandSize = state.getCurrentHandCards().size();

                if (newHandSize > handSizeBeforeDrawAnimation) {
                    drawAnimationPending = false;
                    playDrawCardAnimationThenRender(view);
                    return;
                }

                drawAnimationPending = false;
            }

            if (!drawAnimationRunning && !playAnimationRunning && !hiddenTransferAnimationRunning) {
                renderHand(view);
            }
        };

        state.getCurrentHandCards().addListener(handCardsListener);
        renderHand(view);

        view.getHandFanPane().widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0
                    && !drawAnimationRunning
                    && !playAnimationRunning
                    && !hiddenTransferAnimationRunning) {
                renderHand(view);
            }
        });

        view.getCircularTablePane().setRegistry(registry);

        if (playerInfoListener != null) {
            state.getPlayerInfoList().removeListener(playerInfoListener);
        }
        playerInfoListener = change -> {
            peekActive = false;

            if (playAnimationRunning || hiddenTransferAnimationRunning || opponentDrawAnimationRunning) {
                return;
            }

            if (shouldAnimateOpponentDraw()) {
                String targetPlayer = pendingOpponentDrawPlayer;
                pendingOpponentDrawPlayer = null;

                enqueueOpponentDrawAnimation(view, targetPlayer);
                return;
            }

            refreshOtherPlayers(view);
        };

        state.getPlayerInfoList().addListener(playerInfoListener);
        refreshOtherPlayers(view);
        updateCurrentTurnHighlight(view);

        state.topCardIdProperty().addListener((obs, oldValue, newValue) -> {
            if (!playAnimationRunning) {
                renderDiscardPile(view);
            }
        });

        state.requestedColorProperty().addListener((obs, oldValue, newValue) -> {
            if (!playAnimationRunning) {
                renderDiscardPile(view);
            }
        });

        state.requestedNumberProperty().addListener((obs, oldValue, newValue) -> {
            if (!playAnimationRunning) {
                renderDiscardPile(view);
            }
        });

        renderDiscardPile(view);

        state.drawPileSizeProperty().addListener((obs, oldValue, newValue) ->
                view.renderDrawPile(newValue.intValue())
        );
        view.renderDrawPile(state.getDrawPileSize());

        updateDisplayedChat(view);

        view.getChatModeButton().textProperty().bind(state.chatModeProperty());

        view.getChatModeButton().setOnAction(e -> {
            switch (state.getChatMode()) {
                case "Global" -> state.setChatMode("Lobby");
                case "Lobby" -> state.setChatMode("Whisper");
                case "Whisper" -> state.setChatMode("Game Info");
                default -> state.setChatMode("Global");
            }

            updateDisplayedChat(view);
        });

        view.getSendButton().setOnAction(e -> sendGameViewInput(view));
        view.getChatInput().setOnAction(e -> sendGameViewInput(view));


        view.getCommandButton().setOnAction(e -> sendCommand(view));
        view.getCommandInput().setOnAction(e -> sendCommand(view));
        view.getDrawPilePane().setOnMouseClicked(e -> {
            requestDrawWithAnimation(view);
        });

        view.getEndTurnButton().setOnAction(e -> networkClient.endTurn());
        view.getLeaveButton().setOnAction(e -> leaveCurrentLobbyAndShowLobbyView());

        networkClient.setEffectRequestListener(effectPayload -> {
            if (effectPayload == null || effectPayload.isBlank()) {
                return;
            }

            String payload = effectPayload.trim();

            if (isEffectRequestForMe(payload, "FANTASTIC")) {
                showFantasticEffectDialog(view);
                return;
            }

            if (isEffectRequestForMe(payload, "EQUALITY")) {
                showEqualityEffectDialog(view);
                return;
            }

            if (isEffectRequestForMe(payload, "FANTASTIC_FOUR")) {
                showFantasticFourEffectDialog(view);
                return;
            }

            if (isEffectRequestForMe(payload, "GIFT")) {
                showGiftEffectDialog(view);
                return;
            }

            if (isEffectRequestForMe(payload, "SECOND_CHANCE")) {
                state.setPendingEffectRequest(payload);
                state.getGameMessages().add(
                        "[INFO] Second Chance: click a card to play it, or click draw pile to draw."
                );
                return;
            }

            if (isEffectRequestForMe(payload, "EXCHANGE")) {
                showExchangeEffectDialog(view);
                return;
            }

            if (isEffectRequestForMe(payload, "SKIP")) {
                showSkipEffectDialog(view);
            }

            if (isEffectRequestForMe(payload, "NICE_TRY")) {
                showNiceTryEffectDialog(view);
                return;
            }

            if (isEffectRequestForMe(payload, "COUNTERATTACK")) {
                showCounterattackEffectDialog(view);
                return;
            }
        });

        networkClient.setCardDrawnListener(drawingPlayer -> {
            if (drawingPlayer == null || drawingPlayer.isBlank()) {
                return;
            }

            if (drawingPlayer.equals(state.getUsername())) {
                /*
                 * If I clicked the draw pile myself, requestDrawWithAnimation(...)
                 * already set drawAnimationPending. Do not set it twice.
                 *
                 * If I receive a card from Fantastic Four, Nice Try, event effects, etc.,
                 * this public CARD_DRAWN event prepares the same local draw animation.
                 */
                if (!drawAnimationPending) {
                    drawAnimationPending = true;
                    handSizeBeforeDrawAnimation = state.getCurrentHandCards().size();
                }
                return;
            }

            pendingOpponentDrawPlayer = drawingPlayer;
        });

        networkClient.setCardPlayedListener((playingPlayer, playedCardId) -> {
            if (playingPlayer == null || playingPlayer.isBlank()) {
                return;
            }

            playCardToDiscardAnimationThenRefresh(view, playingPlayer, playedCardId);
        });

        networkClient.setCardTransferListener((sourcePlayer, targetPlayer, count) -> {
            enqueueHiddenTransferAnimation(view, sourcePlayer, targetPlayer, count);
        });

        networkClient.setEventCardFlippedListener(eventCardId -> {
            EventBannerData data = describeEventCard(eventCardId);
            view.playEventOverlay(data.title(), data.description());
        });


        Scene scene = createStyledScene(view, 1280, 800);
        stage.setScene(scene);
        new FadeIn(view).play();

        state.currentPlayerProperty().addListener((obs, oldValue, newValue) -> {
            updateCurrentTurnHighlight(view);

            if (newValue == null) {
                return;
            }

            boolean isNowMyTurn = isCurrentTurnForMe(newValue);
            boolean wasMyTurnBefore = isCurrentTurnForMe(oldValue);

            if (isNowMyTurn && !wasMyTurnBefore) {
                view.playTurnOverlay();
            }
        });

        if (isCurrentTurnForMe(state.getCurrentPlayer())) {
            view.playTurnOverlay();
        }

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

    private boolean isEffectRequestForMe(String payload, String effectName) {
        if (payload == null || payload.isBlank()) {
            return false;
        }

        String username = state.getUsername();
        if (username == null || username.isBlank()) {
            return false;
        }

        String prefix = effectName + ":";
        if (!payload.toUpperCase().startsWith(prefix)) {
            return false;
        }

        String targetPlayer = payload.substring(prefix.length()).trim();
        return targetPlayer.equals(username);
    }

    private boolean isColorlessSpecialCard(int cardId) {
        return cardId >= 101 && cardId <= 124;
    }

    private boolean isCurrentTurnForMe(String currentPlayerName) {
        String username = state.getUsername();
        return username != null
                && !username.isBlank()
                && currentPlayerName != null
                && currentPlayerName.equals(username);
    }

    /**
     * Renders the current hand from {@link ClientState#getCurrentHandCards()} into the
     * fan-arc pane. Cards are positioned on a circular arc so the hand curves upward from
     * the bottom center of the pane.
     *
     * @param view the game view whose hand fan pane should be refreshed
     */
    private void renderHand(GameView view) {
        javafx.scene.layout.Pane fanPane = view.getHandFanPane();
        fanPane.getChildren().clear();

        int n = state.getCurrentHandCards().size();
        if (n == 0) return;

        double paneW = fanPane.getWidth() > 0 ? fanPane.getWidth() : fanPane.getPrefWidth();
        double paneH = fanPane.getHeight() > 0 ? fanPane.getHeight() : fanPane.getPrefHeight();
        double cx = paneW / 2;

        for (int i = 0; i < n; i++) {
            String cardIdText = state.getCurrentHandCards().get(i);
            int cardId;
            try {
                cardId = Integer.parseInt(cardIdText);
            } catch (NumberFormatException e) {
                continue;
            }

            double angle = (n > 1)
                    ? -FAN_SPREAD_DEGREES / 2 + i * (FAN_SPREAD_DEGREES / (n - 1))
                    : 0.0;

            double rad = Math.toRadians(angle);
            double x = cx + FAN_ARC_RADIUS * Math.sin(rad);
            double y = paneH - HAND_VERTICAL_LIFT + FAN_ARC_RADIUS * (1 - Math.cos(rad));

            final int cid = cardId;
            CardView cardView = new CardView(cid, registry, () -> {
                registry.getSoundId(CardView.lookupCard(cid)).ifPresent(soundManager::play);
                if (isSecondChanceActiveForMe()) {
                    networkClient.resolveSecondChance(cid);
                    clearPendingEffectIfSecondChance();
                } else {
                    if (!canLocalPlayerPlayNow()) {
                        state.getGameMessages().add("[CLIENT] You cannot play right now.");
                        return;
                    }

                    networkClient.playCard(cid);
                }
            });

            cardView.setUserData(cid);
            cardView.setRotate(angle);
            cardView.setLayoutX(x - CardBacksideView.CARD_WIDTH / 2);
            cardView.setLayoutY(y - CardBacksideView.CARD_HEIGHT);

            fanPane.getChildren().add(cardView);
        }
    }

    /**
     * Pushes the current {@link ClientState#getPlayerInfoList()} to the circular table,
     * excluding the local player so only opponents are shown.
     *
     * @param view the game view whose circular table should be refreshed
     */
    private void refreshOtherPlayers(GameView view) {
        String username = state.getUsername();
        java.util.List<ClientState.PlayerInfo> others = state.getPlayerInfoList().stream()
                .filter(p -> !p.name().equals(username))
                .toList();

        view.getCircularTablePane().setPlayerSlots(others, username);
        updateCurrentTurnHighlight(view);

        knownPublicHandSizes.clear();
        for (ClientState.PlayerInfo info : state.getPlayerInfoList()) {
            knownPublicHandSizes.put(info.name(), info.handSize());
        }
    }
    /**
     * Updates the lobby chat area to show the message list for the active chat mode.
     *
     * @param view the lobby view whose chat controls should be updated
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
     * Updates the game-view chat area to show the selected mode.
     *
     * <p>In Game Info mode, the same panel acts as the game log and the input
     * field becomes the command input.</p>
     *
     * @param view the game view whose chat controls should be updated
     */
    private void updateDisplayedChat(GameView view) {
        switch (state.getChatMode()) {
            case "Lobby" -> {
                view.getChatList().setItems(state.getLobbyChatMessages());
                view.getChatInput().setPromptText("Type a lobby message...");
                view.getSendButton().setText("Send");
            }
            case "Whisper" -> {
                view.getChatList().setItems(state.getWhisperChatMessages());
                view.getChatInput().setPromptText("player: message");
                view.getSendButton().setText("Send");
            }
            case "Game Info" -> {
                view.getChatList().setItems(state.getGameMessages());
                view.getChatInput().setPromptText("/hand, /gamestate, /dev default, /cheatwin, /peek...");
                view.getSendButton().setText("Run");
            }
            default -> {
                view.getChatList().setItems(state.getGlobalChatMessages());
                view.getChatInput().setPromptText("Type a global message...");
                view.getSendButton().setText("Send");
            }
        }
    }

    /**
     * Sends the current lobby-view chat input using the selected chat mode.
     *
     * @param view the lobby view containing the chat input field
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
     * Sends either a chat message or a command from the unified game-view input.
     *
     * <p>Global, Lobby, and Whisper modes behave like normal chat.
     * Game Info mode uses the same input field as the command line.</p>
     *
     * @param view the game view containing the unified input field
     */
    private void sendGameViewInput(GameView view) {
        if ("Game Info".equals(state.getChatMode())) {
            sendCommandFromChatInput(view);
        } else {
            sendChat(view);
        }
    }

    /**
     * Executes a command from the game-view chat input while Game Info mode is active.
     *
     * <p>This replaces the old visible command input in the sidebar. The old command
     * input still exists for compatibility, but the user now types commands into the
     * unified input field.</p>
     *
     * @param view the game view containing the unified input field
     */
    private void sendCommandFromChatInput(GameView view) {
        String command = view.getChatInput().getText().trim();
        if (command.isBlank()) {
            return;
        }

        if ("/peek".equalsIgnoreCase(command)) {
            togglePeek(view);
            view.getChatInput().clear();
            return;
        }

        boolean disconnected = networkClient.sendCommand(command);
        view.getChatInput().clear();

        if (disconnected) {
            showConnectView();
        }
    }

    /**
     * Sends the current game-view chat input using the selected chat mode.
     *
     * @param view the game view containing the chat input field
     */
    private void sendChat(GameView view) {
        String text = view.getChatInput().getText().trim();
        if (text.isBlank()) {
            return;
        }

        if ("Game Info".equals(state.getChatMode())) {
            sendCommandFromChatInput(view);
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
     * Executes the lobby-view command input as a client command.
     *
     * @param view the lobby view containing the command input field
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
     * Executes the game-view command input as a client command.
     *
     * <p>{@code /peek} is handled locally and never forwarded to the server.</p>
     *
     * @param view the game view containing the command input field
     */
    private void sendCommand(GameView view) {
        String command = view.getCommandInput().getText().trim();
        if (command.isBlank()) {
            return;
        }

        if ("/peek".equalsIgnoreCase(command)) {
            togglePeek(view);
            view.getCommandInput().clear();
            return;
        }

        boolean disconnected = networkClient.sendCommand(command);
        view.getCommandInput().clear();

        if (disconnected) {
            showConnectView();
        }
    }

    /**
     * Toggles the PEEK mode: reveals opponent cards on first call, hides them on the next.
     * Placeholder sequential card IDs (0…handSize-1) are used because the server does not
     * yet send opponent card IDs to other clients.
     */
    private void togglePeek(GameView view) {
        peekActive = !peekActive;
        CircularTablePane table = view.getCircularTablePane();
        if (peekActive) {
            String username = state.getUsername();
            for (ClientState.PlayerInfo info : state.getPlayerInfoList()) {
                if (info.name().equals(username)) continue;
                OtherPlayerView slot = table.getPlayerSlots().get(info.name());
                if (slot == null) continue;
                java.util.List<Integer> ids = new java.util.ArrayList<>();
                for (int i = 0; i < info.handSize(); i++) ids.add(i);
                slot.revealCards(ids);
            }
            state.getGameMessages().add("[CLIENT] PEEK on — showing placeholder cards.");
        } else {
            table.getPlayerSlots().values().forEach(OtherPlayerView::hideCards);
            state.getGameMessages().add("[CLIENT] PEEK off.");
        }
    }

    /**
     * Installs keyboard shortcuts for hidden developer actions.
     *
     * @param scene the active scene
     */
    private void installKeyboardShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()
                    && event.isShiftDown()
                    && event.getCode() == KeyCode.W) {

                networkClient.cheatWin();
                event.consume();
            }

            if (event.isControlDown()
                    && event.isShiftDown()
                    && event.getCode() == KeyCode.D) {

                networkClient.simulateNetworkLossForTesting();
                event.consume();
            }
        });
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

        installKeyboardShortcuts(scene);

        var stylesheet = getClass().getResource(THEME_STYLESHEET);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        applySceneFillFromRootBackground(scene, root);

        return scene;
    }

    private void applySceneFillFromRootBackground(Scene scene, Parent root) {
        root.applyCss();
        root.layout();

        if (root instanceof Region region
                && region.getBackground() != null
                && !region.getBackground().getFills().isEmpty()
                && region.getBackground().getFills().get(0).getFill() instanceof Color color) {
            scene.setFill(color);
        } else {
            scene.setFill(Paint.valueOf("#26201e"));
        }
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

    /**
     * Shows the game-end screen with the winner and final ranking from the shared state.
     */
    public void showWinnerView() {
        GameEndView view = new GameEndView();

        view.setWinner(state.getWinnerName());
        view.setRanking(state.getFinalScoreRows());

        ch.unibas.dmi.dbis.cs108.example.service.HighScoreHistory highScoreHistory =
                new ch.unibas.dmi.dbis.cs108.example.service.HighScoreHistory(java.nio.file.Path.of("highscores.txt"));
        view.setLeaderboard(highScoreHistory.buildLeaderboardDisplayRows());

        view.getLeaveLobbyButton().setOnAction(e -> leaveCurrentLobbyAndShowLobbyView());

        Scene scene = createStyledScene(view, 1000, 700);
        stage.setScene(scene);
        new FadeIn(view).play();
    }

    private void renderDiscardPile(GameView view) {
        view.getDiscardPilePane().getChildren().clear();

        String requestedColor = state.getRequestedColor();
        if (requestedColor != null && !requestedColor.isBlank()) {
            try {
                WishCardView wishCardView = WishCardView.forColorWish(
                        ch.unibas.dmi.dbis.cs108.example.model.game.CardColor.valueOf(requestedColor),
                        registry
                );
                view.getDiscardPilePane().getChildren().add(wishCardView);
                return;
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid color values.
            }
        }

        String requestedNumber = state.getRequestedNumber();
        if (requestedNumber != null && !requestedNumber.isBlank()) {
            try {
                int number = Integer.parseInt(requestedNumber);
                WishCardView wishCardView = WishCardView.forNumberWish(number, registry);
                view.getDiscardPilePane().getChildren().add(wishCardView);
                return;
            } catch (NumberFormatException ignored) {
                // Ignore invalid numeric values.
            }
        }

        String topCardIdText = state.getTopCardId();
        if (topCardIdText == null || topCardIdText.isBlank()) {
            return;
        }

        int cardId;
        try {
            cardId = Integer.parseInt(topCardIdText);
        } catch (NumberFormatException e) {
            return;
        }

        CardView discardCardView = new CardView(cardId, registry, null, false);
        view.getDiscardPilePane().getChildren().add(discardCardView);
    }


    private void showFantasticEffectDialog(GameView view) {
        boolean alreadyOpen = view.getRootStack().getChildren().stream()
                .anyMatch(node -> node instanceof FantasticView);

        if (alreadyOpen) {
            return;
        }

        FantasticView effectView = new FantasticView();

        effectView.setOnFinish((color, number) -> {
            networkClient.resolveFantastic(color, number);
            view.getRootStack().getChildren().remove(effectView);
        });

        view.getRootStack().getChildren().add(effectView);
    }

    private void showEqualityEffectDialog(GameView view) {
        boolean alreadyOpen = view.getRootStack().getChildren().stream()
                .anyMatch(node -> node instanceof EqualityView);

        if (alreadyOpen) {
            return;
        }

        String username = state.getUsername();

        java.util.List<String> selectablePlayers = state.getPlayers().stream()
                .filter(name -> !name.equals(username))
                .toList();

        EqualityView effectView = new EqualityView(selectablePlayers);

        effectView.setOnFinish((targetPlayer, color) -> {
            networkClient.resolveEquality(targetPlayer, color);
            view.getRootStack().getChildren().remove(effectView);
        });

        view.getRootStack().getChildren().add(effectView);
    }

    private void showFantasticFourEffectDialog(GameView view) {
        boolean alreadyOpen = view.getRootStack().getChildren().stream()
                .anyMatch(node -> node instanceof FantasticFourView);

        if (alreadyOpen) {
            return;
        }

        String username = state.getUsername();

        java.util.List<String> selectablePlayers = state.getPlayers().stream()
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> !name.equals(username))
                .toList();

        FantasticFourView effectView = new FantasticFourView(selectablePlayers);

        effectView.setOnFinish(result -> {
            networkClient.resolveFantasticFour(
                    result.getColor(),
                    result.getNumber(),
                    result.getTargetPlayers()
            );
            view.getRootStack().getChildren().remove(effectView);
        });

        view.getRootStack().getChildren().add(effectView);
    }

    private boolean isSecondChanceActiveForMe() {
        String payload = state.getPendingEffectRequest();
        if (payload == null || payload.isBlank()) {
            return false;
        }

        String username = state.getUsername();
        if (username == null || username.isBlank()) {
            return false;
        }

        if (!payload.toUpperCase().startsWith("SECOND_CHANCE:")) {
            return false;
        }

        String targetPlayer = payload.substring("SECOND_CHANCE:".length()).trim();
        return targetPlayer.equals(username);
    }

    private void clearPendingEffectIfSecondChance() {
        String payload = state.getPendingEffectRequest();
        if (payload != null && payload.toUpperCase().startsWith("SECOND_CHANCE:")) {
            state.setPendingEffectRequest("");
        }
    }

    private void showGiftEffectDialog(GameView view) {
        boolean alreadyOpen = view.getRootStack().getChildren().stream()
                .anyMatch(node -> node instanceof GiftView);

        if (alreadyOpen) {
            return;
        }

        String username = state.getUsername();

        java.util.List<String> selectablePlayers = state.getPlayers().stream()
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> !name.equals(username))
                .toList();

        java.util.List<Integer> handCardIds = state.getCurrentHandCards().stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .toList();

        GiftView effectView = new GiftView(selectablePlayers, handCardIds, registry);

        effectView.setOnFinish((targetPlayer, cardIds) -> {
            networkClient.resolveGift(targetPlayer, cardIds);
            view.getRootStack().getChildren().remove(effectView);
        });

        view.getRootStack().getChildren().add(effectView);
    }

    private void showExchangeEffectDialog(GameView view) {
        boolean alreadyOpen = view.getRootStack().getChildren().stream()
                .anyMatch(node -> node instanceof ExchangeView);

        if (alreadyOpen) {
            return;
        }

        String username = state.getUsername();

        java.util.List<String> selectablePlayers = state.getPlayers().stream()
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> !name.equals(username))
                .toList();

        java.util.List<Integer> handCardIds = state.getCurrentHandCards().stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .toList();

        ExchangeView effectView = new ExchangeView(selectablePlayers, handCardIds, registry);

        effectView.setOnFinish((targetPlayer, cardIds) -> {
            networkClient.resolveExchange(targetPlayer, cardIds);
            view.getRootStack().getChildren().remove(effectView);
        });

        view.getRootStack().getChildren().add(effectView);
    }

    private void showSkipEffectDialog(GameView view) {
        boolean alreadyOpen = view.getRootStack().getChildren().stream()
                .anyMatch(node -> node instanceof SkipView);

        if (alreadyOpen) {
            return;
        }

        String username = state.getUsername();

        java.util.List<String> selectablePlayers = state.getPlayers().stream()
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> !name.equals(username))
                .toList();

        SkipView effectView = new SkipView(selectablePlayers);

        effectView.setOnFinish(targetPlayer -> {
            networkClient.resolveSkip(targetPlayer);
            view.getRootStack().getChildren().remove(effectView);
        });

        view.getRootStack().getChildren().add(effectView);
    }

    private void showNiceTryEffectDialog(GameView view) {
        boolean alreadyOpen = view.getRootStack().getChildren().stream()
                .anyMatch(node -> node instanceof NiceTryView);

        if (alreadyOpen) {
            return;
        }

        String username = state.getUsername();

        java.util.List<String> selectablePlayers = state.getPlayers().stream()
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> !name.equals(username))
                .toList();

        NiceTryView effectView = new NiceTryView(selectablePlayers);

        effectView.setOnFinish(targetPlayer -> {
            networkClient.resolveNiceTry(targetPlayer);
            view.getRootStack().getChildren().remove(effectView);
        });

        view.getRootStack().getChildren().add(effectView);
    }

    private void showCounterattackEffectDialog(GameView view) {
        boolean alreadyOpen = view.getRootStack().getChildren().stream()
                .anyMatch(node -> node instanceof CounterattackView);

        if (alreadyOpen) {
            return;
        }

        CounterattackView effectView = new CounterattackView();

        effectView.setOnFinish(color -> {
            networkClient.resolveCounterattack(color);
            view.getRootStack().getChildren().remove(effectView);
        });

        view.getRootStack().getChildren().add(effectView);
    }

    private void requestDrawWithAnimation(GameView view) {
        if (isSecondChanceActiveForMe()) {
            drawAnimationPending = true;
            handSizeBeforeDrawAnimation = state.getCurrentHandCards().size();

            registry.getSoundId("CARD_DRAWN").ifPresent(soundManager::play);

            networkClient.resolveSecondChanceDrawPenalty();
            clearPendingEffectIfSecondChance();
            return;
        }

        if (!canLocalPlayerPlayNow()) {
            state.getGameMessages().add("[CLIENT] You cannot draw right now.");
            return;
        }

        drawAnimationPending = true;
        handSizeBeforeDrawAnimation = state.getCurrentHandCards().size();

        registry.getSoundId("CARD_DRAWN").ifPresent(soundManager::play);
        networkClient.drawCard();
    }


    private void playDrawCardAnimationThenRender(GameView view) {
        StackPane animationLayer = view.getRootStack();

        CardBacksideView movingCard = new CardBacksideView();
        movingCard.setManaged(false);
        movingCard.setMouseTransparent(true);
        movingCard.setViewOrder(-2000);

        Bounds drawBounds = view.getDrawPilePane().localToScene(
                view.getDrawPilePane().getBoundsInLocal()
        );

        Bounds handBounds = view.getHandFanPane().localToScene(
                view.getHandFanPane().getBoundsInLocal()
        );

        Point2D start = animationLayer.sceneToLocal(
                drawBounds.getMinX() + drawBounds.getWidth() / 2,
                drawBounds.getMinY() + drawBounds.getHeight() / 2
        );

        Point2D end = animationLayer.sceneToLocal(
                handBounds.getMinX() + handBounds.getWidth() / 2,
                handBounds.getMinY() + handBounds.getHeight() * 0.55
        );

        double startX = start.getX() - CardBacksideView.CARD_WIDTH / 2;
        double startY = start.getY() - CardBacksideView.CARD_HEIGHT / 2;

        double endX = end.getX() - CardBacksideView.CARD_WIDTH / 2;
        double endY = end.getY() - CardBacksideView.CARD_HEIGHT / 2;

        animationLayer.getChildren().add(movingCard);

        /*
         * IMPORTANT:
         * unmanaged Regions are not automatically sized by the parent.
         * Without an explicit size, only the logo child may appear,
         * while the card background itself remains effectively invisible.
         */
        movingCard.resize(CardBacksideView.CARD_WIDTH, CardBacksideView.CARD_HEIGHT);
        movingCard.autosize();
        movingCard.applyCss();

        movingCard.relocate(startX, startY);

        drawAnimationRunning = true;

        TranslateTransition slide = new TranslateTransition(DRAW_ANIMATION_DURATION, movingCard);
        slide.setByX(endX - startX);
        slide.setByY(endY - startY);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition shrink = new ScaleTransition(DRAW_ANIMATION_DURATION, movingCard);
        shrink.setFromX(1.0);
        shrink.setFromY(1.0);
        shrink.setToX(0.82);
        shrink.setToY(0.82);
        shrink.setInterpolator(Interpolator.EASE_BOTH);

        javafx.animation.ParallelTransition animation =
                new javafx.animation.ParallelTransition(slide, shrink);

        animation.setOnFinished(e -> {
            animationLayer.getChildren().remove(movingCard);
            drawAnimationRunning = false;
            renderHand(view);
        });

        animation.play();
    }

    private boolean shouldAnimateOpponentDraw() {
        if (pendingOpponentDrawPlayer == null || pendingOpponentDrawPlayer.isBlank()) {
            return false;
        }

        if (opponentDrawAnimationRunning) {
            return false;
        }

        if (pendingOpponentDrawPlayer.equals(state.getUsername())) {
            return false;
        }

        Integer oldSize = knownPublicHandSizes.get(pendingOpponentDrawPlayer);
        if (oldSize == null) {
            return false;
        }

        return state.getPlayerInfoList().stream()
                .filter(info -> info.name().equals(pendingOpponentDrawPlayer))
                .findFirst()
                .map(info -> info.handSize() > oldSize)
                .orElse(false);
    }

    private void playOpponentDrawAnimationThenRefresh(GameView view, String targetPlayer) {
        OtherPlayerView targetSlot = view.getCircularTablePane()
                .getPlayerSlots()
                .get(targetPlayer);

        if (targetSlot == null) {
            refreshOtherPlayers(view);
            return;
        }

        StackPane animationLayer = view.getRootStack();

        CardBacksideView movingCard = new CardBacksideView();
        movingCard.setManaged(false);
        movingCard.setMouseTransparent(true);
        movingCard.setViewOrder(-2000);

        Bounds drawBounds = view.getDrawPilePane().localToScene(
                view.getDrawPilePane().getBoundsInLocal()
        );

        Point2D start = animationLayer.sceneToLocal(
                drawBounds.getMinX() + drawBounds.getWidth() / 2,
                drawBounds.getMinY() + drawBounds.getHeight() / 2
        );

        Point2D targetScenePoint = targetSlot.getHandTargetScenePoint();
        Point2D end = animationLayer.sceneToLocal(targetScenePoint);

        double startX = start.getX() - CardBacksideView.CARD_WIDTH / 2;
        double startY = start.getY() - CardBacksideView.CARD_HEIGHT / 2;

        double endX = end.getX() - CardBacksideView.CARD_WIDTH / 2;
        double endY = end.getY() - CardBacksideView.CARD_HEIGHT / 2;

        animationLayer.getChildren().add(movingCard);

        /*
         * Important because the animated card is unmanaged.
         * Without explicit sizing, only the logo may render.
         */
        movingCard.resize(CardBacksideView.CARD_WIDTH, CardBacksideView.CARD_HEIGHT);
        movingCard.autosize();
        movingCard.applyCss();
        movingCard.relocate(startX, startY);

        opponentDrawAnimationRunning = true;

        TranslateTransition slide = new TranslateTransition(DRAW_ANIMATION_DURATION, movingCard);
        slide.setByX(endX - startX);
        slide.setByY(endY - startY);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition shrink = new ScaleTransition(DRAW_ANIMATION_DURATION, movingCard);
        shrink.setFromX(1.0);
        shrink.setFromY(1.0);
        shrink.setToX(OtherPlayerView.OPPONENT_CARD_SCALE);
        shrink.setToY(OtherPlayerView.OPPONENT_CARD_SCALE);
        shrink.setInterpolator(Interpolator.EASE_BOTH);

        javafx.animation.ParallelTransition animation =
                new javafx.animation.ParallelTransition(slide, shrink);

        animation.setOnFinished(e -> {
            animationLayer.getChildren().remove(movingCard);
            opponentDrawAnimationRunning = false;

            refreshOtherPlayers(view);

            String nextTarget = opponentDrawAnimationQueue.poll();
            if (nextTarget != null) {
                enqueueOpponentDrawAnimation(view, nextTarget);
            }
        });

        animation.play();
    }

    private Point2D getPlayerHandScenePoint(GameView view, String playerName) {
        if (playerName != null && playerName.equals(state.getUsername())) {
            Bounds handBounds = view.getHandFanPane().localToScene(
                    view.getHandFanPane().getBoundsInLocal()
            );

            return new Point2D(
                    handBounds.getMinX() + handBounds.getWidth() / 2,
                    handBounds.getMinY() + handBounds.getHeight() * 0.55
            );
        }

        OtherPlayerView slot = view.getCircularTablePane()
                .getPlayerSlots()
                .get(playerName);

        if (slot != null) {
            return slot.getHandTargetScenePoint();
        }

        Bounds tableBounds = view.getCircularTablePane().localToScene(
                view.getCircularTablePane().getBoundsInLocal()
        );

        return new Point2D(
                tableBounds.getMinX() + tableBounds.getWidth() / 2,
                tableBounds.getMinY() + tableBounds.getHeight() / 2
        );
    }

    private double getPlayerCardScale(String playerName) {
        if (playerName != null && playerName.equals(state.getUsername())) {
            return 1.0;
        }

        return OtherPlayerView.OPPONENT_CARD_SCALE;
    }

    private Point2D findLocalCardScenePoint(GameView view, int cardId) {
        for (javafx.scene.Node node : view.getHandFanPane().getChildren()) {
            if (node instanceof CardView && Integer.valueOf(cardId).equals(node.getUserData())) {
                Bounds bounds = node.localToScene(node.getBoundsInLocal());

                return new Point2D(
                        bounds.getMinX() + bounds.getWidth() / 2,
                        bounds.getMinY() + bounds.getHeight() / 2
                );
            }
        }

        return getPlayerHandScenePoint(view, state.getUsername());
    }


    private void hideLocalCardDuringPlayAnimation(GameView view, int cardId) {
        for (javafx.scene.Node node : view.getHandFanPane().getChildren()) {
            if (node instanceof CardView && Integer.valueOf(cardId).equals(node.getUserData())) {
                node.setVisible(false);
                node.setManaged(false);
                return;
            }
        }
    }

    private void playCardToDiscardAnimationThenRefresh(
            GameView view,
            String playingPlayer,
            int playedCardId
    ) {
        if (playAnimationRunning) {
            renderHand(view);
            refreshOtherPlayers(view);
            renderDiscardPile(view);
            return;
        }

        StackPane animationLayer = view.getRootStack();

        boolean localPlayer = playingPlayer.equals(state.getUsername());

        Point2D startScenePoint = localPlayer
                ? findLocalCardScenePoint(view, playedCardId)
                : getPlayerHandScenePoint(view, playingPlayer);

        double startScale = getPlayerCardScale(playingPlayer);

        Bounds discardBounds = view.getDiscardPilePane().localToScene(
                view.getDiscardPilePane().getBoundsInLocal()
        );

        Point2D start = animationLayer.sceneToLocal(startScenePoint);

        Point2D end = animationLayer.sceneToLocal(
                discardBounds.getMinX() + discardBounds.getWidth() / 2,
                discardBounds.getMinY() + discardBounds.getHeight() / 2
        );

        CardView movingCard = new CardView(playedCardId, registry, null, false);
        movingCard.setManaged(false);
        movingCard.setMouseTransparent(true);
        movingCard.setViewOrder(-3000);

        double startX = start.getX() - CardView.CARD_WIDTH / 2;
        double startY = start.getY() - CardView.CARD_HEIGHT / 2;

        double endX = end.getX() - CardView.CARD_WIDTH / 2;
        double endY = end.getY() - CardView.CARD_HEIGHT / 2;

        animationLayer.getChildren().add(movingCard);

        movingCard.resize(CardView.CARD_WIDTH, CardView.CARD_HEIGHT);
        movingCard.autosize();
        movingCard.applyCss();
        movingCard.relocate(startX, startY);

        movingCard.setScaleX(startScale);
        movingCard.setScaleY(startScale);

        playAnimationRunning = true;

        if (localPlayer) {
            hideLocalCardDuringPlayAnimation(view, playedCardId);
        }

        TranslateTransition slide = new TranslateTransition(PLAY_ANIMATION_DURATION, movingCard);
        slide.setByX(endX - startX);
        slide.setByY(endY - startY);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scale = new ScaleTransition(PLAY_ANIMATION_DURATION, movingCard);
        scale.setFromX(startScale);
        scale.setFromY(startScale);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        javafx.animation.ParallelTransition animation =
                new javafx.animation.ParallelTransition(slide, scale);

        animation.setOnFinished(e -> {
            animationLayer.getChildren().remove(movingCard);
            playAnimationRunning = false;

            renderHand(view);
            refreshOtherPlayers(view);
            renderDiscardPile(view);
        });

        animation.play();
    }

    private void enqueueOpponentDrawAnimation(GameView view, String targetPlayer) {
        if (targetPlayer == null || targetPlayer.isBlank()) {
            return;
        }

        if (opponentDrawAnimationRunning) {
            opponentDrawAnimationQueue.add(targetPlayer);
            return;
        }

        playOpponentDrawAnimationThenRefresh(view, targetPlayer);
    }

    private void enqueueHiddenTransferAnimation(
            GameView view,
            String sourcePlayer,
            String targetPlayer,
            int count
    ) {
        if (sourcePlayer == null || sourcePlayer.isBlank()
                || targetPlayer == null || targetPlayer.isBlank()
                || count <= 0) {
            return;
        }

        CardTransferAnimation transfer = new CardTransferAnimation(sourcePlayer, targetPlayer, count);

        if (hiddenTransferAnimationRunning) {
            hiddenTransferAnimationQueue.add(transfer);
            return;
        }

        playHiddenTransferAnimationThenRefresh(view, transfer);
    }

    private void playHiddenTransferAnimationThenRefresh(
            GameView view,
            CardTransferAnimation transfer
    ) {
        StackPane animationLayer = view.getRootStack();

        Point2D startScenePoint = getPlayerHandScenePoint(view, transfer.sourcePlayer());
        Point2D endScenePoint = getPlayerHandScenePoint(view, transfer.targetPlayer());

        Point2D start = animationLayer.sceneToLocal(startScenePoint);
        Point2D end = animationLayer.sceneToLocal(endScenePoint);

        double startScale = getPlayerCardScale(transfer.sourcePlayer());
        double endScale = getPlayerCardScale(transfer.targetPlayer());

        int visibleCount = Math.min(transfer.count(), 4);

        java.util.List<javafx.animation.Animation> animations = new java.util.ArrayList<>();
        java.util.List<CardBacksideView> movingCards = new java.util.ArrayList<>();

        hiddenTransferAnimationRunning = true;

        for (int i = 0; i < visibleCount; i++) {
            CardBacksideView movingCard = new CardBacksideView();
            movingCard.setManaged(false);
            movingCard.setMouseTransparent(true);
            movingCard.setViewOrder(-2500 - i);

            double offset = (i - (visibleCount - 1) / 2.0) * 8.0;

            double startX = start.getX() - CardBacksideView.CARD_WIDTH / 2 + offset;
            double startY = start.getY() - CardBacksideView.CARD_HEIGHT / 2 + offset * 0.25;

            double endX = end.getX() - CardBacksideView.CARD_WIDTH / 2 + offset;
            double endY = end.getY() - CardBacksideView.CARD_HEIGHT / 2 + offset * 0.25;

            animationLayer.getChildren().add(movingCard);

            movingCard.resize(CardBacksideView.CARD_WIDTH, CardBacksideView.CARD_HEIGHT);
            movingCard.autosize();
            movingCard.applyCss();
            movingCard.relocate(startX, startY);

            movingCard.setScaleX(startScale);
            movingCard.setScaleY(startScale);

            movingCards.add(movingCard);

            TranslateTransition slide = new TranslateTransition(HIDDEN_TRANSFER_ANIMATION_DURATION, movingCard);
            slide.setByX(endX - startX);
            slide.setByY(endY - startY);
            slide.setInterpolator(Interpolator.EASE_BOTH);
            slide.setDelay(Duration.millis(i * 55L));

            ScaleTransition scale = new ScaleTransition(HIDDEN_TRANSFER_ANIMATION_DURATION, movingCard);
            scale.setFromX(startScale);
            scale.setFromY(startScale);
            scale.setToX(endScale);
            scale.setToY(endScale);
            scale.setInterpolator(Interpolator.EASE_BOTH);
            scale.setDelay(Duration.millis(i * 55L));

            animations.add(new javafx.animation.ParallelTransition(slide, scale));
        }

        javafx.animation.ParallelTransition all =
                new javafx.animation.ParallelTransition(animations.toArray(new javafx.animation.Animation[0]));

        all.setOnFinished(e -> {
            animationLayer.getChildren().removeAll(movingCards);
            hiddenTransferAnimationRunning = false;

            renderHand(view);
            refreshOtherPlayers(view);

            CardTransferAnimation next = hiddenTransferAnimationQueue.poll();
            if (next != null) {
                enqueueHiddenTransferAnimation(view, next.sourcePlayer(), next.targetPlayer(), next.count());
            }
        });

        all.play();
    }



    private record EventBannerData(String title, String description) {}

    private EventBannerData describeEventCard(int eventCardId) {
        return switch (eventCardId) {
            case 0 -> new EventBannerData("All Draw Two", "Everyone draws two cards.");
            case 1 -> new EventBannerData("All Draw One", "Everyone draws one card.");
            case 2 -> new EventBannerData("All Skip", "Everybody gets skipped once.");
            case 3 -> new EventBannerData("Instant Round End", "The round ends immediately.");
            case 4 -> new EventBannerData("Reverse Order", "Turn order is reversed.");
            case 5 -> new EventBannerData("Steal From Next", "The current player steals from the next player.");
            case 6 -> new EventBannerData("Steal From Previous", "The current player steals from the previous player.");
            case 7 -> new EventBannerData("Discard Highest", "Highest-value cards are discarded.");
            case 8 -> new EventBannerData("Discard Color", "A whole color gets discarded.");
            case 9 -> new EventBannerData("Swap Hands", "Hands are swapped around.");
            case 10 -> new EventBannerData("Block Specials", "Special cards are temporarily blocked.");
            case 11 -> new EventBannerData("Gift Chain", "Cards start moving around the table.");
            case 12 -> new EventBannerData("Hand Reset", "Hands are reset.");
            case 13 -> new EventBannerData("Lucky Draw", "Someone gets lucky with extra cards.");
            case 14 -> new EventBannerData("Penalty Draw", "Penalty cards are handed out.");
            case 15 -> new EventBannerData("Equalize", "Hands get pulled closer together.");
            case 16 -> new EventBannerData("Wild Request", "A new request changes what can be played.");
            case 17 -> new EventBannerData("Cancel Effects", "Pending effects are cancelled.");
            case 18 -> new EventBannerData("Bonus Play", "The current player gets another play.");
            case 19 -> new EventBannerData("Double Scoring", "This round will count double.");
            default -> new EventBannerData("Event Triggered", "A global event changes the game.");
        };
    }


    private boolean canLocalPlayerPlayNow() {
        return isCurrentTurnForMe(state.getCurrentPlayer())
                && "AWAITING_PLAY".equals(state.getCurrentPhase());
    }


    private void updateCurrentTurnHighlight(GameView view) {
        if (view == null || view.getCircularTablePane() == null) {
            return;
        }

        view.getCircularTablePane().highlightCurrentPlayer(state.getCurrentPlayer());
    }

    private void showSettingsOverlay(StackPane rootStack) {
        boolean alreadyOpen = rootStack.getChildren().stream()
                .anyMatch(node -> node instanceof SettingsView);

        if (alreadyOpen) {
            return;
        }

        SettingsView settingsView = new SettingsView(soundManager.getMusicVolume());
        settingsView.prefWidthProperty().bind(rootStack.widthProperty());
        settingsView.prefHeightProperty().bind(rootStack.heightProperty());

        settingsView.getMusicVolumeSlider().valueProperty().addListener((obs, oldValue, newValue) ->
                soundManager.setMusicVolume(newValue.doubleValue() / 100.0)
        );

        settingsView.getOkButton().setOnAction(e ->
                rootStack.getChildren().remove(settingsView)
        );

        rootStack.getChildren().add(settingsView);
    }
}
