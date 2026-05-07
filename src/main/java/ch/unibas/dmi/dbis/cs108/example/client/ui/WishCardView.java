package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Visual representation of an active wished color or wished number.
 *
 * <p>This is not a real card from the deck. It is only a UI representation
 * used on the discard pile when an effect requests a color or number.</p>
 */
public class WishCardView extends StackPane {

    private static final double CARD_WIDTH = CardView.CARD_WIDTH;
    private static final double CARD_HEIGHT = CardView.CARD_HEIGHT;
    private static final double ICON_SIZE = 54;

    private WishCardView() {
        getStyleClass().addAll("game-card-button", "wish-card");
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        setFocusTraversable(false);
    }

    /**
     * Creates a wish card for a requested color.
     *
     * @param color the requested color
     * @param registry the asset registry, kept for API consistency
     * @return a wish card showing the requested color
     */
    public static WishCardView forColorWish(CardColor color, AssetRegistry registry) {
        Objects.requireNonNull(color, "color must not be null");
        Objects.requireNonNull(registry, "registry must not be null");

        WishCardView view = new WishCardView();
        view.getStyleClass().addAll("wish-card-color", toCardColorStyleClass(color));

        VBox centerBox = new VBox(4);
        centerBox.getStyleClass().add("card-center-box");
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMouseTransparent(true);

        Label mainLabel = new Label(color.name());
        mainLabel.getStyleClass().add("wish-card-main-text");
        mainLabel.setWrapText(true);
        mainLabel.setAlignment(Pos.CENTER);
        mainLabel.setMaxWidth(CARD_WIDTH - 16);

        Label subtitle = new Label("REQUEST");
        subtitle.getStyleClass().add("card-subtitle-text");

        centerBox.getChildren().addAll(mainLabel, subtitle);

        AnchorPane overlay = createOverlay("WISH", "");
        view.getChildren().addAll(centerBox, overlay);

        return view;
    }

    /**
     * Creates a wish card for a requested number.
     *
     * @param number the requested number
     * @param registry the asset registry used to load the number SVG
     * @return a wish card showing the requested number
     */
    public static WishCardView forNumberWish(int number, AssetRegistry registry) {
        Objects.requireNonNull(registry, "registry must not be null");

        WishCardView view = new WishCardView();
        view.getStyleClass().add("wish-card-number");

        VBox centerBox = new VBox(4);
        centerBox.getStyleClass().add("card-center-box");
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMouseTransparent(true);

        registry.createIconView("icons/card_" + number + ".svg", ICON_SIZE).ifPresentOrElse(
                icon -> {
                    icon.setMouseTransparent(true);
                    icon.getStyleClass().addAll("card-main-icon", "card-number-icon");
                    centerBox.getChildren().add(icon);
                },
                () -> {
                    Label fallback = new Label(String.valueOf(number));
                    fallback.getStyleClass().add("card-main-fallback");
                    centerBox.getChildren().add(fallback);
                }
        );

        Label subtitle = new Label("REQUEST");
        subtitle.getStyleClass().add("card-subtitle-text");
        centerBox.getChildren().add(subtitle);

        AnchorPane overlay = createOverlay("WISH", String.valueOf(number));
        view.getChildren().addAll(centerBox, overlay);

        return view;
    }

    private static AnchorPane createOverlay(String topText, String bottomText) {
        Label topLabel = new Label(topText);
        topLabel.getStyleClass().add("card-top-text");
        topLabel.setMouseTransparent(true);

        Label bottomLabel = new Label(bottomText);
        bottomLabel.getStyleClass().add("card-bottom-text");
        bottomLabel.setMouseTransparent(true);

        AnchorPane overlay = new AnchorPane();
        overlay.setPickOnBounds(false);
        overlay.setMouseTransparent(true);

        AnchorPane.setTopAnchor(topLabel, 9.0);
        AnchorPane.setLeftAnchor(topLabel, 9.0);

        AnchorPane.setBottomAnchor(bottomLabel, 9.0);
        AnchorPane.setLeftAnchor(bottomLabel, 9.0);

        overlay.getChildren().addAll(topLabel, bottomLabel);
        return overlay;
    }

    private static String toCardColorStyleClass(CardColor color) {
        return switch (color) {
            case RED -> "card-red";
            case YELLOW -> "card-yellow";
            case GREEN -> "card-green";
            case BLUE -> "card-blue";
            case BLACK -> "card-black";
        };
    }
}