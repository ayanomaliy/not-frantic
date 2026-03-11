package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.client.ClientMain;

import java.util.Arrays;

public class AppMain {

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
            default -> {
                System.err.println("Unknown mode: " + args[0]);
                printUsage();
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar not-frantic.jar server [port]");
        System.out.println("  java -jar not-frantic.jar client <host:port> [username]");
    }
}