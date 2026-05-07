package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.CardTextFormatter;
import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import ch.unibas.dmi.dbis.cs108.example.model.game.Card;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardType;
import ch.unibas.dmi.dbis.cs108.example.model.game.DeckFactory;
import com.fluxvend.svgfx.SvgImageView;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Visual representation of a playable card in the player's hand.
 *
 * <p>This version keeps visual styling in CSS as much as possible. The card view
 * adds semantic style classes such as {@code card-red}, {@code card-black},
 * {@code card-neutral}, {@code card-text-light}, and {@code card-text-dark}
 * so the theme can control the appearance centrally.</p>
 *
 * <p>If an icon is available, the label is placed near the bottom of the card.
 * Otherwise the label is centered.</p>
 *
 * <p>This control must be created on the JavaFX Application Thread because it may
 * construct {@link SvgImageView} instances.</p>
 */
public class CardView extends StackPane {

    private static final double CARD_WIDTH = 90;
    private static final double CARD_HEIGHT = 130;
    private static final double ICON_SIZE = 54;

    private static final double HOVER_SCALE = 1.10;
    private static final double HOVER_LIFT_Y = -22;
    private static final Duration HOVER_ANIMATION_DURATION = Duration.millis(140);

    private static volatile Map<Integer, Card> cardById;

    /**
     * Creates a card view for the given card ID.
     *
     * @param cardId the card's numeric ID as used by the server
     * @param registry the asset registry used to resolve icons
     * @param onPlay called when the user clicks the card; may be {@code null}
     */
    public CardView(int cardId, AssetRegistry registry, Runnable onPlay) {
        this(cardId, registry, onPlay, true);
    }

    public CardView(int cardId, AssetRegistry registry, Runnable onPlay, boolean hoverEnabled) {
        getStyleClass().addAll("game-card-button", "card-view");
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        setFocusTraversable(false);

        Card card = lookupCard(cardId);
        applyCardSurfaceClass(card);

        Label label = new Label(CardTextFormatter.formatCardLabel(cardId));
        label.getStyleClass().addAll("card-label", resolveCardTextClass(card));
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(CARD_WIDTH - 8);
        label.setMouseTransparent(true);

        if (card != null) {
            registry.getIconView(card, ICON_SIZE).ifPresentOrElse(
                    icon -> {
                        icon.setMouseTransparent(true);
                        label.getStyleClass().add("card-label-with-icon");
                        StackPane.setAlignment(icon, Pos.CENTER);
                        StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
                        getChildren().addAll(icon, label);
                    },
                    () -> {
                        label.getStyleClass().add("card-label-centered");
                        StackPane.setAlignment(label, Pos.CENTER);
                        getChildren().add(label);
                    }
            );
        } else {
            label.getStyleClass().addAll("card-text-light", "card-label-centered");
            StackPane.setAlignment(label, Pos.CENTER);
            getChildren().add(label);
        }

        if (onPlay != null) {
            setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    onPlay.run();
                }
            });
        }

        if (hoverEnabled) {
            installHoverAnimation();
        }
    }

    private void applyCardSurfaceClass(Card card) {
        getStyleClass().add(resolveCardSurfaceClass(card));
    }

    private static String resolveCardSurfaceClass(Card card) {
        if (card == null) {
            return "card-fallback";
        }

        CardColor color = card.color();
        if (color != null) {
            return switch (color) {
                case RED -> "card-red";
                case YELLOW -> "card-yellow";
                case GREEN -> "card-green";
                case BLUE -> "card-blue";
                case BLACK -> "card-black";
            };
        }

        if (card.type() == CardType.BLACK) {
            return "card-black";
        }

        if (card.type() == CardType.SPECIAL_FOUR) {
            return "card-four-color";
        }

        if (card.type() == CardType.FUCK_YOU) {
            return "card-fuck-you";
        }

        return "card-neutral";
    }

    private static String resolveCardTextClass(Card card) {
        if (card == null) {
            return "card-text-light";
        }

        CardColor color = card.color();
        if (color == CardColor.YELLOW || color == CardColor.GREEN) {
            return "card-text-dark";
        }

        return "card-text-light";
    }

    /**
     * Resolves a card by its numeric identifier.
     *
     * <p>The lookup table is built lazily from {@link DeckFactory#buildMainDeck()}
     * and reused for subsequent calls.</p>
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

    private void installHoverAnimation() {
        setOnMouseEntered(e -> {
            toFront();

            ScaleTransition scaleUp = new ScaleTransition(HOVER_ANIMATION_DURATION, this);
            scaleUp.setToX(HOVER_SCALE);
            scaleUp.setToY(HOVER_SCALE);

            TranslateTransition liftUp = new TranslateTransition(HOVER_ANIMATION_DURATION, this);
            liftUp.setToY(HOVER_LIFT_Y);

            new ParallelTransition(scaleUp, liftUp).play();
        });

        setOnMouseExited(e -> {
            ScaleTransition scaleDown = new ScaleTransition(HOVER_ANIMATION_DURATION, this);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);

            TranslateTransition dropDown = new TranslateTransition(HOVER_ANIMATION_DURATION, this);
            dropDown.setToY(0);

            new ParallelTransition(scaleDown, dropDown).play();
        });
    }
}