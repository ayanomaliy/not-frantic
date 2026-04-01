package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.net.FxNetworkClient;
import ch.unibas.dmi.dbis.cs108.example.client.state.ClientState;
import ch.unibas.dmi.dbis.cs108.example.client.ui.view.ConnectView;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainController {

    private final Stage stage;
    private final ClientState state;
    private final FxNetworkClient networkClient;

    public MainController(Stage stage, ClientState state, FxNetworkClient networkClient) {
        this.stage = stage;
        this.state = state;
        this.networkClient = networkClient;
    }

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

    private void sendChat(LobbyView view) {
        String text = view.getChatInput().getText().trim();
        if (!text.isBlank()) {
            networkClient.sendChat(text);
            view.getChatInput().clear();
        }
    }
}