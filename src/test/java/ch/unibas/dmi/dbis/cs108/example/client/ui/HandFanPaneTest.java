package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class HandFanPaneTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX platform did not start");
    }

    @Test
    void getHandFanPane_returnsPane() throws Exception {
        runOnFxThreadAndWait(() -> {
            GameView view = new GameView();

            assertNotNull(view.getHandFanPane());
            assertInstanceOf(Pane.class, view.getHandFanPane());
        });
    }

    @Test
    void getHandFanPane_prefHeight_is205() throws Exception {
        runOnFxThreadAndWait(() -> {
            GameView view = new GameView();

            assertEquals(205, view.getHandFanPane().getPrefHeight(), 0.001);
        });
    }

    @Test
    void getHandFanPane_minHeight_is205() throws Exception {
        runOnFxThreadAndWait(() -> {
            GameView view = new GameView();

            assertEquals(205, view.getHandFanPane().getMinHeight(), 0.001);
        });
    }

    @Test
    void getHandFanPane_isInitiallyEmpty() throws Exception {
        runOnFxThreadAndWait(() -> {
            GameView view = new GameView();

            assertTrue(view.getHandFanPane().getChildren().isEmpty());
        });
    }

    @Test
    void getHandFanPane_sameInstanceReturnedEveryTime() throws Exception {
        runOnFxThreadAndWait(() -> {
            GameView view = new GameView();

            Pane first = view.getHandFanPane();
            Pane second = view.getHandFanPane();

            assertSame(first, second);
        });
    }

    private void runOnFxThreadAndWait(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] thrown = new Throwable[1];

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                thrown[0] = t;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX task timed out");

        if (thrown[0] != null) {
            if (thrown[0] instanceof Exception exception) {
                throw exception;
            }
            if (thrown[0] instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(thrown[0]);
        }
    }
}