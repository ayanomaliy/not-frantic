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

    private Integer forcedEventCardIdOnBlack;

    /**
     * Creates a new game state for one round.
     *
     * @param playerOrder the ordered list of players for this round
     * @param drawPile    the shuffled draw pile
     * @param discardPile the initial discard pile (containing the starter card)
     * @param eventPile   the shuffled event card pile
     * @param maxScore    the score threshold at which the game ends
     */
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

    /**
     * Returns the {@link PlayerGameState} of the player whose turn it currently is.
     *
     * @return the current player's state
     */
    public PlayerGameState getCurrentPlayer() {
        return playerOrder.get(currentPlayerIndex);
    }

    /**
     * Advances the current player index to the next player in round-robin order.
     */
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

    /**
     * Returns the ordered list of all players in this round.
     *
     * @return the player order list
     */
    public List<PlayerGameState> getPlayerOrder() {
        return playerOrder;
    }

    /**
     * Returns the index into {@link #getPlayerOrder()} of the current player.
     *
     * @return the current player index
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    /**
     * Sets the current player index directly.
     *
     * <p>Used by {@link ch.unibas.dmi.dbis.cs108.example.model.game.EventResolver}
     * when reversing player order, so the same player remains active after the reversal.</p>
     *
     * @param currentPlayerIndex the new player index
     */
    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    /**
     * Returns the current phase of this round.
     *
     * @return the current game phase
     */
    public GamePhase getPhase() {
        return phase;
    }

    /**
     * Sets the current phase of this round.
     *
     * @param phase the new game phase
     */
    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    /**
     * Returns the draw pile for this round.
     *
     * @return the draw pile deque (top at {@link Deque#peek()})
     */
    public Deque<Card> getDrawPile() {
        return drawPile;
    }

    /**
     * Returns the discard pile for this round.
     *
     * @return the discard pile deque (top at {@link Deque#peek()})
     */
    public Deque<Card> getDiscardPile() {
        return discardPile;
    }

    /**
     * Returns the event card pile for this round.
     *
     * @return the event pile deque (top at {@link Deque#peek()})
     */
    public Deque<Card> getEventPile() {
        return eventPile;
    }

    /**
     * Returns the event card currently being resolved, or {@code null} if none.
     *
     * @return the active event card
     */
    public Card getActiveEventCard() {
        return activeEventCard;
    }

    /**
     * Sets the event card currently being resolved.
     *
     * <p>Set to the flipped event card before calling
     * {@link ch.unibas.dmi.dbis.cs108.example.model.game.EventResolver#resolve}, and
     * cleared to {@code null} after resolution is complete.</p>
     *
     * @param activeEventCard the event card to set, or {@code null} to clear it
     */
    public void setActiveEventCard(Card activeEventCard) {
        this.activeEventCard = activeEventCard;
    }

    /**
     * Returns the score threshold at which the game ends.
     *
     * @return the maximum score
     */
    public int getMaxScore() {
        return maxScore;
    }

    /**
     * Returns the color requested by Fantastic / Fantastic Four / Equality, or {@code null}.
     *
     * @return the requested color, or {@code null} if no color is requested
     */
    public CardColor getRequestedColor() {
        return requestedColor;
    }

    /**
     * Sets a color request for the next played card.
     *
     * @param requestedColor the required color, or {@code null} to clear the request
     */
    public void setRequestedColor(CardColor requestedColor) {
        this.requestedColor = requestedColor;
    }

    /**
     * Returns the number requested by Fantastic / Fantastic Four, or {@code null}.
     *
     * @return the requested number, or {@code null} if no number is requested
     */
    public Integer getRequestedNumber() {
        return requestedNumber;
    }

    /**
     * Sets a number request for the next played card.
     *
     * @param requestedNumber the required card value, or {@code null} to clear the request
     */
    public void setRequestedNumber(Integer requestedNumber) {
        this.requestedNumber = requestedNumber;
    }

    /**
     * Returns the stack of pending special effects waiting to be resolved.
     *
     * <p>Effects are pushed when a special card is played and popped as they are resolved.
     * A Counterattack may redirect but not remove the effect beneath it.</p>
     *
     * @return the pending-effects deque (top at {@link Deque#peek()})
     */
    public Deque<SpecialEffect> getPendingEffects() {
        return pendingEffects;
    }

    /**
     * Returns the name of the player targeted by the top pending effect, or {@code null}.
     *
     * @return the pending effect target, or {@code null} if none is set
     */
    public String getPendingEffectTarget() {
        return pendingEffectTarget;
    }

    /**
     * Sets the player targeted by the top pending effect.
     *
     * @param pendingEffectTarget the target player name, or {@code null} to clear it
     */
    public void setPendingEffectTarget(String pendingEffectTarget) {
        this.pendingEffectTarget = pendingEffectTarget;
    }

    /**
     * Returns whether playing special cards is currently blocked.
     *
     * @return {@code true} if {@link ch.unibas.dmi.dbis.cs108.example.model.game.CardType#SPECIAL_SINGLE}
     *         and {@link ch.unibas.dmi.dbis.cs108.example.model.game.CardType#SPECIAL_FOUR} cannot be played
     */
    public boolean isSpecialsBlocked() {
        return specialsBlocked;
    }

    /**
     * Sets whether playing special cards is blocked.
     *
     * @param specialsBlocked {@code true} to block special cards until the next black card
     */
    public void setSpecialsBlocked(boolean specialsBlocked) {
        this.specialsBlocked = specialsBlocked;
    }

    /**
     * Returns whether double scoring is active for this round.
     *
     * @return {@code true} if card scoring values are doubled at round end
     */
    public boolean isDoubleScoringActive() {
        return doubleScoringActive;
    }

    /**
     * Sets whether double scoring is active for this round.
     *
     * @param doubleScoringActive {@code true} to double all card scoring values at round end
     */
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

    /**
     * Returns the dev-mode forced event card id that should be used whenever
     * a black card is played, or {@code null} if normal event drawing is active.
     *
     * @return the forced event card id, or {@code null}
     */
    public Integer getForcedEventCardIdOnBlack() {
        return forcedEventCardIdOnBlack;
    }

    /**
     * Sets the dev-mode forced event card id that should be triggered whenever
     * a black card is played.
     *
     * <p>This is intended for development and testing only. When {@code null},
     * black cards use the normal event pile.</p>
     *
     * @param forcedEventCardIdOnBlack the forced event card id, or {@code null}
     */
    public void setForcedEventCardIdOnBlack(Integer forcedEventCardIdOnBlack) {
        this.forcedEventCardIdOnBlack = forcedEventCardIdOnBlack;
    }

}
