package ch.unibas.dmi.dbis.cs108.example.prototype.model;

/**
 * Represents a player in the game.
 * <p>
 * This is an immutable record containing the player's display name.
 * </p>
 *
 * @param name the player's display name
 */
public record Player(String name) {
}