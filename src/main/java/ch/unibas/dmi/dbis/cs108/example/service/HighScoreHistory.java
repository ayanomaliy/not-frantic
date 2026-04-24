package ch.unibas.dmi.dbis.cs108.example.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

        String line = "timestamp=" + timestamp
                + ";lobby=" + lobbyId
                + ";winner=" + winner
                + ";scores=" + scoresPart;

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
        if (!Files.exists(file)) {
            return List.of();
        }

        try {
            List<GameHistoryEntry> entries = new ArrayList<>();

            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.isBlank()) {
                    continue;
                }

                String timestamp = "";
                String lobbyId = "";
                String winner = "";
                Map<String, Integer> finalScores = new LinkedHashMap<>();

                String[] fields = trimmed.split(";");
                for (String field : fields) {
                    String[] kv = field.split("=", 2);
                    if (kv.length != 2) {
                        continue;
                    }

                    String key = kv[0].trim();
                    String value = kv[1].trim();

                    switch (key) {
                        case "timestamp" -> timestamp = value;
                        case "lobby" -> lobbyId = value;
                        case "winner" -> winner = value;
                        case "scores" -> {
                            if (!value.isBlank()) {
                                String[] scoreEntries = value.split(",");
                                for (String scoreEntry : scoreEntries) {
                                    String[] scoreParts = scoreEntry.split(":", 2);
                                    if (scoreParts.length != 2) {
                                        continue;
                                    }

                                    String playerName = scoreParts[0].trim();
                                    try {
                                        int points = Integer.parseInt(scoreParts[1].trim());
                                        finalScores.put(playerName, points);
                                    } catch (NumberFormatException ignored) {
                                        // skip malformed score
                                    }
                                }
                            }
                        }
                        default -> {
                            // ignore unknown fields for forward compatibility
                        }
                    }
                }

                if (!winner.isBlank() && !finalScores.isEmpty()) {
                    entries.add(new GameHistoryEntry(timestamp, lobbyId, winner, finalScores));
                }
            }

            return entries;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read highscores file.", e);
        }
    }

    public synchronized List<LeaderboardRow> buildLeaderboard() {
        Map<String, MutableStats> statsByPlayer = new LinkedHashMap<>();

        for (GameHistoryEntry entry : readAllGames()) {
            for (Map.Entry<String, Integer> score : entry.finalScores().entrySet()) {
                String player = score.getKey();
                int penaltyPoints = score.getValue();

                MutableStats stats = statsByPlayer.computeIfAbsent(player, p -> new MutableStats());
                stats.gamesPlayed++;
                stats.totalPenaltyPoints += penaltyPoints;

                if (player.equals(entry.winner())) {
                    stats.wins++;
                }
            }
        }

        return statsByPlayer.entrySet().stream()
                .map(entry -> new LeaderboardRow(
                        entry.getKey(),
                        entry.getValue().wins,
                        entry.getValue().gamesPlayed,
                        entry.getValue().totalPenaltyPoints
                ))
                .sorted(Comparator
                        .comparingInt(LeaderboardRow::wins).reversed()
                        .thenComparingInt(LeaderboardRow::totalPenaltyPoints)
                        .thenComparing(LeaderboardRow::playerName))
                .toList();
    }

    private static final class MutableStats {
        int wins;
        int gamesPlayed;
        int totalPenaltyPoints;
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


    public synchronized List<String> buildLeaderboardDisplayRows() {
        return buildLeaderboard().stream()
                .map(row -> row.playerName()
                        + " | Wins: " + row.wins()
                        + " | Games: " + row.gamesPlayed()
                        + " | Points: " + row.totalPenaltyPoints())
                .toList();
    }
}