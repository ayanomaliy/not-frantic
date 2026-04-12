package ch.unibas.dmi.dbis.cs108.example.dev;

import ch.unibas.dmi.dbis.cs108.example.model.Lobby;
import ch.unibas.dmi.dbis.cs108.example.model.game.Card;
import ch.unibas.dmi.dbis.cs108.example.model.game.CardColor;
import ch.unibas.dmi.dbis.cs108.example.model.game.GamePhase;
import ch.unibas.dmi.dbis.cs108.example.model.game.GameState;
import ch.unibas.dmi.dbis.cs108.example.model.game.PlayerGameState;
import ch.unibas.dmi.dbis.cs108.example.model.game.SpecialEffect;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Central manager for lobby-based dev mode scenarios.
 *
 * <p>This class has two responsibilities:</p>
 * <ul>
 *   <li>Remembering whether dev mode is enabled for a lobby and which scenario
 *       should be used.</li>
 *   <li>Loading a scenario configuration file from the classpath and applying it
 *       to an already initialized {@link GameState}.</li>
 * </ul>
 *
 * <p>The normal round initialization still happens through the regular game
 * initializer. Dev mode then selectively overwrites parts of the state such as
 * the top discard card, player hands, current player, phase, and optional
 * requested color or number. This keeps the feature isolated and minimizes
 * interference with the normal game flow.</p>
 *
 * <p>Scenario files are expected at:</p>
 * <pre>{@code /devmode/<scenarioName>.properties}</pre>
 */
public final class DevModeManager {

    /**
     * Prefix inside resources where dev-mode scenario files are stored.
     */
    private static final String RESOURCE_PREFIX = "/devmode/";

    /**
     * File extension for dev-mode scenario files.
     */
    private static final String RESOURCE_SUFFIX = ".properties";

    private DevModeManager() {
    }

    /**
     * Enables dev mode for the given lobby and stores the scenario name that
     * should be applied at the next game start.
     *
     * @param lobby the lobby for which dev mode should be enabled
     * @param scenarioName the scenario name, without path or file extension
     */
    public static void enableForLobby(Lobby lobby, String scenarioName) {
        Objects.requireNonNull(lobby, "lobby must not be null");

        String cleanedScenario = scenarioName == null || scenarioName.isBlank()
                ? "default"
                : scenarioName.trim();

        lobby.setDevModeEnabled(true);
        lobby.setDevScenario(cleanedScenario);
    }

    /**
     * Disables dev mode for the given lobby and clears the stored scenario name.
     *
     * @param lobby the lobby for which dev mode should be disabled
     */
    public static void disableForLobby(Lobby lobby) {
        Objects.requireNonNull(lobby, "lobby must not be null");
        lobby.setDevModeEnabled(false);
        lobby.setDevScenario("none");
    }

    /**
     * Applies the configured dev-mode scenario to the lobby's current game state,
     * if dev mode is enabled.
     *
     * <p>If dev mode is disabled, this method does nothing.</p>
     *
     * @param lobby the lobby whose game state should be modified
     * @throws IllegalArgumentException if the scenario file is malformed
     * @throws IllegalStateException if the scenario file cannot be found or read
     */
    public static void applyIfEnabled(Lobby lobby) {
        Objects.requireNonNull(lobby, "lobby must not be null");

        if (!lobby.isDevModeEnabled()) {
            return;
        }

        GameState state = lobby.getGameState();
        if (state == null) {
            throw new IllegalStateException("Cannot apply dev mode without an active game state.");
        }

        Properties properties = loadScenarioProperties(lobby.getDevScenario());
        applyScenarioToState(state, properties);
    }

