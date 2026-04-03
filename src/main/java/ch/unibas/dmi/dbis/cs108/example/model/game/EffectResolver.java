package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a {@link SpecialEffect} against the current {@link GameState}.
 *
 * <p>Each {@code resolve} call:
 * <ol>
 *   <li>Pops the effect off the top of {@link GameState#getPendingEffects()} (if it
 *       is the top entry — guards against mis-ordering).</li>
 *   <li>Applies the effect's state changes.</li>
 *   <li>Calls {@link TurnEngine#endTurn} at the end for effects that consume the
 *       actor's turn.  {@code COUNTERATTACK} is the exception: it only redirects the
 *       next pending effect, so the phase stays {@code RESOLVING_EFFECT}.</li>
 * </ol>
 *
 * <h2>Caller contract</h2>
 * The caller (ServerService in Phase 8) is responsible for:
 * <ul>
 *   <li>Pushing the effect onto {@link GameState#getPendingEffects()} before calling
 *       {@code resolve} (already done by {@link TurnEngine#playCard} for in-turn
 *       plays; done separately for out-of-turn plays like COUNTERATTACK / NICE_TRY).</li>
 *   <li>After resolve returns, checking whether more pending effects remain and
 *       calling {@code resolve} again as needed.</li>
 * </ul>
 */
public class EffectResolver {

    private EffectResolver() {}

    /**
     * Resolves {@code effect} on behalf of {@code actingPlayer}.
     *
     * @param effect       The effect to resolve (must be the top of pendingEffects).
     * @param state        Current game state (mutated in place).
     * @param actingPlayer Name of the player who played the card triggering the effect.
     * @param args         Client-supplied parameters (target, chosen color/number, cards).
     * @return Events describing what happened; broadcast these to all clients.
     */
    public static List<GameEvent> resolve(SpecialEffect effect,
                                          GameState state,
                                          String actingPlayer,
                                          EffectArgs args) {
        // Pop the effect off the pending stack if it is sitting on top
        if (!state.getPendingEffects().isEmpty()
                && state.getPendingEffects().peek() == effect) {
            state.getPendingEffects().pop();
        }

        return switch (effect) {
            case SECOND_CHANCE -> resolveSecondChance(state, actingPlayer, args);
            case SKIP          -> resolveSkip(state, actingPlayer, args);
            case GIFT          -> resolveGift(state, actingPlayer, args);
            case EXCHANGE      -> resolveExchange(state, actingPlayer, args);
            case FANTASTIC     -> resolveFantastic(state, actingPlayer, args);
            case FANTASTIC_FOUR -> resolveFantasticFour(state, actingPlayer, args);
            case EQUALITY      -> resolveEquality(state, actingPlayer, args);
            case COUNTERATTACK -> resolveCounterattack(state, actingPlayer, args);
            case NICE_TRY      -> resolveNiceTry(state, actingPlayer, args);
        };
    }

    // -------------------------------------------------------------------------
    // SECOND_CHANCE — actor plays another card, or draws 1 if impossible
    // -------------------------------------------------------------------------

    private static List<GameEvent> resolveSecondChance(GameState state,
                                                        String actingPlayer,
                                                        EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        PlayerGameState actor = state.getPlayer(actingPlayer);
        List<Card> selected = args.getSelectedCards();

        if (!selected.isEmpty()) {
            // Play the chosen card directly (no nested TurnEngine.playCard to avoid re-routing)
            Card toPlay = selected.get(0);
            actor.removeCard(toPlay);
            state.pushToDiscardPile(toPlay);
            events.add(GameEvent.cardPlayed(actingPlayer, toPlay.id()));

            if (actor.getHandSize() == 0) {
                state.setPhase(GamePhase.ROUND_END);
                events.add(GameEvent.roundEnded("player_empty_hand"));
                return events;
            }
        } else {
            // No valid card — draw 1 as penalty
            Card drawn = state.drawFromDrawPile();
            if (drawn != null) {
                actor.addCard(drawn);
                events.add(GameEvent.cardDrawn(actingPlayer, drawn.id()));
            }
        }

        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    // -------------------------------------------------------------------------
    // SKIP — mark a named player so their next turn is skipped
    // -------------------------------------------------------------------------

    private static List<GameEvent> resolveSkip(GameState state,
                                                String actingPlayer,
                                                EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        String targetName = args.getTargetPlayer();
        state.getPlayer(targetName).setSkipped(true);
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED,
                SpecialEffect.SKIP.name() + ":" + targetName));
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    // -------------------------------------------------------------------------
    // GIFT — give 1–2 cards to another player
    // -------------------------------------------------------------------------

    private static List<GameEvent> resolveGift(GameState state,
                                                String actingPlayer,
                                                EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        PlayerGameState actor  = state.getPlayer(actingPlayer);
        PlayerGameState target = state.getPlayer(args.getTargetPlayer());

        for (Card card : args.getSelectedCards()) {
            actor.removeCard(card);
            target.addCard(card);
            events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED,
                    SpecialEffect.GIFT.name() + ":" + actingPlayer
                            + ">" + args.getTargetPlayer() + ":" + card.id()));
        }

        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    // -------------------------------------------------------------------------
    // EXCHANGE — swap actor's selected cards with target's first N cards (blind)
    // -------------------------------------------------------------------------

    private static List<GameEvent> resolveExchange(GameState state,
                                                    String actingPlayer,
                                                    EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        PlayerGameState actor  = state.getPlayer(actingPlayer);
        PlayerGameState target = state.getPlayer(args.getTargetPlayer());

        List<Card> actorCards  = args.getSelectedCards();
        // Take the first N cards from the target's hand (hidden to the actor)
        int n = Math.min(actorCards.size(), target.getHandSize());
        List<Card> targetCards = new ArrayList<>(target.getHand().subList(0, n));

        for (Card c : actorCards)  actor.removeCard(c);
        for (Card c : targetCards) target.removeCard(c);
        for (Card c : actorCards)  target.addCard(c);
        for (Card c : targetCards) actor.addCard(c);

        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED,
                SpecialEffect.EXCHANGE.name() + ":" + actingPlayer
                        + "<>" + args.getTargetPlayer()));
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    // -------------------------------------------------------------------------
    // FANTASTIC — set a requested color and/or number
    // -------------------------------------------------------------------------

    private static List<GameEvent> resolveFantastic(GameState state,
                                                     String actingPlayer,
                                                     EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        state.setRequestedColor(args.getChosenColor());
        state.setRequestedNumber(args.getChosenNumber());
        events.add(GameEvent.effectTriggered(SpecialEffect.FANTASTIC));
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    // -------------------------------------------------------------------------
    // FANTASTIC_FOUR — distribute 4 cards from draw pile, then set request
    // -------------------------------------------------------------------------

    private static List<GameEvent> resolveFantasticFour(GameState state,
                                                         String actingPlayer,
                                                         EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        List<PlayerGameState> players = state.getPlayerOrder();
        int actorIdx = players.indexOf(state.getPlayer(actingPlayer));
        int count    = players.size();

        // Distribute up to 4 cards starting with the player after the actor
        for (int i = 1; i <= 4; i++) {
            Card drawn = state.drawFromDrawPile();
            if (drawn == null) break;
            PlayerGameState recipient = players.get((actorIdx + i) % count);
            recipient.addCard(drawn);
            events.add(GameEvent.cardDrawn(recipient.getPlayerName(), drawn.id()));
        }

        state.setRequestedColor(args.getChosenColor());
        state.setRequestedNumber(args.getChosenNumber());
        events.add(GameEvent.effectTriggered(SpecialEffect.FANTASTIC_FOUR));
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    // -------------------------------------------------------------------------
    // EQUALITY — target draws until their hand size equals the actor's;
    //            actor then sets a requested color
    // -------------------------------------------------------------------------

    private static List<GameEvent> resolveEquality(GameState state,
                                                    String actingPlayer,
                                                    EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        PlayerGameState actor  = state.getPlayer(actingPlayer);
        PlayerGameState target = state.getPlayer(args.getTargetPlayer());

        while (target.getHandSize() < actor.getHandSize()) {
            Card drawn = state.drawFromDrawPile();
            if (drawn == null) break;
            target.addCard(drawn);
            events.add(GameEvent.cardDrawn(target.getPlayerName(), drawn.id()));
        }

        state.setRequestedColor(args.getChosenColor());
        events.add(GameEvent.effectTriggered(SpecialEffect.EQUALITY));
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    // -------------------------------------------------------------------------
    // COUNTERATTACK — redirect the top pending effect to a new target
    //
    // Does NOT call endTurn: the redirected effect still needs to be resolved.
    // Phase stays RESOLVING_EFFECT.
    // -------------------------------------------------------------------------

    private static List<GameEvent> resolveCounterattack(GameState state,
                                                         String actingPlayer,
                                                         EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        state.setPendingEffectTarget(args.getTargetPlayer());
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED,
                SpecialEffect.COUNTERATTACK.name() + ":" + actingPlayer
                        + ">" + args.getTargetPlayer()));
        // Intentionally no endTurn — the redirected effect in pendingEffects must
        // still be resolved before the turn can advance.
        return events;
    }

    // -------------------------------------------------------------------------
    // NICE_TRY — force the player who just emptied their hand to draw 3 cards;
    //            the round does NOT end; the game continues with the next player
    // -------------------------------------------------------------------------

    private static List<GameEvent> resolveNiceTry(GameState state,
                                                   String actingPlayer,
                                                   EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        String targetName      = args.getTargetPlayer();
        PlayerGameState target = state.getPlayer(targetName);

        for (int i = 0; i < 3; i++) {
            Card drawn = state.drawFromDrawPile();
            if (drawn == null) break;
            target.addCard(drawn);
            events.add(GameEvent.cardDrawn(targetName, drawn.id()));
        }

        // Un-ROUND_END: advance to the next player and continue
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }
}
