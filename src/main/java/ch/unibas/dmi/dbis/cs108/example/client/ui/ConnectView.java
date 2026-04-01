package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
/**
 * JavaFX view for the initial server connection screen.
 *
 * <p>This view provides input fields for the server host, port, and
 * username, as well as a connect button and a status label for feedback.</p>
 */
public class ConnectView extends VBox {
    /** Text field containing the target server host. */
    private final TextField hostField = new TextField("localhost");
    /** Text field containing the target server port. */
    private final TextField portField = new TextField("5555");
    /** Text field containing the player's desired username. */
    private final TextField usernameField = new TextField(System.getProperty("user.name", "Player"));
    /** Button that starts the connection attempt. */
    private final Button connectButton = new Button("Connect");
    /** Label used to display connection status or error messages. */
    private final Label statusLabel = new Label();
    /**
     * Creates the connect view and initializes its layout and controls.
     *
     * <p>The view consists of a small form for host, port, and username,
     * followed by a connect button and a status label.</p>
     */
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

    /**
     * Returns the host input field.
     *
     * @return the host text field
     */
    public TextField getHostField() {
        return hostField;
    }

    /**
     * Returns the port input field.
     *
     * @return the port text field
     */
    public TextField getPortField() {
        return portField;
    }

    /**
     * Returns the username input field.
     *
     * @return the username text field
     */
    public TextField getUsernameField() {
        return usernameField;
    }

    /**
     * Returns the connect button.
     *
     * @return the connect button
     */
    public Button getConnectButton() {
        return connectButton;
    }

    /**
     * Returns the status label.
     *
     * @return the status label
     */
    public Label getStatusLabel() {
        return statusLabel;
    }
}