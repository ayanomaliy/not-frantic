package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import com.fluxvend.svgfx.SvgImageView;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;

/**
 * A self-contained node representing one opponent on the circular table.
 *
 * <p>Contains a tinted player icon, a name label, and a compact fan of face-down cards.
 * Call {@link #setFanRotation} to rotate only the card fan (so cards face inward) while
 * the icon and name label remain upright.</p>
 *
 * <p>Must be created on the JavaFX Application Thread.</p>
 */
public class OtherPlayerView extends Group {

    static final double ICON_SIZE = 60;
    static final double CARD_OFFSET_X = 18;
    static final double CARD_ROTATION_STEP = 4.0;

    private static final String PLAYER_ICON_PATH = "/icons/player.svg";

    private final AssetRegistry registry;
    private final Pane fanPane;
    private final Group fanGroup;
    private int handSize;

    /**
     * Creates a view with no asset registry; {@link #revealCards} will be a no-op.
     */
    public OtherPlayerView(String playerName, int handSize, String colorClass) {
        this(playerName, handSize, colorClass, null);
    }

    /**
     * Creates a view with the given asset registry for card reveals.
     */
    public OtherPlayerView(String playerName, int handSize, String colorClass, AssetRegistry registry) {
        this.registry = registry;
        this.handSize = handSize;

        Node iconNode = buildIconNode(colorClass);

        Label nameLabel = new Label(playerName);
        nameLabel.getStyleClass().add("field-label");
        nameLabel.setMouseTransparent(true);

        fanPane = new Pane();
        fanGroup = new Group(fanPane);

        VBox layout = new VBox(4, iconNode, nameLabel, fanGroup);
        layout.setAlignment(Pos.CENTER);
        getChildren().add(layout);

        buildFan(handSize);
    }

    private Node buildIconNode(String colorClass) {
        if (getClass().getResource(PLAYER_ICON_PATH) != null) {
            try {
                SvgImageView iconView = new SvgImageView(PLAYER_ICON_PATH);
                iconView.setPrefWidth(ICON_SIZE);
                iconView.setPrefHeight(ICON_SIZE);
                iconView.setMouseTransparent(true);

                Color tint = resolveColor(colorClass);
                ColorInput colorOverlay = new ColorInput(0, 0, ICON_SIZE, ICON_SIZE, tint);
                Blend blend = new Blend(BlendMode.SRC_ATOP);
                blend.setTopInput(colorOverlay);
                iconView.setEffect(blend);

                return iconView;
            } catch (Exception e) {
                System.err.println("[OtherPlayerView] Cannot load player icon: " + e.getMessage());
            }
        }
        return buildFallbackIcon(colorClass);
    }

    private Node buildFallbackIcon(String colorClass) {
        Circle circle = new Circle(ICON_SIZE / 2);
        circle.getStyleClass().add("player-color-" + colorClass);
        return circle;
    }

    private static Color resolveColor(String colorClass) {
        return switch (colorClass) {
            case "red"    -> Color.web("#e74c3c");
            case "green"  -> Color.web("#2ecc71");
            case "blue"   -> Color.web("#3498db");
            case "yellow" -> Color.web("#f1c40f");
            case "purple" -> Color.web("#9b59b6");
            case "pink"   -> Color.web("#e91e8c");
            case "orange" -> Color.web("#e67e22");
            case "teal"   -> Color.web("#1abc9c");
            default       -> Color.web("#72505b");
        };
    }

    private void buildFan(int n) {
        fanPane.getChildren().clear();
        if (n <= 0) {
            fanPane.setPrefWidth(0);
            fanPane.setPrefHeight(0);
            return;
        }

        double totalWidth = (n - 1) * CARD_OFFSET_X + CardBacksideView.CARD_WIDTH;
        double centerIndex = (n - 1) / 2.0;

        for (int i = 0; i < n; i++) {
            CardBacksideView card = new CardBacksideView();
            double rotation = (i - centerIndex) * CARD_ROTATION_STEP;
            card.setLayoutX(i * CARD_OFFSET_X);
            card.setLayoutY(0);
            card.setRotate(rotation);
            fanPane.getChildren().add(card);
        }

        fanPane.setPrefWidth(totalWidth);
        fanPane.setPrefHeight(CardBacksideView.CARD_HEIGHT);
    }

    /** Rebuilds the card fan with a new hand size. */
    public void setHandSize(int n) {
        this.handSize = n;
        buildFan(n);
    }

    /**
     * Replaces face-down cards with face-up {@link CardView} instances for the given IDs.
     * No-op if no asset registry was provided at construction.
     */
    public void revealCards(List<Integer> cardIds) {
        if (registry == null) return;
        fanPane.getChildren().clear();
        int n = cardIds.size();
        if (n == 0) {
            fanPane.setPrefWidth(0);
            fanPane.setPrefHeight(0);
            return;
        }
        double totalWidth = (n - 1) * CARD_OFFSET_X + CardBacksideView.CARD_WIDTH;
        double centerIndex = (n - 1) / 2.0;

        for (int i = 0; i < n; i++) {
            CardView card = new CardView(cardIds.get(i), registry, null);
            double rotation = (i - centerIndex) * CARD_ROTATION_STEP;
            card.setLayoutX(i * CARD_OFFSET_X);
            card.setLayoutY(0);
            card.setRotate(rotation);
            fanPane.getChildren().add(card);
        }

        fanPane.setPrefWidth(totalWidth);
        fanPane.setPrefHeight(CardBacksideView.CARD_HEIGHT);
    }

    /** Restores face-down card backsides, reverting any previous {@link #revealCards} call. */
    public void hideCards() {
        buildFan(handSize);
    }

    /**
     * Rotates the card fan independently so cards face inward on the circular table
     * while the icon and name label remain upright.
     */
    public void setFanRotation(double degrees) {
        fanGroup.setRotate(degrees);
    }

    /** Returns the current hand size. */
    public int getHandSize() {
        return handSize;
    }

    int getFanCardCount() {
        return fanPane.getChildren().size();
    }

    boolean hasFaceUpCards() {
        return fanPane.getChildren().stream().anyMatch(c -> c instanceof CardView);
    }

    double getFanRotation() {
        return fanGroup.getRotate();
    }
}
