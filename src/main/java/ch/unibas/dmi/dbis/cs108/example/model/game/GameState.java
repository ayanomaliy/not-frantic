package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Holds the complete mutable state of a single game round.
 *
 * <p>All three piles use {@link Deque} as stacks: the "top" of each pile
 * is the {@code peek()} / {@code pop()} end (front of the deque).
 * Cards are pushed onto the front with {@link Deque#push(Object)}.
 */
public class GameState {

    private final List<PlayerGameState> playerOrder;
    private int currentPlayerIndex;
    private GamePhase phase;

    private final Deque<Card> drawPile;
    private final Deque<Card> discardPile;
    private final Deque<Card> eventPile;

    /** The event card currently being resolved, or {@code null} if none. */
    private Card activeEventCard;

    private final int maxScore;

    /** Color requested by Fantastic / Fantastic Four / Equality, or {@code null}. */
    private CardColor requestedColor;

    /** Number requested by Fantastic / Fantastic Four, or {@code null}. */
    private Integer requestedNumber;

    /** Stack of pending effects waiting to be resolved (e.g. counterattack chains). */
    private final Deque<SpecialEffect> pendingEffects;

    /** Name of the player the top pending effect targets, or {@code null}. */
    private String pendingEffectTarget;

    /**
     * When {@code true}, playing {@link CardType#SPECIAL_SINGLE} or
     * {@link CardType#SPECIAL_FOUR} cards is forbidden until the next black card is played.
     * Set by the {@code BLOCK_SPECIALS} event; cleared automatically in
     * {@link TurnEngine} when a {@link CardType#BLACK} card is played.
     */
    private boolean specialsBlocked = false;

    /**
     * When {@code true}, card scoring values are doubled at round end.
     * Set by the {@code DOUBLE_SCORING} event; consumed and cleared by
     * {@link ScoreCalculator#calculateRoundScores(java.util.List, GameState)}.
     */
    private boolean doubleScoringActive = false;

    public GameState(List<PlayerGameState> playerOrder,
                     Deque<Card> drawPile,
                     Deque<Card> discardPile,
                     Deque<Card> eventPile,
                     int maxScore) {
        this.playerOrder = playerOrder;
        this.currentPlayerIndex = 0;
        this.phase = GamePhase.TURN_START;
        this.drawPile = drawPile;
        this.discardPile = discardPile;
        this.eventPile = eventPile;
        this.maxScore = maxScore;
        this.pendingEffects = new ArrayDeque<>();
    }

    // --- Current player ---

    public PlayerGameState getCurrentPlayer() {
        return playerOrder.get(currentPlayerIndex);
    }

    public void advanceToNextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % playerOrder.size();
    }

    // --- Piles ---

    /**
     * Draws the top card from the draw pile, or {@code null} if empty.
     */
    public Card drawFromDrawPile() {
        return drawPile.isEmpty() ? null : drawPile.pop();
    }

    /**
     * Places a card on top of the discard pile.
     */
    public void pushToDiscardPile(Card card) {
        discardPile.push(card);
    }

    /**
     * Returns the top card of the discard pile without removing it,
     * or {@code null} if the pile is empty.
     */
    public Card peekDiscardPile() {
        return discardPile.isEmpty() ? null : discardPile.peek();
    }

    /**
     * Draws the top card from the event pile, or {@code null} if empty.
     */
    public Card drawFromEventPile() {
        return eventPile.isEmpty() ? null : eventPile.pop();
    }

    // --- Getters & setters ---

    public List<PlayerGameState> getPlayerOrder() {
        return playerOrder;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public Deque<Card> getDrawPile() {
        return drawPile;
    }

    public Deque<Card> getDiscardPile() {
        return discardPile;
    }

    public Deque<Card> getEventPile() {
        return eventPile;
    }

    public Card getActiveEventCard() {
        return activeEventCard;
    }

    public void setActiveEventCard(Card activeEventCard) {
        this.activeEventCard = activeEventCard;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public CardColor getRequestedColor() {
        return requestedColor;
    }

    public void setRequestedColor(CardColor requestedColor) {
        this.requestedColor = requestedColor;
    }

    public Integer getRequestedNumber() {
        return requestedNumber;
    }

    public void setRequestedNumber(Integer requestedNumber) {
        this.requestedNumber = requestedNumber;
    }

    public Deque<SpecialEffect> getPendingEffects() {
        return pendingEffects;
    }

    public String getPendingEffectTarget() {
        return pendingEffectTarget;
    }

    public void setPendingEffectTarget(String pendingEffectTarget) {
        this.pendingEffectTarget = pendingEffectTarget;
    }

    public boolean isSpecialsBlocked() {
        return specialsBlocked;
    }

    public void setSpecialsBlocked(boolean specialsBlocked) {
        this.specialsBlocked = specialsBlocked;
    }

    public boolean isDoubleScoringActive() {
        return doubleScoringActive;
    }

    public void setDoubleScoringActive(boolean doubleScoringActive) {
        this.doubleScoringActive = doubleScoringActive;
    }

    /**
     * Looks up a player by name in the current player order.
     * @throws IllegalArgumentException if the name is not found.
     */
    public PlayerGameState getPlayer(String name) {
        return playerOrder.stream()
                .filter(p -> p.getPlayerName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + name));
    }
}
