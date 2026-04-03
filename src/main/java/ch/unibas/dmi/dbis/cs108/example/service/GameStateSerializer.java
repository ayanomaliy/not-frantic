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
 * <h2>Payload formats</h2>
 * <dl>
 *   <dt>{@link #serializePublicState}</dt>
 *   <dd>{@code phase:AWAITING_PLAY,currentPlayer:Alice,discardTop:42,drawPileSize:80,
 *       players:Alice:7:0,Bob:5:12}</dd>
 *
 *   <dt>{@link #serializeHand}</dt>
 *   <dd>Comma-separated card ids: {@code 5,18,42,77}</dd>
 *
 *   <dt>{@link #serializeRoundEnd}</dt>
 *   <dd>One entry per player separated by {@code ,}: {@code Alice:5:20,Bob:12:30}</dd>
 *
 *   <dt>{@link #serializeEffectRequest}</dt>
 *   <dd>{@code SKIP:Alice} — effect name and acting player</dd>
 * </dl>
 *
 * All formats use only {@code |}, {@code ,}, and {@code :} as separators so
 * they can be embedded safely in a single {@code TYPE|payload} message.
 */
public class GameStateSerializer {

    private GameStateSerializer() {}

    /**
     * Serializes the public (non-secret) game state for broadcasting to all
     * players in the lobby.
     *
     * <p>Format:
     * {@code phase:<phase>,currentPlayer:<name>,discardTop:<id>,drawPileSize:<n>,
     * players:<name>:<handSize>:<totalScore>[,…]}
     *
     * @param state Current game state.
     * @return Encoded payload string.
     */
    public static String serializePublicState(GameState state) {
        Card top = state.peekDiscardPile();
        String discardTop = top == null ? "none" : String.valueOf(top.id());

        String playersSummary = state.getPlayerOrder().stream()
                .map(p -> p.getPlayerName() + ":" + p.getHandSize() + ":" + p.getTotalScore())
                .collect(Collectors.joining(","));

        return "phase:" + state.getPhase().name()
                + ",currentPlayer:" + state.getCurrentPlayer().getPlayerName()
                + ",discardTop:" + discardTop
                + ",drawPileSize:" + state.getDrawPile().size()
                + ",players:" + playersSummary;
    }

    /**
     * Serializes a player's private hand as a comma-separated list of card ids.
     *
     * @param player The player whose hand to serialize.
     * @return Comma-separated card ids, e.g. {@code "5,18,42"}, or {@code ""} if empty.
     */
    public static String serializeHand(PlayerGameState player) {
        return player.getHand().stream()
                .map(c -> String.valueOf(c.id()))
                .collect(Collectors.joining(","));
    }

    /**
     * Serializes round-end scores for broadcasting.
     *
     * <p>Format: {@code name:roundScore:totalScore} per player, comma-separated.
     *
     * @param roundScores  Per-round delta scores (player name → round score).
     * @param players      Full player list (used to read updated total scores).
     * @return Encoded payload string.
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
     * Serializes an effect-request prompt sent to the acting player.
     *
     * <p>Format: {@code <EFFECT_NAME>:<actingPlayer>}
     *
     * @param effectName   The name of the effect to resolve.
     * @param actingPlayer The player who must supply arguments.
     * @return Encoded payload string.
     */
    public static String serializeEffectRequest(String effectName, String actingPlayer) {
        return effectName + ":" + actingPlayer;
    }
}
