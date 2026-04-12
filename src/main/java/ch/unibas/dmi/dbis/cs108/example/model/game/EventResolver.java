package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves an event card effect when a {@link CardType#BLACK} card is played.
 *
 * <h2>Contract</h2>
 * <ol>
 *   <li>The caller (ServerService) sets {@link GameState#setActiveEventCard(Card)}
 *       before calling {@link #resolve}.</li>
 *   <li>{@link #resolve} dispatches to the appropriate handler, which mutates
 *       {@code state} and returns a list of {@link GameEvent}s.</li>
 *   <li>The caller clears {@link GameState#setActiveEventCard(Card) activeEventCard}
 *       after the events have been broadcast.</li>
 * </ol>
 */
public class EventResolver {

    private EventResolver() {}

    /**
     * Resolves the effect of {@code eventCard} against the current {@code state}.
     *
     * @param eventCard The event card that was just flipped (ID 0–19).
     * @param state     Current game state (mutated in place).
     * @return Events describing what happened; broadcast these to all clients.
     * @throws IllegalArgumentException if the event card ID is out of range.
     */
    public static List<GameEvent> resolve(Card eventCard, GameState state) {
        EventEffect effect = EventEffect.fromCardId(eventCard.id());
        List<GameEvent> events = new ArrayList<>();

        events.addAll(switch (effect) {
            case ALL_DRAW_TWO      -> handleAllDrawTwo(state);
            case ALL_DRAW_ONE      -> handleAllDrawOne(state);
            case ALL_SKIP          -> handleAllSkip(state);
            case INSTANT_ROUND_END -> handleInstantRoundEnd(state);
            case REVERSE_ORDER     -> handleReverseOrder(state);
            case STEAL_FROM_NEXT   -> handleStealFromNext(state);
            case STEAL_FROM_PREV   -> handleStealFromPrev(state);
            case DISCARD_HIGHEST   -> handleDiscardHighest(state);
            case DISCARD_COLOR     -> handleDiscardColor(state);
            case SWAP_HANDS        -> handleSwapHands(state);
            case BLOCK_SPECIALS    -> handleBlockSpecials(state);
            case GIFT_CHAIN        -> handleGiftChain(state);
            case HAND_RESET        -> handleHandReset(state);
            case LUCKY_DRAW        -> handleLuckyDraw(state);
            case PENALTY_DRAW      -> handlePenaltyDraw(state);
            case EQUALIZE          -> handleEqualize(state);
            case WILD_REQUEST      -> handleWildRequest(state);
            case CANCEL_EFFECTS    -> handleCancelEffects(state);
            case BONUS_PLAY        -> handleBonusPlay(state);
            case DOUBLE_SCORING    -> handleDoubleScoring(state);
        });

        // Clear the active event card after resolution
        state.setActiveEventCard(null);

        return events;
    }

    // =========================================================================
    // Handlers
    // =========================================================================

    /** All players draw 2 cards from the draw pile. */
    private static List<GameEvent> handleAllDrawTwo(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        for (PlayerGameState p : state.getPlayerOrder()) {
            for (int i = 0; i < 2; i++) {
                Card card = state.drawFromDrawPile();
                if (card != null) {
                    p.addCard(card);
                    events.add(GameEvent.cardDrawn(p.getPlayerName(), card.id()));
                }
            }
        }
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "ALL_DRAW_TWO"));
        return events;
    }

    /** All players draw 1 card from the draw pile. */
    private static List<GameEvent> handleAllDrawOne(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        for (PlayerGameState p : state.getPlayerOrder()) {
            Card card = state.drawFromDrawPile();
            if (card != null) {
                p.addCard(card);
                events.add(GameEvent.cardDrawn(p.getPlayerName(), card.id()));
            }
        }
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "ALL_DRAW_ONE"));
        return events;
    }

    /**
     * All players other than the triggering player (current player) must skip
     * their next turn.
     */
    private static List<GameEvent> handleAllSkip(GameState state) {
        String currentName = state.getCurrentPlayer().getPlayerName();
        for (PlayerGameState p : state.getPlayerOrder()) {
            if (!p.getPlayerName().equals(currentName)) {
                p.setSkipped(true);
            }
        }
        return List.of(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "ALL_SKIP"));
    }

    /** The round ends immediately; scores are calculated by the caller. */
    private static List<GameEvent> handleInstantRoundEnd(GameState state) {
        state.setPhase(GamePhase.ROUND_END);
        return List.of(GameEvent.roundEnded("instant_round_end"));
    }

    /**
     * Reverses the player order list in-place and updates {@code currentPlayerIndex}
     * so the same player remains current.
     */
    private static List<GameEvent> handleReverseOrder(GameState state) {
        List<PlayerGameState> order = state.getPlayerOrder();
        int oldIndex = state.getCurrentPlayerIndex();
        Collections.reverse(order);
        state.setCurrentPlayerIndex(order.size() - 1 - oldIndex);
        return List.of(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "REVERSE_ORDER"));
    }

    /**
     * The triggering player takes the last card from the next player's hand.
     * No-op if the next player has no cards.
     */
    private static List<GameEvent> handleStealFromNext(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        List<PlayerGameState> order = state.getPlayerOrder();
        int n = order.size();
        PlayerGameState current = order.get(state.getCurrentPlayerIndex());
        PlayerGameState next = order.get((state.getCurrentPlayerIndex() + 1) % n);
        if (!next.getHand().isEmpty()) {
            Card stolen = next.getHand().remove(next.getHand().size() - 1);
            current.addCard(stolen);
            events.add(GameEvent.cardDrawn(current.getPlayerName(), stolen.id()));
        }
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED,
                "STEAL_FROM_NEXT:" + current.getPlayerName()));
        return events;
    }

    /**
     * The triggering player takes the last card from the previous player's hand.
     * No-op if the previous player has no cards.
     */
    private static List<GameEvent> handleStealFromPrev(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        List<PlayerGameState> order = state.getPlayerOrder();
        int n = order.size();
        PlayerGameState current = order.get(state.getCurrentPlayerIndex());
        PlayerGameState prev = order.get((state.getCurrentPlayerIndex() - 1 + n) % n);
        if (!prev.getHand().isEmpty()) {
            Card stolen = prev.getHand().remove(prev.getHand().size() - 1);
            current.addCard(stolen);
            events.add(GameEvent.cardDrawn(current.getPlayerName(), stolen.id()));
        }
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED,
                "STEAL_FROM_PREV:" + current.getPlayerName()));
        return events;
    }

    /**
     * Every player discards their single highest-scoring card onto the discard pile.
     * If any player empties their hand, the round ends.
     */
    private static List<GameEvent> handleDiscardHighest(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        for (PlayerGameState p : state.getPlayerOrder()) {
            if (p.getHand().isEmpty()) continue;
            Card highest = p.getHand().stream()
                    .max(Comparator.comparingInt(Card::scoringValue))
                    .get();
            p.getHand().remove(highest);
            state.pushToDiscardPile(highest);
            events.add(GameEvent.cardPlayed(p.getPlayerName(), highest.id()));
        }
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "DISCARD_HIGHEST"));
        boolean anyEmpty = state.getPlayerOrder().stream().anyMatch(p -> p.getHandSize() == 0);
        if (anyEmpty) {
            state.setPhase(GamePhase.ROUND_END);
            events.add(GameEvent.roundEnded("player_empty_hand_via_discard_highest"));
        }
        return events;
    }

    /**
     * Every player discards all cards whose color matches the top of the discard pile.
     * Cards of types without a color (e.g. four-color specials) are unaffected.
     * If any player empties their hand, the round ends.
     */
    private static List<GameEvent> handleDiscardColor(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        Card top = state.peekDiscardPile();
        CardColor color = (top != null) ? top.color() : null;
        if (color != null) {
            for (PlayerGameState p : state.getPlayerOrder()) {
                List<Card> toDiscard = p.getHand().stream()
                        .filter(c -> c.color() == color)
                        .toList();
                for (Card c : toDiscard) {
                    p.getHand().remove(c);
                    state.pushToDiscardPile(c);
                    events.add(GameEvent.cardPlayed(p.getPlayerName(), c.id()));
                }
            }
            boolean anyEmpty = state.getPlayerOrder().stream().anyMatch(p -> p.getHandSize() == 0);
            if (anyEmpty) {
                state.setPhase(GamePhase.ROUND_END);
                events.add(GameEvent.roundEnded("player_empty_hand_via_discard_color"));
            }
        }
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED,
                "DISCARD_COLOR:" + color));
        return events;
    }

    /**
     * The triggering player swaps their entire hand with the next player in turn order.
     */
    private static List<GameEvent> handleSwapHands(GameState state) {
        List<PlayerGameState> order = state.getPlayerOrder();
        int n = order.size();
        PlayerGameState current = order.get(state.getCurrentPlayerIndex());
        PlayerGameState next = order.get((state.getCurrentPlayerIndex() + 1) % n);
        List<Card> tmp = new ArrayList<>(current.getHand());
        current.getHand().clear();
        current.getHand().addAll(next.getHand());
        next.getHand().clear();
        next.getHand().addAll(tmp);
        return List.of(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED,
                "SWAP_HANDS:" + current.getPlayerName() + ":" + next.getPlayerName()));
    }

    /**
     * Forbids playing special cards until the next black card is played.
     * Sets {@link GameState#setSpecialsBlocked(boolean) specialsBlocked = true}.
     */
    private static List<GameEvent> handleBlockSpecials(GameState state) {
        state.setSpecialsBlocked(true);
        return List.of(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "BLOCK_SPECIALS"));
    }

    /**
     * Each player simultaneously passes their last hand card to the next player
     * in turn order. Players with empty hands contribute nothing.
     */
    private static List<GameEvent> handleGiftChain(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        List<PlayerGameState> order = state.getPlayerOrder();
        int n = order.size();
        // Collect one card per player first (to avoid cascading)
        List<Card> gifts = new ArrayList<>();
        for (PlayerGameState p : order) {
            if (!p.getHand().isEmpty()) {
                gifts.add(p.getHand().remove(p.getHand().size() - 1));
            } else {
                gifts.add(null);
            }
        }
        // Distribute: player i passes to player (i+1)%n
        for (int i = 0; i < n; i++) {
            Card gift = gifts.get(i);
            if (gift != null) {
                PlayerGameState recipient = order.get((i + 1) % n);
                recipient.addCard(gift);
                events.add(GameEvent.cardDrawn(recipient.getPlayerName(), gift.id()));
            }
        }
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "GIFT_CHAIN"));
        return events;
    }

    /**
     * All players discard their entire hand onto the discard pile, then each
     * draws 7 new cards from the draw pile.
     */
    private static List<GameEvent> handleHandReset(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        // Discard all hands
        for (PlayerGameState p : state.getPlayerOrder()) {
            for (Card c : new ArrayList<>(p.getHand())) {
                p.getHand().remove(c);
                state.pushToDiscardPile(c);
            }
        }
        // Redraw 7 each
        for (PlayerGameState p : state.getPlayerOrder()) {
            for (int i = 0; i < 7; i++) {
                Card card = state.drawFromDrawPile();
                if (card == null) break;
                p.addCard(card);
                events.add(GameEvent.cardDrawn(p.getPlayerName(), card.id()));
            }
        }
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "HAND_RESET"));
        return events;
    }

    /** The triggering player draws 3 extra cards. */
    private static List<GameEvent> handleLuckyDraw(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        PlayerGameState current = state.getCurrentPlayer();
        for (int i = 0; i < 3; i++) {
            Card card = state.drawFromDrawPile();
            if (card == null) break;
            current.addCard(card);
            events.add(GameEvent.cardDrawn(current.getPlayerName(), card.id()));
        }
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "LUCKY_DRAW"));
        return events;
    }

    /** The player currently holding the most cards must draw 2 more. */
    private static List<GameEvent> handlePenaltyDraw(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        state.getPlayerOrder().stream()
                .max(Comparator.comparingInt(PlayerGameState::getHandSize))
                .ifPresent(richest -> {
                    for (int i = 0; i < 2; i++) {
                        Card card = state.drawFromDrawPile();
                        if (card == null) break;
                        richest.addCard(card);
                        events.add(GameEvent.cardDrawn(richest.getPlayerName(), card.id()));
                    }
                });
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "PENALTY_DRAW"));
        return events;
    }

    /**
     * All players draw cards until their hand size equals that of the player with
     * the most cards at the time of resolution.
     */
    private static List<GameEvent> handleEqualize(GameState state) {
        List<GameEvent> events = new ArrayList<>();
        int maxSize = state.getPlayerOrder().stream()
                .mapToInt(PlayerGameState::getHandSize)
                .max().orElse(0);
        for (PlayerGameState p : state.getPlayerOrder()) {
            while (p.getHandSize() < maxSize) {
                Card card = state.drawFromDrawPile();
                if (card == null) break;
                p.addCard(card);
                events.add(GameEvent.cardDrawn(p.getPlayerName(), card.id()));
            }
        }
        events.add(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "EQUALIZE"));
        return events;
    }

    /**
     * Sets a color request matching the color of the current discard-pile top.
     * Falls back to {@link CardColor#RED} if the top card has no color.
     */
    private static List<GameEvent> handleWildRequest(GameState state) {
        Card top = state.peekDiscardPile();
        CardColor color = (top != null && top.color() != null) ? top.color() : CardColor.RED;
        state.setRequestedColor(color);
        return List.of(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED,
                "WILD_REQUEST:" + color));
    }

    /**
     * Clears all pending special effects and their target, cancelling any
     * queued effect chain.
     */
    private static List<GameEvent> handleCancelEffects(GameState state) {
        state.getPendingEffects().clear();
        state.setPendingEffectTarget(null);
        return List.of(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "CANCEL_EFFECTS"));
    }

    /**
     * Allows the triggering player to play one additional card by setting the
     * phase back to {@link GamePhase#AWAITING_PLAY}.
     */
    private static List<GameEvent> handleBonusPlay(GameState state) {
        state.setPhase(GamePhase.AWAITING_PLAY);
        return List.of(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "BONUS_PLAY"));
    }

    /**
     * Marks this round for double scoring. The multiplier is applied when
     * {@link ScoreCalculator#calculateRoundScores(java.util.List, GameState)} is called.
     */
    private static List<GameEvent> handleDoubleScoring(GameState state) {
        state.setDoubleScoringActive(true);
        return List.of(new GameEvent(GameEvent.EventType.EFFECT_TRIGGERED, "DOUBLE_SCORING"));
    }
}
