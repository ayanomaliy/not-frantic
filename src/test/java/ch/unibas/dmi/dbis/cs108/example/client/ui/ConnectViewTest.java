package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ConnectView}.
 */
class ConnectViewTest {

    /**
     * Starts the JavaFX runtime once for all tests.
     *
     * @throws Exception if startup coordination fails
     */
    @BeforeAll
    static void startJavaFx() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit already started.
        }
    }

    /**
     * Verifies constructor defaults and basic control setup.
     */
    @Test
    void constructorInitializesDefaultValues() {
        ConnectView view = new ConnectView();

        assertEquals("localhost", view.getHostField().getText());
        assertEquals("5555", view.getPortField().getText());
        assertEquals(System.getProperty("user.name", "Player"), view.getUsernameField().getText());

        assertEquals("localhost", view.getHostField().getPromptText());
        assertEquals("5555", view.getPortField().getPromptText());
        assertEquals("Player", view.getUsernameField().getPromptText());

        assertTrue(view.getStyleClass().contains("screen"));
        assertTrue(view.getStyleClass().contains("connect-screen"));
    }

    /**
     * Verifies control getters return non-null controls of the expected type.
     */
    @Test
    void gettersReturnControls() {
        ConnectView view = new ConnectView();

        TextField hostField = view.getHostField();
        TextField portField = view.getPortField();
        TextField usernameField = view.getUsernameField();
        Button connectButton = view.getConnectButton();
        Label statusLabel = view.getStatusLabel();

        assertNotNull(hostField);
        assertNotNull(portField);
        assertNotNull(usernameField);
        assertNotNull(connectButton);
        assertNotNull(statusLabel);
    }

    /**
     * Verifies the connect button and status label are initialized as expected.
     */
    @Test
    void constructorInitializesButtonAndStatusLabel() {
        ConnectView view = new ConnectView();

        assertEquals("Connect", view.getConnectButton().getText());
        assertEquals("", view.getStatusLabel().getText());

        assertTrue(view.getConnectButton().getStyleClass().contains("frantic-button"));
        assertTrue(view.getConnectButton().getStyleClass().contains("primary-button"));
        assertTrue(view.getStatusLabel().getStyleClass().contains("status-label"));
    }

    /**
     * Verifies text fields have the shared style class.
     */
    @Test
    void textFieldsHaveExpectedStyleClass() {
        ConnectView view = new ConnectView();

        assertTrue(view.getHostField().getStyleClass().contains("frantic-text-field"));
        assertTrue(view.getPortField().getStyleClass().contains("frantic-text-field"));
        assertTrue(view.getUsernameField().getStyleClass().contains("frantic-text-field"));
    }
}