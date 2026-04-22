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
 * <p>Static visual styling such as radius, border, and special-card background should
 * come from CSS classes. Only the dynamic wished color is applied programmatically,
 * because it depends on runtime state.</p>
 */
public class WishCardView extends StackPane {

    /** Shared card width used by the game card visuals. */
    private static final double CARD_WIDTH = 90;

    /** Shared card height used by the game card visuals. */
    private static final double CARD_HEIGHT = 130;

    /** Preferred size of the centered number icon for number wishes. */
    private static final double ICON_SIZE = 54;

    /**
     * Creates the base container for a wish card.
     *
     * <p>The common geometry is defined here, while visual appearance is supplied
     * through CSS classes and, for color wishes, a runtime background color.</p>
     */
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
     * <p>The card is intentionally blank and uses only the configured card color
     * background so that it reads as a pure color wish rather than as a normal
     * playable card.</p>
     *
     * @param color the requested color to display
     * @param registry the asset registry used to resolve configured card colors
     * @return a wish card showing the requested color
     * @throws NullPointerException if {@code color} or {@code registry} is {@code null}
     */
    public static WishCardView forColorWish(CardColor color, AssetRegistry registry) {
        Objects.requireNonNull(color, "color must not be null");
        Objects.requireNonNull(registry, "registry must not be null");

        WishCardView view = new WishCardView();
        view.getStyleClass().add("wish-card-color");

        String bg = registry.getBackgroundColor(color).orElse("");
        if (!bg.isBlank()) {
            view.setStyle("-fx-background-color: " + bg + ";");
        }

        return view;
    }

    /**
     * Creates a visual card for a requested number.
     *
     * <p>The background styling is supplied through CSS so that the special-card
     * appearance remains centralized in the theme. Only the requested number icon
     * is added programmatically.</p>
     *
     * @param number the requested number, expected in the range supported by the card icons
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
}