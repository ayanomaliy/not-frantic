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

    public String getPlayerName() {
        return playerName;
    }

    public List<Card> getHand() {
        return hand;
    }

    public int getHandSize() {
        return hand.size();
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void addToTotalScore(int points) {
        this.totalScore += points;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public boolean hasPlayedThisTurn() {
        return hasPlayedThisTurn;
    }

    public void setHasPlayedThisTurn(boolean hasPlayedThisTurn) {
        this.hasPlayedThisTurn = hasPlayedThisTurn;
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
