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




/**
 * Connect screen for the Frantic^-1 GUI client.
 *
 * <p>This view allows the user to enter the server host, port, and desired
 * username before opening the lobby. It is styled through JavaFX CSS and may
 * optionally display PNG assets if the corresponding files exist in the
 * resources folder.</p>
 */
public class ConnectView extends VBox {

    private final TextField hostField;
    private final TextField portField;
    private final TextField usernameField;
    private final Button connectButton = new Button("Connect");
    private final Label statusLabel = new Label();

    /**
     * Creates the connect view with default values.
     */
    public ConnectView() {
        this("localhost", "5555", System.getProperty("user.name", "Player"));
    }

    /**
     * Creates the connect view with prefilled host, port, and username values.
     *
     * @param initialHost the prefilled host
     * @param initialPort the prefilled port
     * @param initialUsername the prefilled username
     */
    public ConnectView(String initialHost, String initialPort, String initialUsername) {
        this.hostField = new TextField(initialHost == null || initialHost.isBlank()
                ? "localhost" : initialHost);
        this.portField = new TextField(initialPort == null || initialPort.isBlank()
                ? "5555" : initialPort);
        this.usernameField = new TextField(initialUsername == null || initialUsername.isBlank()
                ? System.getProperty("user.name", "Player") : initialUsername);

        getStyleClass().addAll("screen", "connect-screen");
        setSpacing(18);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);

        Label fallbackTitleLabel = new Label("Frantic^-1");
        fallbackTitleLabel.getStyleClass().add("screen-title");

        ImageView logoView = createLogoView();

        Label subtitleLabel = new Label("Connect to a lobby and bring chaos.");
        subtitleLabel.getStyleClass().add("screen-subtitle");

        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);

        if (logoView != null) {
            headerBox.getChildren().add(logoView);
        } else {
            headerBox.getChildren().add(fallbackTitleLabel);
        }

        headerBox.getChildren().add(subtitleLabel);

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

    public TextField getHostField() {
        return hostField;
    }

    public TextField getPortField() {
        return portField;
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public Button getConnectButton() {
        return connectButton;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }


    private ImageView createLogoView() {
        Image logoImage = SvgImageLoader.loadSvgAsImage(
                ConnectView.class,
                "/icons/logo_font.svg",
                900f,
                300f
        );

        if (logoImage == null) {
            return null;
        }

        ImageView logoView = new ImageView(logoImage);
        logoView.getStyleClass().add("connect-logo-image");
        logoView.setPreserveRatio(true);
        logoView.setSmooth(true);
        logoView.setMouseTransparent(true);

        /*
         * Technical display size:
         * The SVG is rendered larger for sharpness, but displayed smaller so it fits
         * into the connect panel. This is layout sizing, not visual styling.
         */
        logoView.setFitWidth(420);
        logoView.setFitHeight(140);

        return logoView;
    }
}