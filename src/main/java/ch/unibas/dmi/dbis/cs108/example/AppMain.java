package ch.unibas.dmi.dbis.cs108.example;


import ch.unibas.dmi.dbis.cs108.example.client.ClientMain;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GuiMain;

import java.util.Arrays;

/**
 * Unified entry point for the Frantic^-1 application.
 *
 * <p>This launcher allows the executable JAR to start either the server
 * or the client depending on the first command-line argument.</p>
 *
 * <p>Supported modes:</p>
 * <ul>
 *   <li>{@code server} – starts {@link ServerMain}</li>
 *   <li>{@code client} – starts {@link ClientMain}</li>
 * </ul>
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code java -jar not-frantic.jar server}</li>
 *   <li>{@code java -jar not-frantic.jar server 5555}</li>
 *   <li>{@code java -jar not-frantic.jar client localhost:5555}</li>
 *   <li>{@code java -jar not-frantic.jar client localhost:5555 Alice}</li>
 * </ul>
 */
public class AppMain {

    /**
     * Starts the application in either server or client mode.
     *
     * <p>The first argument determines the mode:
     * {@code server} starts the TCP server,
     * {@code client} starts the TCP client.
     * Any remaining arguments are forwarded unchanged to the selected
     * main class.</p>
     *
     * <p>If no mode is given, or if the mode is unknown, usage information
     * is printed.</p>
     *
     * @param args command-line arguments where the first argument selects
     *             the mode and the remaining arguments are passed to the
     *             corresponding entry point
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].trim().toLowerCase();
        String[] forwardedArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (mode) {
            case "server" -> ServerMain.main(forwardedArgs);
            case "client" -> ClientMain.main(forwardedArgs);
            case "gui" -> GuiMain.main(forwardedArgs);
            default -> {
                System.err.println("Unknown mode: " + args[0]);
                printUsage();
            }
        }
    }

    /**
     * Prints command-line usage information for the executable JAR.
     *
     * <p>This method describes how to start the application in
     * server mode or client mode.</p>
     */
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar not-frantic.jar server [port]");
        System.out.println("  java -jar not-frantic.jar client <host:port> [username]");
    }
}