    /**
     * Loads the scenario properties from the classpath.
     *
     * @param scenarioName the scenario name without extension
     * @return the loaded properties
     * @throws IllegalStateException if the file is missing or cannot be read
     */
    private static Properties loadScenarioProperties(String scenarioName) {
        String resourcePath = RESOURCE_PREFIX + scenarioName + RESOURCE_SUFFIX;

        try (InputStream in = DevModeManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Dev-mode scenario not found: " + resourcePath);
            }

            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read dev-mode scenario: " + resourcePath, e);
        }
    }

    /**
     * Applies one scenario configuration to the given game state.
     *
     * @param state the game state to modify
     * @param properties the loaded scenario properties
     */
    private static void applyScenarioToState(GameState state, Properties properties) {
        clearAllHands(state);
        clearPendingEffects(state);

        applyTopCard(state, properties);
        applyPlayerHands(state, properties);
        applyCurrentPlayer(state, properties);
        applyPhase(state, properties);
        applyRequestedColor(state, properties);
        applyRequestedNumber(state, properties);
        applyPendingEffects(state, properties);
        applyPendingEffectTarget(state, properties);
    }

    /**
     * Removes all hand cards from all players before the configured hands are applied.
     *
     * @param state the game state to reset
     */
    private static void clearAllHands(GameState state) {
        for (PlayerGameState player : state.getPlayerOrder()) {
            player.getHand().clear();
            player.setHasPlayedThisTurn(false);
            player.setHasDrawnThisTurn(false);
            player.setSkipped(false);
        }
    }

    /**
     * Clears pending-effect related state.
     *
     * @param state the game state to reset
     */
    private static void clearPendingEffects(GameState state) {
        state.getPendingEffects().clear();
        state.setPendingEffectTarget(null);
        state.setRequestedColor(null);
        state.setRequestedNumber(null);
        state.setActiveEventCard(null);
    }

    /**
     * Applies the configured top discard card.
     *
     * <p>Property key: {@code topCard}</p>
     *
     * @param state the game state to modify
     * @param properties the scenario properties
     */
    private static void applyTopCard(GameState state, Properties properties) {
        String rawTopCard = properties.getProperty("topCard");
        if (rawTopCard == null || rawTopCard.isBlank()) {
            return;
        }

        int cardId = parseRequiredInt(rawTopCard, "topCard");
        Card card = removeCardFromAnyPile(state, cardId);

        state.getDiscardPile().clear();
        state.getDiscardPile().push(card);
    }

    /**
     * Applies configured hands for players.
     *
     * <p>Property keys:</p>
     * <pre>
     * player.&lt;name&gt;.hand=1,2,3
     * </pre>
     *
     * @param state the game state to modify
     * @param properties the scenario properties
     */
    private static void applyPlayerHands(GameState state, Properties properties) {
        for (int i = 0; i < state.getPlayerOrder().size(); i++) {
            PlayerGameState player = state.getPlayerOrder().get(i);

            String aliasKey = "player" + (i + 1) + ".hand";
            String rawHand = properties.getProperty(aliasKey, "").trim();

            if (rawHand.isBlank()) {
                String legacyKey = "player." + player.getPlayerName() + ".hand";
                rawHand = properties.getProperty(legacyKey, "").trim();
            }

            if (rawHand.isBlank()) {
                continue;
            }

            for (String token : rawHand.split(",")) {
                String trimmed = token.trim();
                if (trimmed.isBlank()) {
                    continue;
                }

                int cardId = parseRequiredInt(trimmed, aliasKey);
                Card card = removeCardFromAnyPile(state, cardId);
                player.addCard(card);
            }
        }
    }

    /**
     * Applies the configured current player.
     *
     * <p>Property key: {@code currentPlayer}</p>
     *
     * @param state the game state to modify
     * @param properties the scenario properties
     */
    private static void applyCurrentPlayer(GameState state, Properties properties) {
        String rawCurrentPlayer = properties.getProperty("currentPlayer");
        if (rawCurrentPlayer == null || rawCurrentPlayer.isBlank()) {
            return;
        }

        String resolvedName = resolvePlayerReference(state, rawCurrentPlayer.trim(), "currentPlayer");

        for (int i = 0; i < state.getPlayerOrder().size(); i++) {
            if (state.getPlayerOrder().get(i).getPlayerName().equals(resolvedName)) {
                state.setCurrentPlayerIndex(i);
                return;
            }
        }

        throw new IllegalArgumentException("Unknown currentPlayer in dev config: " + rawCurrentPlayer);
    }

    /**
     * Applies the configured game phase.
     *
     * <p>Property key: {@code phase}</p>
     *
     * @param state the game state to modify
     * @param properties the scenario properties
     */
    private static void applyPhase(GameState state, Properties properties) {
        String rawPhase = properties.getProperty("phase");
        if (rawPhase == null || rawPhase.isBlank()) {
            return;
        }

        try {
            state.setPhase(GamePhase.valueOf(rawPhase.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid phase in dev config: " + rawPhase, e);
        }
    }

    /**
     * Applies the configured requested color.
     *
     * <p>Property key: {@code requestedColor}</p>
     *
     * @param state the game state to modify
     * @param properties the scenario properties
     */
    private static void applyRequestedColor(GameState state, Properties properties) {
        String rawColor = properties.getProperty("requestedColor");
        if (rawColor == null || rawColor.isBlank()) {
            return;
        }

        try {
            state.setRequestedColor(CardColor.valueOf(rawColor.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid requestedColor in dev config: " + rawColor, e);
        }
    }

    /**
     * Applies the configured requested number.
     *
     * <p>Property key: {@code requestedNumber}</p>
     *
     * @param state the game state to modify
     * @param properties the scenario properties
     */
    private static void applyRequestedNumber(GameState state, Properties properties) {
        String rawNumber = properties.getProperty("requestedNumber");
        if (rawNumber == null || rawNumber.isBlank()) {
            return;
        }

        state.setRequestedNumber(parseRequiredInt(rawNumber, "requestedNumber"));
    }

    /**
     * Applies pending special effects from the config.
     *
     * <p>Property key:</p>
     * <pre>
     * pendingEffects=SECOND_CHANCE,SKIP
     * </pre>
     *
     * @param state the game state to modify
     * @param properties the scenario properties
     */
    private static void applyPendingEffects(GameState state, Properties properties) {
        String rawEffects = properties.getProperty("pendingEffects");
        if (rawEffects == null || rawEffects.isBlank()) {
            return;
        }

        List<SpecialEffect> parsedEffects = new ArrayList<>();

        for (String token : rawEffects.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            try {
                parsedEffects.add(SpecialEffect.valueOf(trimmed.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid pending effect in dev config: " + trimmed, e);
            }
        }

        /*
         * Push in reverse order because the deque acts as a stack and the last
         * pushed element becomes the top pending effect.
         */
        for (int i = parsedEffects.size() - 1; i >= 0; i--) {
            state.getPendingEffects().push(parsedEffects.get(i));
        }
    }

    /**
     * Applies the optional pending effect target.
     *
     * <p>Property key: {@code pendingEffectTarget}</p>
     *
     * @param state the game state to modify
     * @param properties the scenario properties
     */
    private static void applyPendingEffectTarget(GameState state, Properties properties) {
        String rawTarget = properties.getProperty("pendingEffectTarget");
        if (rawTarget == null || rawTarget.isBlank()) {
            return;
        }

        String resolvedName = resolvePlayerReference(state, rawTarget.trim(), "pendingEffectTarget");
        state.setPendingEffectTarget(resolvedName);
    }


    private static String resolvePlayerReference(GameState state, String reference, String key) {
        String trimmed = reference.trim();

        if (trimmed.matches("player\\d+")) {
            int index = Integer.parseInt(trimmed.substring("player".length())) - 1;

            if (index < 0 || index >= state.getPlayerOrder().size()) {
                throw new IllegalArgumentException(
                        "Invalid player reference for " + key + ": " + reference
                );
            }

            return state.getPlayerOrder().get(index).getPlayerName();
        }

        for (PlayerGameState player : state.getPlayerOrder()) {
            if (player.getPlayerName().equals(trimmed)) {
                return trimmed;
            }
        }

        throw new IllegalArgumentException("Unknown player reference for " + key + ": " + reference);
    }

    /**
     * Removes one specific card id from the draw pile or event pile and returns it.
     *
     * <p>The discard pile is intentionally not searched here because the dev-mode
     * setup first clears and replaces the discard top explicitly. All cards that
     * should end up in player hands or on top of discard are therefore expected
     * to come from the undealt piles after normal initialization.</p>
     *
     * @param state the source state
     * @param cardId the card id to remove
     * @return the removed card
     * @throws IllegalArgumentException if the card cannot be found
     */
    private static Card removeCardFromAnyPile(GameState state, int cardId) {
        Card card = removeCardById(state.getDrawPile(), cardId);
        if (card != null) {
            return card;
        }

        card = removeCardById(state.getEventPile(), cardId);
        if (card != null) {
            return card;
        }

        throw new IllegalArgumentException("Card " + cardId + " not found in remaining piles.");
    }

    /**
     * Removes and returns one card with the given id from the deque.
     *
     * @param pile the pile to search
     * @param cardId the desired card id
     * @return the removed card, or {@code null} if not present
     */
    private static Card removeCardById(Deque<Card> pile, int cardId) {
        Deque<Card> rebuilt = new ArrayDeque<>();
        Card found = null;

        while (!pile.isEmpty()) {
            Card current = pile.pop();
            if (found == null && current.id() == cardId) {
                found = current;
            } else {
                rebuilt.addLast(current);
            }
        }

        while (!rebuilt.isEmpty()) {
            pile.addLast(rebuilt.removeFirst());
        }

        return found;
    }

    /**
     * Parses an integer property and throws a readable error on failure.
     *
     * @param rawValue the raw text
     * @param key the property key for diagnostics
     * @return the parsed integer
     */
    private static int parseRequiredInt(String rawValue, String key) {
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for " + key + ": " + rawValue, e);
        }
    }


}