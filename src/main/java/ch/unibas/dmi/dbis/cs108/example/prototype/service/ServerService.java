package ch.unibas.dmi.dbis.cs108.example.prototype.service;

import ch.unibas.dmi.dbis.cs108.example.prototype.controller.ClientSession;
import ch.unibas.dmi.dbis.cs108.example.prototype.model.Lobby;
import ch.unibas.dmi.dbis.cs108.example.prototype.model.Player;

import java.util.ArrayList;
import java.util.List;

public class ServerService {

    private final Lobby lobby = new Lobby();
    private boolean gameStarted = false;

    private void log(String message) {
        System.out.println("[SERVER] " + message);
    }

    public synchronized void registerClient(ClientSession session) {
        lobby.addSession(session);
        log("Client connected. Total clients: " + lobby.getPlayerCount());

        session.send("INFO Connected. Current players: " + lobby.getPlayerCount());
        broadcast("INFO A new client connected.");
    }

    public synchronized void unregisterClient(ClientSession session) {
        lobby.removeSession(session);
        log("Client disconnected: " + session.getPlayerName()
                + ". Remaining clients: " + lobby.getPlayerCount());

        broadcast("INFO " + session.getPlayerName() + " disconnected.");
    }

    public synchronized void handleMessage(ClientSession session, Message message) {
        log("Received from " + session.getPlayerName() + ": " + message.type()
                + (message.content().isBlank() ? "" : " -> " + message.content()));

        switch (message.type()) {
            case NAME -> handleName(session, message.content());
            case CHAT -> handleChat(session, message.content());
            case PLAYERS -> handlePlayers(session);
            case START -> handleStart(session);
            case QUIT -> {
                log(session.getPlayerName() + " requested quit.");
                session.send("INFO Goodbye.");
            }
            case UNKNOWN -> {
                log("Unknown command from " + session.getPlayerName() + ": " + message.content());
                session.send("ERROR Unknown command.");
            }
        }
    }

    private void handleName(ClientSession session, String name) {
        if (name == null || name.isBlank()) {
            log("Rejected empty name from " + session.getPlayerName());
            session.send("ERROR Name cannot be empty.");
            return;
        }

        log("Player renamed: " + session.getPlayerName() + " -> " + name);
        session.send("INFO Name set to " + name);
        broadcast("INFO " + name + " joined the lobby.");
    }

    private void handleChat(ClientSession session, String text) {
        log("Chat from " + session.getPlayerName() + ": " + text);
        broadcast("CHAT [" + session.getPlayerName() + "]: " + text);
    }

    private void handlePlayers(ClientSession session) {
        List<String> names = new ArrayList<>();
        for (Player player : lobby.getPlayers()) {
            names.add(player.name());
        }

        log(session.getPlayerName() + " requested player list: " + names);
        session.send("PLAYERS " + String.join(", ", names));
    }

    private void handleStart(ClientSession session) {
        log(session.getPlayerName() + " requested game start.");

        if (gameStarted) {
            log("Start rejected: game already started.");
            session.send("ERROR Game already started.");
            return;
        }

        if (lobby.getPlayerCount() < 2) {
            log("Start rejected: not enough players.");
            session.send("ERROR Need at least 2 players to start.");
            return;
        }

        gameStarted = true;
        log("Game started with " + lobby.getPlayerCount() + " players.");

        broadcast("GAME Starting prototype game...");
        broadcast("GAME Connected players: " + lobby.getPlayerCount());
        broadcast("GAME This is where actual game initialization will go.");
    }

    private void broadcast(String text) {
        log("Broadcast: " + text);
        for (ClientSession session : lobby.getSessions()) {
            session.send(text);
        }
    }
}