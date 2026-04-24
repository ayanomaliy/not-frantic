package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HandFanPaneTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException ignored) {
            // already started
        }
    }

    private GameView createView() throws Exception {
        AtomicReference<GameView> ref = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(new GameView());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(err.get(), "GameView constructor threw: " + err.get());
        return ref.get();
    }

    @Test
    void getHandFanPane_returnsNonNull() throws Exception {
        GameView view = createView();
        assertNotNull(view.getHandFanPane());
    }

    @Test
    void getHandFanPane_isPane() throws Exception {
        GameView view = createView();
        assertInstanceOf(Pane.class, view.getHandFanPane());
    }

    @Test
    void getHandFanPane_prefHeight_is180() throws Exception {
        GameView view = createView();
        assertEquals(180.0, view.getHandFanPane().getPrefHeight(), 0.001);
    }

    @Test
    void getHandFanPane_startsEmpty() throws Exception {
        GameView view = createView();
        assertEquals(0, view.getHandFanPane().getChildren().size());
    }

    @Test
    void getPlayerHandPane_stillReturnsLegacyFlowPane() throws Exception {
        GameView view = createView();
        assertNotNull(view.getPlayerHandPane());
    }

    private static double fanAngle(int i, int n) {
        return (n > 1) ? -30.0 + i * (60.0 / (n - 1)) : 0.0;
    }

    @Test
    void fanAngle_singleCard_isZero() {
        assertEquals(0.0, fanAngle(0, 1), 0.001);
    }

    @Test
    void fanAngle_fiveCards_firstAndLastAreSymmetric() {
        assertEquals(-30.0, fanAngle(0, 5), 0.001);
        assertEquals(30.0,  fanAngle(4, 5), 0.001);
    }

    @Test
    void fanAngle_tenCards_firstAndLastAreSymmetric() {
        assertEquals(-30.0, fanAngle(0,  10), 0.001);
        assertEquals(30.0,  fanAngle(9, 10), 0.001);
    }

    @Test
    void fanAngle_fifteenCards_firstAndLastAreSymmetric() {
        assertEquals(-30.0, fanAngle(0,  15), 0.001);
        assertEquals(30.0,  fanAngle(14, 15), 0.001);
    }

    @Test
    void fanX_centerCard_isAtCx() {
        double arcRadius = 500.0;
        int n = 5;
        double angle = fanAngle(2, n); // center index
        double cx = 400.0;
        double x = cx + arcRadius * Math.sin(Math.toRadians(angle));
        assertEquals(cx, x, 0.01);
    }

    @Test
    void fanY_centerCard_isAtPaneBottom() {
        double arcRadius = 500.0;
        double paneH = 180.0;
        int n = 5;
        double angle = fanAngle(2, n); // center index
        double y = paneH + arcRadius * (1 - Math.cos(Math.toRadians(angle)));
        assertEquals(paneH, y, 0.01);
    }
}
