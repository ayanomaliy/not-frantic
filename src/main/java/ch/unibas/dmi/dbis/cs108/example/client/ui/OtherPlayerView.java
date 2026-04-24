package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;

import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

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

    static final double ICON_SIZE = 46;
    static final double CARD_OFFSET_X = 10;
    static final double CARD_ROTATION_STEP = 2.0;
    static final double OPPONENT_CARD_SCALE = 0.52;
    static final double FAN_VERTICAL_ARC = 8.0;

    static final double INFO_INWARD_DISTANCE = 34.0;
    static final double FAN_OUTWARD_DISTANCE = 88.0;

    static final double PLAYER_OUTWARD_DISTANCE = 14.0;
    static final double HAND_INWARD_DISTANCE = 82.0;

    private static final String PLAYER_ICON_PATH = "/icons/player.svg";

    private final AssetRegistry registry;
    private final Pane fanPane;
    private final Group fanGroup;
    private final Group infoGroup;
    private double seatAngleDegrees = 0.0;
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

        VBox infoBox = new VBox(2, iconNode, nameLabel);
        infoBox.setAlignment(Pos.CENTER);
        infoGroup = new Group(infoBox);

        getChildren().addAll(fanGroup, infoGroup);

        buildFan(handSize);
        updateSeatLayout();
    }

    public void setSeatAngle(double degrees) {
        this.seatAngleDegrees = degrees;
        updateSeatLayout();
    }

    private void updateSeatLayout() {
        double rad = Math.toRadians(seatAngleDegrees);

        /*
         * Vector from center outward toward this opponent seat.
         * Example:
         * - top-left opponent -> up-left
         * - top-right opponent -> up-right
         */
        double outwardX = Math.sin(rad);
        double outwardY = -Math.cos(rad);

        Bounds infoBounds = infoGroup.getLayoutBounds();
        double infoW = infoBounds.getWidth();
        double infoH = infoBounds.getHeight();

        Bounds fanBounds = fanGroup.getLayoutBounds();
        double fanW = fanBounds.getWidth();
        double fanH = fanBounds.getHeight();

        /*
         * Player icon + name go OUTWARD from the seat anchor.
         */
        infoGroup.setLayoutX(outwardX * PLAYER_OUTWARD_DISTANCE - infoW / 2);
        infoGroup.setLayoutY(outwardY * PLAYER_OUTWARD_DISTANCE - infoH / 2);

        /*
         * Opponent hand goes INWARD toward the table center.
         */
        fanGroup.setLayoutX(-outwardX * HAND_INWARD_DISTANCE - fanW / 2);
        fanGroup.setLayoutY(-outwardY * HAND_INWARD_DISTANCE - fanH / 2);

        /*
         * Rotate only the fan so the cards face inward.
         */
        fanGroup.setRotate(seatAngleDegrees + 180);

        fanGroup.toBack();
        infoGroup.toFront();
    }

    private Node buildIconNode(String colorClass) {
        return buildFallbackIcon(colorClass);
    }

    private Node buildFallbackIcon(String colorClass) {
        Circle circle = new Circle(ICON_SIZE / 2);
        circle.getStyleClass().add("player-color-" + colorClass);
        return circle;
    }



    private void buildFan(int n) {
        fanPane.getChildren().clear();

        if (n <= 0) {
            fanPane.setPrefWidth(0);
            fanPane.setPrefHeight(0);
            return;
        }

        double scaledCardWidth = CardBacksideView.CARD_WIDTH * OPPONENT_CARD_SCALE;
        double scaledCardHeight = CardBacksideView.CARD_HEIGHT * OPPONENT_CARD_SCALE;

        double totalWidth = (n - 1) * CARD_OFFSET_X + scaledCardWidth;
        double centerIndex = (n - 1) / 2.0;

        double yOffsetChange = (n > 1) ? Math.PI / (n - 1) : 0;
        double currentYOffset = 0;

        for (int i = 0; i < n; i++) {
            CardBacksideView card = new CardBacksideView();

            double rotation = (i - centerIndex) * CARD_ROTATION_STEP;
            double yOffset = -Math.sin(currentYOffset) * FAN_VERTICAL_ARC;

            card.setScaleX(OPPONENT_CARD_SCALE);
            card.setScaleY(OPPONENT_CARD_SCALE);

            card.setLayoutX(i * CARD_OFFSET_X);
            card.setLayoutY(yOffset);
            card.setRotate(rotation);

            fanPane.getChildren().add(card);
            currentYOffset += yOffsetChange;
        }

        fanPane.setPrefWidth(totalWidth);
        fanPane.setPrefHeight(scaledCardHeight + FAN_VERTICAL_ARC);
        updateSeatLayout();
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

        double scaledCardWidth = CardBacksideView.CARD_WIDTH * OPPONENT_CARD_SCALE;
        double scaledCardHeight = CardBacksideView.CARD_HEIGHT * OPPONENT_CARD_SCALE;

        double totalWidth = (n - 1) * CARD_OFFSET_X + scaledCardWidth;
        double centerIndex = (n - 1) / 2.0;

        double yOffsetChange = (n > 1) ? Math.PI / (n - 1) : 0;
        double currentYOffset = 0;

        for (int i = 0; i < n; i++) {
            CardView card = new CardView(cardIds.get(i), registry, null, false);

            double rotation = (i - centerIndex) * CARD_ROTATION_STEP;
            double yOffset = -Math.sin(currentYOffset) * FAN_VERTICAL_ARC;

            card.setScaleX(OPPONENT_CARD_SCALE);
            card.setScaleY(OPPONENT_CARD_SCALE);

            card.setLayoutX(i * CARD_OFFSET_X);
            card.setLayoutY(yOffset);
            card.setRotate(rotation);

            fanPane.getChildren().add(card);
            currentYOffset += yOffsetChange;
        }

        fanPane.setPrefWidth(totalWidth);
        fanPane.setPrefHeight(scaledCardHeight + FAN_VERTICAL_ARC);
        updateSeatLayout();
    }

    /** Restores face-down card backsides, reverting any previous {@link #revealCards} call. */
    public void hideCards() {
        buildFan(handSize);
        updateSeatLayout();
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
