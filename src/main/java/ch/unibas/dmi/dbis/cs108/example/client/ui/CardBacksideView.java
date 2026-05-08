package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * A card-sized pane displaying the backside of a card.
 *
 * <p>This view is used for opponent hands, the draw pile, and draw animations.
 * The card surface is styled through CSS using {@code card-backside}. The center
 * logo is only decorative and does not define the card background.</p>
 *
 * <p>Must be created on the JavaFX Application Thread.</p>
 */
public class CardBacksideView extends StackPane {

    public static final double CARD_WIDTH = 90;
    public static final double CARD_HEIGHT = 130;

    private static final String LOGO_PATH = "/icons/frantic_logo.png";

    /**
     * Creates a new card backside view.
     */
    public CardBacksideView() {
        getStyleClass().addAll("card-view", "card-backside", "game-card-button");

        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);

        setFocusTraversable(false);
        setMouseTransparent(true);

        StackPane logoBox = new StackPane();
        logoBox.getStyleClass().add("card-backside-logo-box");
        logoBox.setMouseTransparent(true);

        if (getClass().getResource(LOGO_PATH) != null) {
            Image image = new Image(getClass().getResource(LOGO_PATH).toExternalForm());
            ImageView logo = new ImageView(image);
            logo.getStyleClass().add("card-backside-logo-image");
            logo.setPreserveRatio(true);
            logo.setFitWidth(46);
            logo.setFitHeight(46);
            logo.setMouseTransparent(true);
            logoBox.getChildren().add(logo);
        } else {
            Label fallbackLogo = new Label("F");
            fallbackLogo.getStyleClass().add("card-backside-logo-text");
            fallbackLogo.setMouseTransparent(true);
            logoBox.getChildren().add(fallbackLogo);
        }

        StackPane.setAlignment(logoBox, Pos.CENTER);
        getChildren().add(logoBox);
    }
}