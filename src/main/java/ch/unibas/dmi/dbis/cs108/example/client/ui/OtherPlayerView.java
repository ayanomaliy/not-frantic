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

import javafx.geometry.Point2D;

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
    public static final double OPPONENT_CARD_SCALE = 0.52;
    static final double FAN_VERTICAL_ARC = 8.0;

    static final double INFO_INWARD_DISTANCE = 34.0;
    static final double FAN_OUTWARD_DISTANCE = 88.0;

    static final double PLAYER_OUTWARD_DISTANCE = 38.0;
    static final double HAND_INWARD_DISTANCE = 82.0;

    private static final String PLAYER_ICON_PATH = "/icons/player.svg";

    private final AssetRegistry registry;
    private final Pane fanPane;
    private final Group fanGroup;
    private final Group infoGroup;
    private double seatAngleDegrees = 0.0;
    private int handSize;
    private final Circle iconCircle;
    private final Label nameLabel;

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

        this.iconCircle = buildFallbackIcon(colorClass);

        this.nameLabel = new Label(playerName);
        this.nameLabel.getStyleClass().addAll("field-label", "player-name-label");
        this.nameLabel.setMouseTransparent(true);

        fanPane = new Pane();
        fanGroup = new Group(fanPane);

        VBox infoBox = new VBox(2, nameLabel, iconCircle);
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
         * Vector from table center outward toward this opponent seat.
         * 0 degrees = top.
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
         * First calculate the visual rotation of the opponent hand.
         */
        double fanRotation = calculateOpponentHandRotation(seatAngleDegrees);

        /*
         * The player icon/name is the front of the opponent slot.
         * It sits slightly outward from the table center.
         */
        double infoCenterX = outwardX * PLAYER_OUTWARD_DISTANCE;
        double infoCenterY = outwardY * PLAYER_OUTWARD_DISTANCE;

        /*
         * Now place the hand fan BEHIND the player marker according to the same
         * visual direction as the fan rotation.
         *
         * This is the important fix:
         * previously the fan position used only the table-center direction.
         * That made side players drift away from their own hand.
         */
        double fanRad = Math.toRadians(fanRotation);
        double handDirectionX = Math.sin(fanRad);
        double handDirectionY = -Math.cos(fanRad);

        double infoToHandDistance = 76.0;

        double fanCenterX = infoCenterX + handDirectionX * infoToHandDistance;
        double fanCenterY = infoCenterY + handDirectionY * infoToHandDistance;

        infoGroup.setLayoutX(infoCenterX - infoW / 2);
        infoGroup.setLayoutY(infoCenterY - infoH / 2);

        fanGroup.setLayoutX(fanCenterX - fanW / 2);
        fanGroup.setLayoutY(fanCenterY - fanH / 2);

        fanGroup.setRotate(fanRotation);

        /*
         * Player marker stays visually in front of the cards.
         */
        fanGroup.toBack();
        infoGroup.toFront();
    }

    /**
     * Calculates the visual rotation of an opponent hand.
     *
     * <p>The seat angle still controls where the player sits around the table,
     * but the hand rotation is biased downward so the opponent hands visually
     * guide the eye toward the local player's hand and the center piles.</p>
     *
     * @param seatAngle the opponent seat angle in degrees
     * @return the rotation applied to the opponent card fan
     */
    private double calculateOpponentHandRotation(double seatAngle) {
        /*
         * Current inward-facing logic would be:
         *
         *     seatAngle + 180
         *
         * That points exactly toward the center of the table.
         *
         * We instead blend that direction with 180 degrees, which is visually
         * downward in JavaFX rotation terms for these card fans.
         */
        double inwardRotation = normalizeDegrees(seatAngle + 180.0);
        double downwardBias = 180.0;

        /*
         * 0.0 = only inward-facing
         * 1.0 = completely downward-facing
         *
         * Around 0.45 looks natural: side players still keep their own angle,
         * but they no longer look like they point sideways.
         */
        double biasStrength = 0.45;

        return interpolateAngle(inwardRotation, downwardBias, biasStrength);
    }

    /**
     * Interpolates between two angles using the shortest path.
     */
    private double interpolateAngle(double from, double to, double t) {
        double difference = normalizeDegrees(to - from);

        if (difference > 180.0) {
            difference -= 360.0;
        }

        return normalizeDegrees(from + difference * t);
    }

    /**
     * Normalizes an angle to the range [0, 360).
     */
    private double normalizeDegrees(double degrees) {
        double result = degrees % 360.0;
        return result < 0 ? result + 360.0 : result;
    }

    private Node buildIconNode(String colorClass) {
        return buildFallbackIcon(colorClass);
    }

    private Circle buildFallbackIcon(String colorClass) {
        Circle circle = new Circle(ICON_SIZE / 2);
        circle.getStyleClass().addAll(
                "player-icon",
                "player-color-" + colorClass
        );
        return circle;
    }

    public void setActiveTurn(boolean active) {
        getStyleClass().remove("active-turn-player");

        if (active) {
            getStyleClass().add("active-turn-player");
        }
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

    /**
     * Returns the visual center of this opponent's card fan in scene coordinates.
     *
     * <p>This is used as the target point for public draw animations.</p>
     *
     * @return center point of the opponent hand fan in scene coordinates
     */
    public Point2D getHandTargetScenePoint() {
        javafx.geometry.Bounds bounds = fanGroup.localToScene(fanGroup.getBoundsInLocal());

        return new Point2D(
                bounds.getMinX() + bounds.getWidth() / 2,
                bounds.getMinY() + bounds.getHeight() / 2
        );
    }


}
