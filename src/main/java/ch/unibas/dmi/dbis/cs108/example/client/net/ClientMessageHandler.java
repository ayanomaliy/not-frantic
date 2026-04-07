package ch.unibas.dmi.dbis.cs108.example.client.net;

import ch.unibas.dmi.dbis.cs108.example.service.Message;

/**
 * Receives incoming protocol events from a shared client transport.
 *
 * <p>This interface allows different front ends, such as the terminal client
 * and the JavaFX GUI client, to react to incoming server messages without
 * duplicating socket, heartbeat, or read-loop logic.</p>
 */
public interface ClientMessageHandler {

    /**
     * Handles a validated incoming protocol message from the server.
     *
     * @param message the parsed incoming message
     */
    void onMessage(Message message);

    /**
     * Handles a local client-side status or error message.
     *
     * <p>This is used for messages generated locally by the client transport,
     * for example invalid server messages or local command parsing errors.</p>
     *
     * @param text the user-visible local message
     */
    void onLocalMessage(String text);

    /**
     * Called when the connection is closed or lost.
     *
     * @param reason a human-readable reason
     */
    void onDisconnected(String reason);
}