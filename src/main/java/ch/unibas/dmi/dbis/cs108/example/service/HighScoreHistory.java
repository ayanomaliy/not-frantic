package ch.unibas.dmi.dbis.cs108.example.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HighScoreHistory {

    private final Path file;

    public HighScoreHistory(Path file) {
        this.file = file;
    }

    public synchronized void appendFinishedGame(
            String lobbyId,
            String winner,
            Map<String, Integer> finalScores
    ) {
        String timestamp = LocalDateTime.now().toString();

        String scoresPart = finalScores.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));

        String line = timestamp + ";" + lobbyId + ";" + winner + ";" + scoresPart;

        try {
            Files.writeString(
                    file,
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new IllegalStateException("Could not write highscores file.", e);
        }
    }

    public synchronized List<GameHistoryEntry> readAllGames() {
        return List.of();
    }

    public synchronized List<LeaderboardRow> buildLeaderboard() {
        return List.of();
    }

    public record GameHistoryEntry(
            String timestamp,
            String lobbyId,
            String winner,
            Map<String, Integer> finalScores
    ) {
    }

    public record LeaderboardRow(
            String playerName,
            int wins,
            int gamesPlayed,
            int totalPenaltyPoints
    ) {
    }
}