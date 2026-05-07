package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import ch.unibas.dmi.dbis.cs108.example.model.game.Card;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardType;
import ch.unibas.dmi.dbis.cs108.example.model.game.DeckFactory;
import ch.unibas.dmi.dbis.cs108.example.model.game.SpecialEffect;
import com.fluxvend.svgfx.SvgImageView;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Visual representation of a playable Frantic^-1 card.
 *
 * <p>The card is rendered as a custom JavaFX control instead of a standard
 * Button skin. This makes it possible to imitate the website card design:
 * rounded corners, gradient backgrounds, top-left type text, centered SVG
 * number/effect icon, subtitle text, and bottom-left score/value text.</p>
 *
 * <p>The actual colors and visual surface are controlled by CSS classes such as
 * {@code card-red}, {@code card-blue}, {@code card-black}, and
 * {@code card-four-color}. SVG icons are resolved through {@link AssetRegistry}.</p>
 */
public class CardView extends StackPane {

    public static final double CARD_WIDTH = 90;
    public static final double CARD_HEIGHT = 130;

    private static final double NUMBER_ICON_SIZE = 54;
    private static final double EFFECT_ICON_SIZE = 46;

    private static final double HOVER_SCALE = 1.06;
    private static final double HOVER_LIFT_Y = -14;
    private static final Duration HOVER_ANIMATION_DURATION = Duration.millis(140);

    private double viewOrderBeforeHover;
    private boolean raisedByHover = false;

    private static volatile Map<Integer, Card> cardById;

    /**
     * Creates a card view for the given card ID.
     *
     * @param cardId the card id used by the server
     * @param registry the asset registry used to load SVG icons
     * @param onPlay callback executed when the card is clicked, or {@code null}
     */
    public CardView(int cardId, AssetRegistry registry, Runnable onPlay) {
        this(cardId, registry, onPlay, true);
    }

