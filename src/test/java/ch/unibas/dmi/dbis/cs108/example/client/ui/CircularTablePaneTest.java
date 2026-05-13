package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CircularTablePaneTest {

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
    void constructor_containsCenterContent() throws Exception {
        runOnFxThreadAndWait(() -> {
            StackPane centerContent = new StackPane();
            CircularTablePane tablePane = new CircularTablePane(centerContent);

            assertTrue(
                    tablePane.getChildren().contains(centerContent),
                    "CircularTablePane should contain the center content node"
            );
        });
    }

    @Test
    void setPlayerSlots_noPlayers_keepsCenterContent() throws Exception {
        runOnFxThreadAndWait(() -> {
            StackPane centerContent = new StackPane();
            CircularTablePane tablePane = new CircularTablePane(centerContent);

            tablePane.setPlayerSlots(List.of(), "Alice");

            assertTrue(
                    tablePane.getChildren().contains(centerContent),
                    "Center content should still be present after setting no player slots"
            );

            long opponentCount = tablePane.getChildren()
                    .stream()
                    .filter(node -> node instanceof OtherPlayerView)
                    .count();

            assertEquals(0, opponentCount);
        });
    }

    @Test
    void setPlayerSlots_emptyListDoesNotCrashAfterLayout() throws Exception {
        runOnFxThreadAndWait(() -> {
            StackPane centerContent = new StackPane();
            CircularTablePane tablePane = new CircularTablePane(centerContent);

            tablePane.resize(1000, 420);
            tablePane.setPlayerSlots(List.of(), "Alice");
            tablePane.layout();

            assertTrue(tablePane.getChildren().contains(centerContent));
        });
    }

    @Test
    void setPlayerSlots_canBeCalledRepeatedlyWithEmptyList() throws Exception {
        runOnFxThreadAndWait(() -> {
            StackPane centerContent = new StackPane();
            CircularTablePane tablePane = new CircularTablePane(centerContent);

            tablePane.setPlayerSlots(List.of(), "Alice");
            tablePane.setPlayerSlots(List.of(), "Bob");
            tablePane.setPlayerSlots(List.of(), "Charlie");

            long opponentCount = tablePane.getChildren()
                    .stream()
                    .filter(node -> node instanceof OtherPlayerView)
                    .count();

            assertEquals(0, opponentCount);
            assertTrue(tablePane.getChildren().contains(centerContent));
        });
    }

    @Test
    void centerContentRemainsAfterResizeAndLayout() throws Exception {
        runOnFxThreadAndWait(() -> {
            StackPane centerContent = new StackPane();
            CircularTablePane tablePane = new CircularTablePane(centerContent);

            tablePane.resize(1200, 520);
            tablePane.setPlayerSlots(List.of(), "Alice");
            tablePane.layout();

            assertTrue(tablePane.getChildren().contains(centerContent));
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