package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;

/**
 * Decorative contour frame for the hand area.
 *
 * <p>This reproduces the visual language of the Frantic^-1 logo:
 * gradient contour, small sparkles, and a soft glow. It is drawn
 * directly in JavaFX so it can resize with the hand panel.</p>
 */
public class FancyHandFrame extends Region {

    private final Path upperFrame = new Path();
    private final Path lowerFrame = new Path();

    private final Group topSparkle = createSparkle();
    private final Group bottomSparkle = createSparkle();

    public FancyHandFrame() {
        setMouseTransparent(true);

        configurePath(upperFrame);
        configurePath(lowerFrame);

        DropShadow glow = new DropShadow();
        glow.setRadius(10);
        glow.setColor(Color.rgb(255, 92, 115, 0.28));
        setEffect(glow);

        getChildren().addAll(upperFrame, lowerFrame, topSparkle, bottomSparkle);
    }

    private void configurePath(Path path) {
        path.setFill(null);
        path.setStrokeWidth(3.0);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
    }

    private Group createSparkle() {
        Polygon diamondVertical = new Polygon();
        Polygon diamondHorizontal = new Polygon();

        Paint sparkleFill = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFBB59")),
                new Stop(1, Color.web("#FF4A8C"))
        );

        diamondVertical.setFill(sparkleFill);
        diamondHorizontal.setFill(sparkleFill);
        diamondHorizontal.setOpacity(0.92);

        return new Group(diamondVertical, diamondHorizontal);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 760;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 300;
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();

        if (w <= 0 || h <= 0) {
            return;
        }

        double left = 18;
        double right = w - 18;
        double top = 18;
        double bottom = h - 18;

        double radius = 30;
        double middleX = w / 2.0;
        double gap = 28; // gap around sparkle

        upperFrame.setStroke(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#7F35F7")),
                new Stop(0.30, Color.web("#9C39FF")),
                new Stop(0.70, Color.web("#FF7C48")),
                new Stop(1.00, Color.web("#FF2C8D"))
        ));

        lowerFrame.setStroke(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#7F35F7")),
                new Stop(0.40, Color.web("#A83DFF")),
                new Stop(1.00, Color.web("#FF52A2"))
        ));

        upperFrame.getElements().setAll(
                new MoveTo(left + radius, top),
                new LineTo(middleX - gap, top),

                new MoveTo(middleX + gap, top),
                new LineTo(right - radius, top),
                new QuadCurveTo(right, top, right, top + radius),
                new LineTo(right, bottom - radius),
                new QuadCurveTo(right, bottom, right - radius, bottom),
                new LineTo(middleX + gap, bottom)
        );

        lowerFrame.getElements().setAll(
                new MoveTo(middleX - gap, bottom),
                new LineTo(left + radius, bottom),
                new QuadCurveTo(left, bottom, left, bottom - radius),
                new LineTo(left, top + radius),
                new QuadCurveTo(left, top, left + radius, top),
                new LineTo(middleX - gap, top)
        );

        layoutSparkle(topSparkle, middleX, top);
        layoutSparkle(bottomSparkle, middleX, bottom);
    }

    private void layoutSparkle(Group sparkle, double cx, double cy) {
        Polygon vertical = (Polygon) sparkle.getChildren().get(0);
        Polygon horizontal = (Polygon) sparkle.getChildren().get(1);

        double s1 = 8;
        vertical.getPoints().setAll(
                cx, cy - s1,
                cx + s1, cy,
                cx, cy + s1,
                cx - s1, cy
        );

        double s2 = 12;
        horizontal.getPoints().setAll(
                cx - s2, cy,
                cx, cy - s2,
                cx + s2, cy,
                cx, cy + s2
        );
    }
}