    /**
     * Creates a card view for the given card ID.
     *
     * @param cardId the card id used by the server
     * @param registry the asset registry used to load SVG icons
     * @param onPlay callback executed when the card is clicked, or {@code null}
     * @param hoverEnabled whether hover lift/scale animation should be installed
     */
    public CardView(int cardId, AssetRegistry registry, Runnable onPlay, boolean hoverEnabled) {
        getStyleClass().addAll("game-card-button", "card-view");

        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        setFocusTraversable(false);

        Card card = lookupCard(cardId);
        getStyleClass().add(resolveCardSurfaceClass(card));

        VBox centerBox = new VBox(4);
        centerBox.getStyleClass().add("card-center-box");
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMouseTransparent(true);

        Node centerGraphic = buildCenterGraphic(card, registry);
        Label subtitleLabel = new Label(resolveSubtitleText(card));
        subtitleLabel.getStyleClass().add("card-subtitle-text");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMaxWidth(CARD_WIDTH - 16);
        subtitleLabel.setMouseTransparent(true);

        centerBox.getChildren().addAll(centerGraphic, subtitleLabel);

        Label topLabel = new Label(resolveTopText(card));
        topLabel.getStyleClass().add("card-top-text");
        topLabel.setMouseTransparent(true);

        Label bottomLabel = new Label(resolveBottomText(card));
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

        StackPane.setAlignment(centerBox, Pos.CENTER);
        getChildren().addAll(centerBox, overlay);

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

    private Node buildCenterGraphic(Card card, AssetRegistry registry) {
        if (card == null) {
            return createFallbackGraphic("?");
        }

        double size = isNumberCard(card) ? NUMBER_ICON_SIZE : EFFECT_ICON_SIZE;

        return registry.getIconView(card, size)
                .map(icon -> prepareIcon(icon, card))
                .orElseGet(() -> createFallbackGraphic(resolveFallbackCenterText(card)));
    }

    private Node prepareIcon(SvgImageView icon, Card card) {
        icon.setMouseTransparent(true);
        icon.getStyleClass().add("card-main-icon");

        if (isNumberCard(card)) {
            icon.getStyleClass().add("card-number-icon");
        } else {
            icon.getStyleClass().add("card-effect-icon");
        }

        return icon;
    }

    private Node createFallbackGraphic(String text) {
        Label fallback = new Label(text);
        fallback.getStyleClass().add("card-main-fallback");
        fallback.setMouseTransparent(true);
        return fallback;
    }

    private static boolean isNumberCard(Card card) {
        return card != null
                && card.value() > 0
                && (card.type() == CardType.COLOR || card.type() == CardType.BLACK);
    }

    private static String resolveFallbackCenterText(Card card) {
        if (card == null) {
            return "?";
        }

        if (card.value() > 0) {
            return String.valueOf(card.value());
        }

        if (card.effect() != null) {
            return effectShortSymbol(card.effect());
        }

        if (card.type() == CardType.FUCK_YOU) {
            return "!";
        }

        return "?";
    }

    private static String effectShortSymbol(SpecialEffect effect) {
        return switch (effect) {
            case SECOND_CHANCE -> "↻";
            case SKIP -> "⏭";
            case GIFT -> "🎁";
            case EXCHANGE -> "⇄";
            case FANTASTIC -> "★";
            case FANTASTIC_FOUR -> "✦";
            case EQUALITY -> "=";
            case COUNTERATTACK -> "↯";
            case NICE_TRY -> "!";
        };
    }

    private static String resolveTopText(Card card) {
        if (card == null || card.type() == null) {
            return "CARD";
        }

        return switch (card.type()) {
            case COLOR -> card.color() == null ? "COLOR" : card.color().name();
            case BLACK -> "BLACK";
            case SPECIAL_SINGLE, SPECIAL_FOUR, FUCK_YOU -> "SPECIAL";
            case EVENT -> "EVENT";
        };
    }

    private static String resolveSubtitleText(Card card) {
        if (card == null || card.type() == null) {
            return "";
        }

        return switch (card.type()) {
            case COLOR -> "NORMAL";
            case BLACK -> "TRIGGER EVENT";
            case SPECIAL_SINGLE, SPECIAL_FOUR -> prettyEffectName(card.effect());
            case FUCK_YOU -> "FUCK YOU";
            case EVENT -> "EVENT";
        };
    }

    private static String resolveBottomText(Card card) {
        if (card == null || card.type() == null) {
            return "";
        }

        return switch (card.type()) {
            case COLOR, BLACK -> card.value() > 0 ? String.valueOf(card.value()) : "";
            case SPECIAL_SINGLE, SPECIAL_FOUR -> "10";
            case FUCK_YOU -> "50";
            case EVENT -> "";
        };
    }

    private static String prettyEffectName(SpecialEffect effect) {
        if (effect == null) {
            return "SPECIAL";
        }

        return switch (effect) {
            case SECOND_CHANCE -> "SECOND CHANCE";
            case SKIP -> "SKIP";
            case GIFT -> "GIFT";
            case EXCHANGE -> "EXCHANGE";
            case FANTASTIC -> "FANTASTIC";
            case FANTASTIC_FOUR -> "FANTASTIC FOUR";
            case EQUALITY -> "EQUALITY";
            case COUNTERATTACK -> "COUNTER";
            case NICE_TRY -> "NICE TRY";
        };
    }

    private static String resolveCardSurfaceClass(Card card) {
        if (card == null || card.type() == null) {
            return "card-fallback";
        }

        if (card.type() == CardType.SPECIAL_FOUR || card.effect() == SpecialEffect.NICE_TRY) {
            return "card-four-color";
        }

        if (card.type() == CardType.FUCK_YOU) {
            return "card-fuck-you";
        }

        if (card.type() == CardType.BLACK) {
            return "card-black";
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

        return "card-neutral";
    }

    /**
     * Resolves a card by its numeric identifier.
     *
     * <p>The lookup table is built lazily from {@link DeckFactory#buildMainDeck()}
     * and reused for later calls.</p>
     *
     * @param id the numeric card id
     * @return the matching card, or {@code null} if the id is unknown
     */
    static Card lookupCard(int id) {
        if (cardById == null) {
            synchronized (CardView.class) {
                if (cardById == null) {
                    Map<Integer, Card> map = new HashMap<>();
                    for (Card card : DeckFactory.buildMainDeck()) {
                        map.put(card.id(), card);
                    }
                    cardById = map;
                }
            }
        }

        return cardById.get(id);
    }


    private void installHoverAnimation() {
        setOnMouseEntered(e -> {
            if (!raisedByHover) {
                viewOrderBeforeHover = getViewOrder();
                setViewOrder(-1000);
                raisedByHover = true;
            }

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

            ParallelTransition resetAnimation = new ParallelTransition(scaleDown, dropDown);

            resetAnimation.setOnFinished(event -> {
                setViewOrder(viewOrderBeforeHover);
                raisedByHover = false;
            });

            resetAnimation.play();
        });
    }
}