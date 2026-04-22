package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a {@link SpecialEffect} against the current {@link GameState}.
 *
 * <p>Each resolve call may mutate the game state and returns a list of
 * {@link GameEvent}s describing the performed actions.</p>
 *
 * <p>The caller is responsible for pushing effects onto the pending stack
 * before resolution where required.</p>
 */
public class EffectResolver {

    private EffectResolver() {}

    /**
     * Resolves the given effect for the acting player.
     *
     * @param effect the effect to resolve
     * @param state the current game state
     * @param actingPlayer the player resolving the effect
     * @param args client-supplied effect arguments
     * @return the generated game events
     */
    public static List<GameEvent> resolve(SpecialEffect effect,
                                          GameState state,
                                          String actingPlayer,
                                          EffectArgs args) {
        if (!state.getPendingEffects().isEmpty()
                && state.getPendingEffects().peek() == effect) {
            state.getPendingEffects().pop();
        }

        return switch (effect) {
            case SECOND_CHANCE -> resolveSecondChance(state, actingPlayer, args);
            case SKIP -> resolveSkip(state, actingPlayer, args);
            case GIFT -> resolveGift(state, actingPlayer, args);
            case EXCHANGE -> resolveExchange(state, actingPlayer, args);
            case FANTASTIC -> resolveFantastic(state, actingPlayer, args);
            case FANTASTIC_FOUR -> resolveFantasticFour(state, actingPlayer, args);
            case EQUALITY -> resolveEquality(state, actingPlayer, args);
            case COUNTERATTACK -> resolveCounterattack(state, actingPlayer, args);
            case NICE_TRY -> resolveNiceTry(state, actingPlayer, args);
        };
    }

    /**
     * Ensures that the acting player is considered to have performed a valid
     * turn action before {@link TurnEngine#endTurn(GameState)} is called.
     *
     * <p>This is useful in tests and in direct effect-resolution paths where the
     * card play may not have set the per-turn flags beforehand.</p>
     *
     * @param state the current game state
     * @param actingPlayer the acting player name
     */
    private static void ensureTurnActionRecorded(GameState state, String actingPlayer) {
        PlayerGameState actor = state.getPlayer(actingPlayer);
        if (!actor.hasPlayedThisTurn() && !actor.hasDrawnThisTurn()) {
            actor.setHasPlayedThisTurn(true);
        }
    }

