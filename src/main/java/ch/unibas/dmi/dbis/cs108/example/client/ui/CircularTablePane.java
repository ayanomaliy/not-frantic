package ch.unibas.dmi.dbis.cs108.example.client.ui;

import ch.unibas.dmi.dbis.cs108.example.client.ClientState;
import ch.unibas.dmi.dbis.cs108.example.client.assets.AssetRegistry;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A circular table layout that places opponent player views around a ring and
 * centers the core game controls (piles, status, end-turn button) in the middle.
 *
 * <p>The 360° around the table is divided as follows:</p>
 * <ul>
 *   <li>120°–240° (bottom arc) — reserved for the local player's hand area</li>
 *   <li>240°–120° through 0° (top 240° arc) — distributed evenly among opponents</li>
 * </ul>
 *
 * <p>Angle formula: {@code interval = 240 / (numOthers + 1)},
 * {@code angle_i = (240 + i * interval) % 360} for i = 1..numOthers.
 * Example: 2 opponents → interval = 80° → angles 320° and 40°.</p>
 *
 * <p>Must be created on the JavaFX Application Thread.</p>
 */
public class CircularTablePane extends Pane {

    private static final double RADIUS_X_FACTOR = 0.39;
    private static final double RADIUS_Y_FACTOR = 0.34;

    private static final double TOP_SLOT_LIFT = 34.0;
    private static final double SIDE_SLOT_PUSH = 34.0;

    private AssetRegistry registry;

    private final Node centerContent;
    private final List<OtherPlayerView> slots = new ArrayList<>();
    private final List<Double> slotAngles = new ArrayList<>();
    private final Map<String, OtherPlayerView> playerSlots = new LinkedHashMap<>();

    /**
     * Creates a circular table pane with the given center content.
     *
     * @param centerContent the node placed in the center of the table (piles, labels, buttons)
     */
    public CircularTablePane(Node centerContent) {
        this.centerContent = centerContent;
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        getChildren().add(centerContent);
    }


    /**
     * Replaces all opponent player slots with new ones derived from {@code others}.
     *
     * @param others        the opponents to display (local player already excluded)
     * @param localUsername the local player's username (reserved for future use)
     */
    public void setPlayerSlots(List<ClientState.PlayerInfo> others, String localUsername) {
        getChildren().removeAll(slots);
        slots.clear();
        slotAngles.clear();
        playerSlots.clear();

        int n = others.size();
        if (n > 0) {
            double startAngle = 285.0;
            double endAngle = 75.0;
            double arc = 150.0;

            double interval = arc / Math.max(1, n - 1);

            for (int i = 0; i < n; i++) {
                ClientState.PlayerInfo info = others.get(i);

                double angle;
                if (n == 1) {
                    angle = 0.0;
                } else {
                    angle = (startAngle + i * interval) % 360;
                }

                OtherPlayerView slot = new OtherPlayerView(info.name(), info.handSize(), info.color(), registry);
                slot.setSeatAngle(angle);

                slots.add(slot);
                slotAngles.add(angle);
                playerSlots.put(info.name(), slot);
                getChildren().add(slot);
            }
        }

        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();

        if (w <= 0 || h <= 0) {
            return;
        }

        double cx = w / 2;
        double cy = h / 2;

        /*
         * Oval seat layout:
         * The game area is wide, so opponent seats need more horizontal spacing.
         */
        double radiusX = w * RADIUS_X_FACTOR;
        double radiusY = h * RADIUS_Y_FACTOR;
        

        positionCenter(cx, cy);

        for (int i = 0; i < slots.size(); i++) {
            OtherPlayerView slot = slots.get(i);
            double angle = slotAngles.get(i);
            double rad = Math.toRadians(angle);

            double anchorX = cx + radiusX * Math.sin(rad);
            double anchorY = cy - radiusY * Math.cos(rad);

            double topness = Math.max(0, Math.cos(rad));
            anchorY -= topness * TOP_SLOT_LIFT;

            double sideness = Math.abs(Math.sin(rad));
            anchorX += Math.signum(Math.sin(rad)) * sideness * SIDE_SLOT_PUSH;

            slot.setLayoutX(anchorX);
            slot.setLayoutY(anchorY);
        }
    }

    private void positionCenter(double cx, double cy) {
        if (centerContent instanceof Region region) {
            double prefW = region.prefWidth(-1);
            double prefH = region.prefHeight(-1);
            if (prefW > 0 && prefH > 0) {
                region.setLayoutX(cx - prefW / 2);
                region.setLayoutY(cy - prefH / 2);
                region.resize(prefW, prefH);
                return;
            }
        }
        Bounds cb = centerContent.getBoundsInLocal();
        double cw = cb.getWidth();
        double ch = cb.getHeight();
        centerContent.setLayoutX(cx - (cw > 0 ? cw / 2 : 0));
        centerContent.setLayoutY(cy - (ch > 0 ? ch / 2 : 0));
    }

    /** Sets the asset registry used when creating player slots for card reveals. */
    public void setRegistry(AssetRegistry registry) {
        this.registry = registry;
    }

    /** Returns an unmodifiable view of the current player slot map (name → view). */
    public Map<String, OtherPlayerView> getPlayerSlots() {
        return Collections.unmodifiableMap(playerSlots);
    }

    /** Package-private — used by tests to verify angle calculations. */
    List<Double> getSlotAngles() {
        return Collections.unmodifiableList(slotAngles);
    }

    public void highlightCurrentPlayer(String currentPlayerName) {
        for (Map.Entry<String, OtherPlayerView> entry : playerSlots.entrySet()) {
            boolean isActive = entry.getKey().equals(currentPlayerName);
            entry.getValue().setActiveTurn(isActive);
        }
    }
}
