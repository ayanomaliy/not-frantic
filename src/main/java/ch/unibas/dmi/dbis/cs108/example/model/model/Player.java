package ch.unibas.dmi.dbis.cs108.example.model.model;

/**
 * Represents a player in the game.
 * <p>
 * This record encapsulates the basic information of a player, including their name.
 * Instances of this record are immutable and used throughout the server for managing
 * player state and communicating player information to clients.
 * </p>
 *
 * @param name the name of the player
 */
public record Player(String name) {
}