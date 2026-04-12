package ch.unibas.dmi.dbis.cs108.example.model.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the mutable in-game state for a single player during a round.
 * This is separate from the persistent {@code Player} model, which only
 * carries the player's name across rounds.
 */
public class PlayerGameState {

    private final String playerName;
    private final List<Card> hand;
    private int totalScore;
    private boolean skipped;
    private boolean hasPlayedThisTurn;
    private boolean hasDrawnThisTurn;

    /**
     * Creates a new player state with a cumulative score of zero.
     *
     * @param playerName the display name of the player
     */
    public PlayerGameState(String playerName) {
        this.playerName = playerName;
        this.hand = new ArrayList<>();
        this.totalScore = 0;
        this.skipped = false;
        this.hasPlayedThisTurn = false;
    }

    /** Creates a state carrying an existing cumulative score into a new round. */
    public PlayerGameState(String playerName, int totalScore) {
        this(playerName);
        this.totalScore = totalScore;
    }

    /**
     * Returns the display name of the player.
     *
     * @return the player name
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Returns the player's current hand.
     *
     * @return the mutable list of cards in hand
     */
    public List<Card> getHand() {
        return hand;
    }

    /**
     * Returns the number of cards currently in the player's hand.
     *
     * @return the hand size
     */
    public int getHandSize() {
        return hand.size();
    }

    /**
     * Returns the player's cumulative score across all completed rounds.
     *
     * @return the total score
     */
    public int getTotalScore() {
        return totalScore;
    }

    /**
     * Adds points to the player's cumulative score.
     *
     * @param points the number of points to add (may be negative)
     */
    public void addToTotalScore(int points) {
        this.totalScore += points;
    }

    /**
     * Returns whether this player's next turn is skipped.
     *
     * @return {@code true} if the player must skip their next turn
     */
    public boolean isSkipped() {
        return skipped;
    }

    /**
     * Sets whether this player's next turn should be skipped.
     *
     * @param skipped {@code true} to mark the player as skipped
     */
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    /**
     * Returns whether the player has already played a card during the current turn.
     *
     * @return {@code true} if a card was played this turn
     */
    public boolean hasPlayedThisTurn() {
        return hasPlayedThisTurn;
    }

    /**
     * Sets whether the player has played a card during the current turn.
     *
     * @param hasPlayedThisTurn {@code true} if a card was played this turn
     */
    public void setHasPlayedThisTurn(boolean hasPlayedThisTurn) {
        this.hasPlayedThisTurn = hasPlayedThisTurn;
    }

    /**
     * Returns whether the player has already drawn a card during the current turn.
     *
     * @return {@code true} if a card was drawn this turn
     */
    public boolean hasDrawnThisTurn() {
        return hasDrawnThisTurn;
    }

    /**
     * Sets whether the player has drawn a card during the current turn.
     *
     * @param hasDrawnThisTurn {@code true} if a card was drawn this turn
     */
    public void setHasDrawnThisTurn(boolean hasDrawnThisTurn) {
        this.hasDrawnThisTurn = hasDrawnThisTurn;
    }

    /** Adds a card to the player's hand. */
    public void addCard(Card card) {
        hand.add(card);
    }

    /** Removes and returns the specified card from the player's hand.
     *  @throws IllegalArgumentException if the card is not in hand */
    public Card removeCard(Card card) {
        if (!hand.remove(card)) {
            throw new IllegalArgumentException(
                    playerName + " does not hold card id=" + card.id());
        }
        return card;
    }
}
