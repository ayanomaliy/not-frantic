package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.InputStream;

/**
 * Connect screen for the Frantic^-1 GUI client.
 *
 * <p>This view allows the user to enter the server host, port, and desired
 * username before opening the lobby. It is styled through JavaFX CSS and may
 * optionally display PNG assets if the corresponding files exist in the
 * resources folder.</p>
 */
public class ConnectView extends VBox {

    private final TextField hostField = new TextField("localhost");
    private final TextField portField = new TextField("5555");
    private final TextField usernameField = new TextField(System.getProperty("user.name", "Player"));
    private final Button connectButton = new Button("Connect");
    private final Label statusLabel = new Label();

    /**
     * Creates the connect view.
     */
    public ConnectView() {
        getStyleClass().addAll("screen", "connect-screen");
        setSpacing(18);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Frantic^-1");
        titleLabel.getStyleClass().add("screen-title");

        Label subtitleLabel = new Label("Connect to a lobby and bring chaos.");
        subtitleLabel.getStyleClass().add("screen-subtitle");

        ImageView logoView = createImageView("/images/app/logo.png", 96, 96);

        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);
        if (logoView != null) {
            headerBox.getChildren().add(logoView);
        }
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.setHgap(12);
        form.setVgap(12);

        Label hostLabel = new Label("Host");
        Label portLabel = new Label("Port");
        Label usernameLabel = new Label("Username");

        hostLabel.getStyleClass().add("field-label");
        portLabel.getStyleClass().add("field-label");
        usernameLabel.getStyleClass().add("field-label");

        hostField.getStyleClass().add("frantic-text-field");
        portField.getStyleClass().add("frantic-text-field");
        usernameField.getStyleClass().add("frantic-text-field");
        connectButton.getStyleClass().addAll("frantic-button", "primary-button");
        statusLabel.getStyleClass().add("status-label");

        hostField.setPromptText("localhost");
        portField.setPromptText("5555");
        usernameField.setPromptText("Player");

        form.addRow(0, hostLabel, hostField);
        form.addRow(1, portLabel, portField);
        form.addRow(2, usernameLabel, usernameField);

        HBox buttonRow = new HBox(connectButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(18, headerBox, form, buttonRow, statusLabel);
        card.getStyleClass().add("panel");
        card.setMaxWidth(520);

        getChildren().add(card);
    }

    /**
     * Returns the host input field.
     *
     * @return the host field
     */
    public TextField getHostField() {
        return hostField;
    }

    /**
     * Returns the port input field.
     *
     * @return the port field
     */
    public TextField getPortField() {
        return portField;
    }

    /**
     * Returns the username input field.
     *
     * @return the username field
     */
    public TextField getUsernameField() {
        return usernameField;
    }

    /**
     * Returns the button used to establish the connection.
     *
     * @return the connect button
     */
    public Button getConnectButton() {
        return connectButton;
    }

    /**
     * Returns the status label shown below the connect form.
     *
     * @return the status label
     */
    public Label getStatusLabel() {
        return statusLabel;
    }

    /**
     * Creates an image view for a PNG resource if the resource exists.
     *
     * @param resourcePath the classpath resource path
     * @param fitWidth preferred width
     * @param fitHeight preferred height
     * @return the configured image view, or {@code null} if the image is missing
     */
    private ImageView createImageView(String resourcePath, double fitWidth, double fitHeight) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }

            ImageView imageView = new ImageView(new Image(in));
            imageView.setFitWidth(fitWidth);
            imageView.setFitHeight(fitHeight);
            imageView.setPreserveRatio(true);
            return imageView;
        } catch (Exception e) {
            return null;
        }
    }
}