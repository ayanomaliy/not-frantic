package ch.unibas.dmi.dbis.cs108.example.service;

import ch.unibas.dmi.dbis.cs108.example.model.game.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the payload of game-action {@link Message}s sent by clients.
 *
 * <h2>Payload formats understood</h2>
 * <dl>
 *   <dt>{@code PLAY_CARD|<cardId>}</dt>
 *   <dd>Single integer card id.</dd>
 *
 *   <dt>{@code EFFECT_RESPONSE|<EFFECT_NAME>|<arg1>|<arg2>|…}</dt>
 *   <dd>
 *     The second segment is the effect name (matches {@link SpecialEffect#name()}).
 *     Remaining segments depend on the effect:
 *     <ul>
 *       <li>SKIP / COUNTERATTACK / NICE_TRY: {@code |<targetPlayer>}</li>
 *       <li>GIFT:     {@code |<targetPlayer>|<cardId1>[,<cardId2>]}</li>
 *       <li>EXCHANGE: {@code |<targetPlayer>|<cardId1>,<cardId2>}</li>
 *       <li>FANTASTIC:     {@code |<COLOR>} or {@code |<COLOR>|<number>}</li>
 *       <li>FANTASTIC_FOUR:{@code |<COLOR>} or {@code |<COLOR>|<number>}</li>
 *       <li>EQUALITY: {@code |<targetPlayer>|<COLOR>}</li>
 *       <li>SECOND_CHANCE: {@code |<cardId>}  (empty = draw penalty)</li>
 *     </ul>
 *   </dd>
 * </dl>
 */
public class GameMessageParser {

    private GameMessageParser() {}

    /**
     * Parses a {@code PLAY_CARD} payload into a card id.
     *
     * @param payload Raw payload string (just the card id integer).
     * @return The card id, or {@code -1} if the payload is malformed.
     */
    public static int parsePlayCard(String payload) {
        try {
            return Integer.parseInt(payload.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Parses an {@code EFFECT_RESPONSE} payload into the effect name and
     * a populated {@link EffectArgs}.
     *
     * <p>Returns {@code null} if the payload is malformed or the effect name
     * is not recognized.
     *
     * @param payload  Raw payload after the {@code EFFECT_RESPONSE|} prefix.
     * @param state    Current game state (used to look up cards in player hands).
     * @param actingPlayer Name of the player submitting the response.
     * @return A two-element array {@code [effectName, EffectArgs]}, or {@code null}.
     */
    public static Object[] parseEffectResponse(String payload, GameState state,
                                               String actingPlayer) {
        if (payload == null || payload.isBlank()) return null;

        String[] parts = payload.split("\\|", -1);
        if (parts.length < 1) return null;

        String effectName = parts[0].trim().toUpperCase();
        SpecialEffect effect;
        try {
            effect = SpecialEffect.valueOf(effectName);
        } catch (IllegalArgumentException e) {
            return null;
        }

        EffectArgs args = switch (effect) {
            case SKIP, COUNTERATTACK, NICE_TRY -> {
                if (parts.length < 2) yield null;
                yield EffectArgs.withTarget(parts[1].trim());
            }
            case GIFT -> {
                if (parts.length < 3) yield null;
                String target = parts[1].trim();
                List<Card> cards = parseCardIds(parts[2], state, actingPlayer);
                yield EffectArgs.withTargetAndCards(target, cards);
            }
            case EXCHANGE -> {
                if (parts.length < 3) yield null;
                String target = parts[1].trim();
                List<Card> cards = parseCardIds(parts[2], state, actingPlayer);
                yield EffectArgs.withTargetAndCards(target, cards);
            }
            case FANTASTIC, FANTASTIC_FOUR -> {
                if (parts.length < 2) yield EffectArgs.empty();
                CardColor color = parseColor(parts[1]);
                Integer number = (parts.length >= 3) ? parseNumber(parts[2]) : null;
                yield EffectArgs.withColorAndNumber(color, number);
            }
            case EQUALITY -> {
                if (parts.length < 3) yield null;
                String target = parts[1].trim();
                CardColor color = parseColor(parts[2]);
                yield EffectArgs.withTargetAndColor(target, color);
            }
            case SECOND_CHANCE -> {
                if (parts.length < 2 || parts[1].isBlank()) {
                    yield EffectArgs.withCards(List.of()); // draw penalty
                }
                List<Card> cards = parseCardIds(parts[1], state, actingPlayer);
                yield EffectArgs.withCards(cards);
            }
        };

        if (args == null) return null;
        return new Object[]{effectName, args};
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves a comma-separated list of card id strings to actual {@link Card}
     * objects from the acting player's hand.
     */
    private static List<Card> parseCardIds(String segment, GameState state,
                                           String actingPlayer) {
        List<Card> result = new ArrayList<>();
        PlayerGameState player = state.getPlayer(actingPlayer);
        for (String part : segment.split(",")) {
            try {
                int id = Integer.parseInt(part.trim());
                player.getHand().stream()
                        .filter(c -> c.id() == id)
                        .findFirst()
                        .ifPresent(result::add);
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static CardColor parseColor(String text) {
        try {
            return CardColor.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Integer parseNumber(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