    /**
     * Resolves SECOND_CHANCE.
     *
     * <p>If a follow-up card was selected, that card is played immediately.
     * Otherwise the acting player draws one card. The turn then ends unless a
     * newly played card triggers another effect.</p>
     *
     * @param state the current game state
     * @param actingPlayer the acting player
     * @param args the effect arguments
     * @return the generated game events
     */
    private static List<GameEvent> resolveSecondChance(GameState state,
                                                       String actingPlayer,
                                                       EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        PlayerGameState actor = state.getPlayer(actingPlayer);
        List<Card> selected = args.getSelectedCards();

        if (!selected.isEmpty()) {
            Card toPlay = selected.get(0);

            actor.removeCard(toPlay);
            state.pushToDiscardPile(toPlay);
            actor.setHasPlayedThisTurn(true);
            events.add(GameEvent.cardPlayed(actingPlayer, toPlay.id()));

            if (actor.getHandSize() == 0) {
                state.setPhase(GamePhase.ROUND_END);
                events.add(GameEvent.roundEnded("player_empty_hand"));
                return events;
            }

            if (toPlay.type() == CardType.BLACK) {
                Card eventCard = state.drawFromEventPile();
                if (eventCard != null) {
                    state.setActiveEventCard(eventCard);
                    events.add(GameEvent.eventCardFlipped(eventCard.id()));
                }
                state.setPhase(GamePhase.RESOLVING_EFFECT);
                return events;
            }

            if (toPlay.type() == CardType.SPECIAL_SINGLE || toPlay.type() == CardType.SPECIAL_FOUR) {
                state.getPendingEffects().push(toPlay.effect());
                state.setPhase(GamePhase.RESOLVING_EFFECT);
                events.add(GameEvent.effectTriggered(toPlay.effect()));
                return events;
            }
        } else {
            Card drawn = state.drawFromDrawPile();
            if (drawn != null) {
                actor.addCard(drawn);
                actor.setHasDrawnThisTurn(true);
                events.add(GameEvent.cardDrawn(actingPlayer, drawn.id()));
            }
        }

        if (TurnEngine.endRoundIfAnyPlayerHasNoCards(state, events)) {
            return events;
        }

        ensureTurnActionRecorded(state, actingPlayer);
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    /**
     * Resolves SKIP by marking the target player as skipped for their next turn.
     *
     * @param state the current game state
     * @param actingPlayer the acting player
     * @param args the effect arguments
     * @return the generated game events
     */
    private static List<GameEvent> resolveSkip(GameState state,
                                               String actingPlayer,
                                               EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        String targetName = args.getTargetPlayer();

        state.getPlayer(targetName).setSkipped(true);
        events.add(new GameEvent(
                GameEvent.EventType.EFFECT_TRIGGERED,
                SpecialEffect.SKIP.name() + ":" + targetName
        ));

        ensureTurnActionRecorded(state, actingPlayer);
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    /**
     * Resolves GIFT by transferring the selected cards from the acting player to
     * the target player.
     *
     * @param state the current game state
     * @param actingPlayer the acting player
     * @param args the effect arguments
     * @return the generated game events
     */
    private static List<GameEvent> resolveGift(GameState state,
                                               String actingPlayer,
                                               EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        PlayerGameState actor = state.getPlayer(actingPlayer);
        PlayerGameState target = state.getPlayer(args.getTargetPlayer());

        for (Card card : args.getSelectedCards()) {
            actor.removeCard(card);
            target.addCard(card);
            events.add(new GameEvent(
                    GameEvent.EventType.EFFECT_TRIGGERED,
                    SpecialEffect.GIFT.name() + ":" + actingPlayer
                            + ">" + args.getTargetPlayer() + ":" + card.id()
            ));
        }

        if (TurnEngine.endRoundIfAnyPlayerHasNoCards(state, events)) {
            return events;
        }

        ensureTurnActionRecorded(state, actingPlayer);
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    /**
     * Resolves EXCHANGE by swapping the selected actor cards with the first
     * equally many cards from the target player's hand.
     *
     * @param state the current game state
     * @param actingPlayer the acting player
     * @param args the effect arguments
     * @return the generated game events
     */
    private static List<GameEvent> resolveExchange(GameState state,
                                                   String actingPlayer,
                                                   EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        PlayerGameState actor = state.getPlayer(actingPlayer);
        PlayerGameState target = state.getPlayer(args.getTargetPlayer());

        List<Card> actorCards = args.getSelectedCards();
        int n = Math.min(actorCards.size(), target.getHandSize());
        List<Card> targetCards = new ArrayList<>(target.getHand().subList(0, n));

        for (Card c : actorCards) {
            actor.removeCard(c);
        }
        for (Card c : targetCards) {
            target.removeCard(c);
        }
        for (Card c : actorCards) {
            target.addCard(c);
        }
        for (Card c : targetCards) {
            actor.addCard(c);
        }

        events.add(new GameEvent(
                GameEvent.EventType.EFFECT_TRIGGERED,
                SpecialEffect.EXCHANGE.name() + ":" + actingPlayer
                        + "<>" + args.getTargetPlayer()
        ));

        if (TurnEngine.endRoundIfAnyPlayerHasNoCards(state, events)) {
            return events;
        }

        ensureTurnActionRecorded(state, actingPlayer);
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    /**
     * Resolves FANTASTIC by setting a requested color and/or number.
     *
     * @param state the current game state
     * @param actingPlayer the acting player
     * @param args the effect arguments
     * @return the generated game events
     */
    private static List<GameEvent> resolveFantastic(GameState state,
                                                    String actingPlayer,
                                                    EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        state.setRequestedColor(args.getChosenColor());
        state.setRequestedNumber(args.getChosenNumber());
        events.add(GameEvent.effectTriggered(SpecialEffect.FANTASTIC));

        ensureTurnActionRecorded(state, actingPlayer);
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    /**
     * Resolves FANTASTIC_FOUR.
     *
     * <p>The acting player distributes four drawn cards among exactly four
     * recipient slots. Repeated recipient names are allowed.</p>
     *
     * @param state the current game state
     * @param actingPlayer the acting player
     * @param args the effect arguments
     * @return the generated game events
     */
    private static List<GameEvent> resolveFantasticFour(GameState state,
                                                        String actingPlayer,
                                                        EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        List<String> targets = args.getTargetPlayers();

        if (targets.size() != 4) {
            events.add(GameEvent.error("FANTASTIC_FOUR requires exactly 4 target recipients."));
            return events;
        }

        for (String targetName : targets) {
            Card drawn = state.drawFromDrawPile();
            if (drawn == null) {
                break;
            }

            PlayerGameState recipient = state.getPlayer(targetName);
            recipient.addCard(drawn);
            events.add(GameEvent.cardDrawn(recipient.getPlayerName(), drawn.id()));
        }

        state.setRequestedColor(args.getChosenColor());
        state.setRequestedNumber(args.getChosenNumber());
        events.add(GameEvent.effectTriggered(SpecialEffect.FANTASTIC_FOUR));

        if (TurnEngine.endRoundIfAnyPlayerHasNoCards(state, events)) {
            return events;
        }

        ensureTurnActionRecorded(state, actingPlayer);
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    /**
     * Resolves EQUALITY by forcing the target player to draw up to the acting
     * player's hand size, then setting a requested color.
     *
     * @param state the current game state
     * @param actingPlayer the acting player
     * @param args the effect arguments
     * @return the generated game events
     */
    private static List<GameEvent> resolveEquality(GameState state,
                                                   String actingPlayer,
                                                   EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        PlayerGameState actor = state.getPlayer(actingPlayer);
        PlayerGameState target = state.getPlayer(args.getTargetPlayer());

        while (target.getHandSize() < actor.getHandSize()) {
            Card drawn = state.drawFromDrawPile();
            if (drawn == null) {
                break;
            }
            target.addCard(drawn);
            events.add(GameEvent.cardDrawn(target.getPlayerName(), drawn.id()));
        }

        state.setRequestedColor(args.getChosenColor());
        events.add(GameEvent.effectTriggered(SpecialEffect.EQUALITY));

        ensureTurnActionRecorded(state, actingPlayer);
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    /**
     * Resolves COUNTERATTACK.
     *
     * <p>Counterattack always sets a requested color. If another effect remains
     * pending underneath and a target was supplied, the target of that pending
     * effect is redirected and the turn is not ended yet.</p>
     *
     * @param state the current game state
     * @param actingPlayer the acting player
     * @param args the effect arguments
     * @return the generated game events
     */
    private static List<GameEvent> resolveCounterattack(GameState state,
                                                        String actingPlayer,
                                                        EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();

        CardColor color = args.getChosenColor();
        if (color == null) {
            events.add(GameEvent.error("COUNTERATTACK requires a color."));
            return events;
        }

        state.setRequestedColor(color);
        state.setRequestedNumber(null);

        String target = args.getTargetPlayer();

        if (!state.getPendingEffects().isEmpty() && target != null && !target.isBlank()) {
            state.setPendingEffectTarget(target);
            events.add(new GameEvent(
                    GameEvent.EventType.EFFECT_TRIGGERED,
                    SpecialEffect.COUNTERATTACK.name() + ":" + actingPlayer + ">" + target + ":" + color
            ));
            return events;
        }

        state.setPendingEffectTarget(null);
        events.add(new GameEvent(
                GameEvent.EventType.EFFECT_TRIGGERED,
                SpecialEffect.COUNTERATTACK.name() + ":" + actingPlayer + ":" + color
        ));

        ensureTurnActionRecorded(state, actingPlayer);
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }

    /**
     * Resolves NICE_TRY by forcing the target player to draw three cards.
     *
     * <p>The round does not end. After the target has drawn, the current turn is
     * advanced normally.</p>
     *
     * @param state the current game state
     * @param actingPlayer the acting player
     * @param args the effect arguments
     * @return the generated game events
     */
    private static List<GameEvent> resolveNiceTry(GameState state,
                                                  String actingPlayer,
                                                  EffectArgs args) {
        List<GameEvent> events = new ArrayList<>();
        String targetName = args.getTargetPlayer();
        PlayerGameState target = state.getPlayer(targetName);

        for (int i = 0; i < 3; i++) {
            Card drawn = state.drawFromDrawPile();
            if (drawn == null) {
                break;
            }
            target.addCard(drawn);
            events.add(GameEvent.cardDrawn(targetName, drawn.id()));
        }

        /*
         * NICE_TRY explicitly keeps the round alive. After the target draws,
         * the empty-hand condition should normally be gone, so ending the turn
         * is valid again.
         */
        ensureTurnActionRecorded(state, actingPlayer);
        events.addAll(TurnEngine.endTurn(state));
        return events;
    }
}