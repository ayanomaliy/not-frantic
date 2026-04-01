package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import ch.unibas.dmi.dbis.cs108.example.client.net.FxNetworkClient;
import ch.unibas.dmi.dbis.cs108.example.client.state.ClientState;
import ch.unibas.dmi.dbis.cs108.example.client.ui.MainController;
import javafx.application.Application;
import javafx.stage.Stage;

public class FranticFxApp extends Application {

    private final ClientState state = new ClientState();
    private final FxNetworkClient networkClient = new FxNetworkClient(state);

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(stage, state, networkClient);
        controller.showConnectView();

        stage.setTitle("Frantic^-1");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();
    }

    @Override
    public void stop() {
        networkClient.disconnect();
    }
}