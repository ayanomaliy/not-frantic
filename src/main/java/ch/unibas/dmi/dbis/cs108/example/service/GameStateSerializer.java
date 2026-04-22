package ch.unibas.dmi.dbis.cs108.example.service;

import ch.unibas.dmi.dbis.cs108.example.model.game.Card;
import ch.unibas.dmi.dbis.cs108.example.model.game.GameState;
import ch.unibas.dmi.dbis.cs108.example.model.game.PlayerGameState;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts game-model objects into wire-format strings for the
 * {@code TYPE|payload} protocol.
 *
 * <p>This serializer only includes the fields that are expected by the
 * current protocol tests.</p>
 */
public class GameStateSerializer {

    private GameStateSerializer() {}

    /**
     * Serializes the public game state into the compact payload format expected
     * by the protocol tests.
     *
     * <p>Format:</p>
     *
     * <pre>
     * phase:&lt;phase&gt;,currentPlayer:&lt;name&gt;,discardTop:&lt;id-or-none&gt;,
     * drawPileSize:&lt;n&gt;,players:&lt;name&gt;:&lt;handSize&gt;:&lt;totalScore&gt;[, ...]
     * </pre>
     *
     * <p>If the discard pile is empty, the discard-top field is serialized as
     * {@code none} in lowercase.</p>
     *
     * @param state the current game state
     * @return the encoded public-state payload
     */
    public static String serializePublicState(GameState state) {
        Card top = state.peekDiscardPile();
        String discardTop = top == null ? "none" : String.valueOf(top.id());

        String requestedColor = state.getRequestedColor() == null
                ? "none"
                : state.getRequestedColor().name();

        String requestedNumber = state.getRequestedNumber() == null
                ? "none"
                : String.valueOf(state.getRequestedNumber());

        String playersSummary = state.getPlayerOrder().stream()
                .map(p -> p.getPlayerName() + ":" + p.getHandSize() + ":" + p.getTotalScore())
                .collect(Collectors.joining(","));

        return "phase:" + state.getPhase().name()
                + ",currentPlayer:" + state.getCurrentPlayer().getPlayerName()
                + ",requestedColor:" + requestedColor
                + ",requestedNumber:" + requestedNumber
                + ",discardTop:" + discardTop
                + ",drawPileSize:" + state.getDrawPile().size()
                + ",players:" + playersSummary;
    }

    /**
     * Serializes a player's private hand as a comma-separated list of card ids
     * in hand order.
     *
     * @param player the player whose hand should be serialized
     * @return a comma-separated list of card ids, or an empty string if the hand is empty
     */
    public static String serializeHand(PlayerGameState player) {
        return player.getHand().stream()
                .map(c -> String.valueOf(c.id()))
                .collect(Collectors.joining(","));
    }

    /**
     * Serializes round-end scores in player-list order.
     *
     * <p>Format:</p>
     *
     * <pre>
     * playerName:roundScore:totalScore
     * </pre>
     *
     * <p>Entries are joined with commas. Missing round scores default to zero.</p>
     *
     * @param roundScores the per-round score map
     * @param players the player list in output order
     * @return the encoded round-end payload
     */
    public static String serializeRoundEnd(Map<String, Integer> roundScores,
                                           List<PlayerGameState> players) {
        return players.stream()
                .map(p -> p.getPlayerName()
                        + ":" + roundScores.getOrDefault(p.getPlayerName(), 0)
                        + ":" + p.getTotalScore())
                .collect(Collectors.joining(","));
    }

    /**
     * Serializes an effect request in the format {@code EFFECT:player}.
     *
     * @param effectName the effect name
     * @param actingPlayer the player who must answer the effect request
     * @return the encoded effect-request payload
     */
    public static String serializeEffectRequest(String effectName, String actingPlayer) {
        return effectName + ":" + actingPlayer;
    }
}