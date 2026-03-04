package ch.unibas.dmi.dbis.cs108.example.prototype.client;

import java.io.BufferedReader;

public class ClientReader implements Runnable {

    private final BufferedReader serverIn;

    public ClientReader(BufferedReader serverIn) {
        this.serverIn = serverIn;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = serverIn.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            System.err.println("Disconnected from server.");
        }
    }
}