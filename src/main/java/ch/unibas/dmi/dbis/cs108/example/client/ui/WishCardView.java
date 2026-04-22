package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

import java.util.Objects;

/**
 * Visual representation of a requested color or requested number on the discard pile.
 *
 * <p>This view is not a real game card from the deck. It is a UI-only representation
 * of an active wish state. The control reuses the same size and general card styling
 * as normal hand cards so it fits visually into the existing game table.</p>
 *
 * <p>Rendering rules:</p>
 * <ul>
 *   <li><b>Color wish:</b> shows a blank card in the requested color.</li>
 *   <li><b>Number wish:</b> shows a special-style card with only the wished number icon.</li>
 * </ul>
 *
 * <p>All visual styling is driven by CSS classes so the wish cards stay consistent
 * with normal {@link CardView} colors.</p>
 */
public class WishCardView extends StackPane {

    private static final double CARD_WIDTH = 90;
    private static final double CARD_HEIGHT = 130;
    private static final double ICON_SIZE = 54;

    private WishCardView() {
        getStyleClass().addAll("game-card-button", "wish-card");
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        setFocusTraversable(false);
    }

    /**
     * Creates a visual card for a requested color.
     *
     * @param color the requested color to display
     * @param registry unused for color styling now, kept for API compatibility
     * @return a wish card showing the requested color
     * @throws NullPointerException if {@code color} or {@code registry} is {@code null}
     */
    public static WishCardView forColorWish(CardColor color, AssetRegistry registry) {
        Objects.requireNonNull(color, "color must not be null");
        Objects.requireNonNull(registry, "registry must not be null");

        WishCardView view = new WishCardView();
        view.getStyleClass().addAll("wish-card-color", toCardColorStyleClass(color));
        return view;
    }

    /**
     * Creates a visual card for a requested number.
     *
     * @param number the requested number
     * @param registry the asset registry used to load the configured number icon
     * @return a wish card showing the requested number
     * @throws NullPointerException if {@code registry} is {@code null}
     */
    public static WishCardView forNumberWish(int number, AssetRegistry registry) {
        Objects.requireNonNull(registry, "registry must not be null");

        WishCardView view = new WishCardView();
        view.getStyleClass().add("wish-card-number");

        registry.createIconView("icons/card_" + number + ".svg", ICON_SIZE).ifPresent(icon -> {
            icon.setMouseTransparent(true);
            StackPane.setAlignment(icon, Pos.CENTER);
            view.getChildren().add(icon);
        });

        return view;
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