package ch.unibas.dmi.dbis.cs108.example.prototype.model;


import ch.unibas.dmi.dbis.cs108.example.prototype.controller.ClientSession;

import java.util.ArrayList;
import java.util.List;

public class Lobby {

    private final List<ClientSession> sessions = new ArrayList<>();

    public void addSession(ClientSession session) {
        sessions.add(session);
    }

    public void removeSession(ClientSession session) {
        sessions.remove(session);
    }

    public List<ClientSession> getSessions() {
        return new ArrayList<>(sessions);
    }

    public int getPlayerCount() {
        return sessions.size();
    }

    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();
        for (ClientSession session : sessions) {
            players.add(new Player(session.getPlayerName()));
        }
        return players;
    }
}