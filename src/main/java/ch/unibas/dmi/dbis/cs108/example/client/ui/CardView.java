package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.CardTextFormatter;
import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import ch.unibas.dmi.dbis.cs108.example.model.game.Card;
import ch.unibas.dmi.dbis.cs108.example.model.game.DeckFactory;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

/**
 * Visual representation of a playable card in the player's hand.
 *
 * <p>A card view uses the configured asset registry to resolve background colors, text colors,
 * and optional SVG icons. If an icon is available, the label is shown below it; otherwise the
 * label is centered. A primary-button click invokes the provided {@code onPlay} callback.</p>
 *
 * <p>This control must be created on the JavaFX Application Thread because it may construct
 * {@link com.fluxvend.svgfx.SvgImageView} instances.</p>
 */
public class CardView extends StackPane {

    private static final double CARD_WIDTH = 90;
    private static final double CARD_HEIGHT = 130;
    private static final double ICON_SIZE = 54;

    private static volatile Map<Integer, Card> cardById;

    /**
     * Creates a card view for the given card ID.
     *
     * @param cardId   the card's numeric ID as used by the server
     * @param registry the asset registry used to resolve colors and icons
     * @param onPlay   called when the user clicks the card; may be {@code null}
     */
    public CardView(int cardId, AssetRegistry registry, Runnable onPlay) {
        getStyleClass().add("game-card-button");
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        setFocusTraversable(false);

        Card card = lookupCard(cardId);

        String bg = card != null
                ? registry.getBackgroundColor(card.color()).orElseGet(() -> fallbackBg(registry))
                : fallbackBg(registry);
        String textColor = card != null
                ? registry.getTextColor(card.color()).orElse("#ffffff")
                : "#ffffff";

        setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 16; -fx-border-radius: 16;");

        Label label = new Label(CardTextFormatter.formatCardLabel(cardId));
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(CARD_WIDTH - 8);
        label.setMouseTransparent(true);

        if (card != null) {
            registry.getIconView(card, ICON_SIZE).ifPresentOrElse(
                    icon -> {
                        icon.setMouseTransparent(true);
                        label.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 10px;");
                        StackPane.setAlignment(icon, Pos.CENTER);
                        StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
                        getChildren().addAll(icon, label);
                    },
                    () -> {
                        label.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 12px;");
                        StackPane.setAlignment(label, Pos.CENTER);
                        getChildren().add(label);
                    }
            );
        } else {
            label.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px;");
            StackPane.setAlignment(label, Pos.CENTER);
            getChildren().add(label);
        }

        if (onPlay != null) {
            setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) onPlay.run();
            });
        }
    }

    private static String fallbackBg(AssetRegistry registry) {
        var fb = registry.getFallback();
        return fb != null && fb.background() != null ? fb.background() : "#444444";
    }

    /**
     * Resolves a card by its numeric identifier.
     *
     * <p>The lookup table is built lazily from {@link DeckFactory#buildMainDeck()} and reused
     * for subsequent calls.</p>
     *
     * @param id the numeric card identifier
     * @return the matching card, or {@code null} if the id is not present in the deck
     */
    static Card lookupCard(int id) {
        if (cardById == null) {
            synchronized (CardView.class) {
                if (cardById == null) {
                    Map<Integer, Card> map = new HashMap<>();
                    for (Card c : DeckFactory.buildMainDeck()) {
                        map.put(c.id(), c);
                    }
                    cardById = map;
                }
            }
        }
        return cardById.get(id);
    }
}
