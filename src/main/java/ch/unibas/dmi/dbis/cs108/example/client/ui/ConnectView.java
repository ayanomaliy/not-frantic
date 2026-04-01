package ch.unibas.dmi.dbis.cs108.example.client.ui.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ConnectView extends VBox {

    private final TextField hostField = new TextField("localhost");
    private final TextField portField = new TextField("5555");
    private final TextField usernameField = new TextField(System.getProperty("user.name", "Player"));
    private final Button connectButton = new Button("Connect");
    private final Label statusLabel = new Label();

    public ConnectView() {
        setSpacing(12);
        setPadding(new Insets(20));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        form.addRow(0, new Label("Host:"), hostField);
        form.addRow(1, new Label("Port:"), portField);
        form.addRow(2, new Label("Username:"), usernameField);

        getChildren().addAll(
                new Label("Frantic^-1"),
                form,
                connectButton,
                statusLabel
        );
    }

    public TextField getHostField() { return hostField; }
    public TextField getPortField() { return portField; }
    public TextField getUsernameField() { return usernameField; }
    public Button getConnectButton() { return connectButton; }
    public Label getStatusLabel() { return statusLabel; }
}