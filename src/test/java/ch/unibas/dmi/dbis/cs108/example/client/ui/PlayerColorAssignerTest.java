package ch.unibas.dmi.dbis.cs108.example.client.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerColorAssignerTest {

    @Test
    void assign_singleLocalPlayer_getsRed() {
        Map<String, String> result = PlayerColorAssigner.assign(List.of("Alice"), "Alice");
        assertEquals("red", result.get("Alice"));
    }

    @Test
    void assign_localPlayerAlwaysGetsRed_regardlessOfPosition() {
        Map<String, String> result = PlayerColorAssigner.assign(
                List.of("Alice", "Bob", "Charlie", "Dave"), "Bob");
        assertEquals("red", result.get("Bob"));
    }

    @Test
    void assign_fourPlayers_localGetsRed_noSharedColors() {
        List<String> names = List.of("Alice", "Bob", "Charlie", "Dave");
        Map<String, String> result = PlayerColorAssigner.assign(names, "Alice");

        assertEquals("red", result.get("Alice"));
        long uniqueColors = result.values().stream().distinct().count();
        assertEquals(4, uniqueColors);
    }

    @Test
    void assign_eightPlayers_noSharedColors() {
        List<String> names = List.of("A", "B", "C", "D", "E", "F", "G", "H");
        Map<String, String> result = PlayerColorAssigner.assign(names, "A");

        assertEquals("red", result.get("A"));
        long uniqueColors = result.values().stream().distinct().count();
        assertEquals(8, uniqueColors);
    }

    @Test
    void assign_emptyList_returnsEmptyMap() {
        assertTrue(PlayerColorAssigner.assign(List.of(), "Alice").isEmpty());
    }

    @Test
    void assign_localNotInList_noPlayerGetsRed() {
        Map<String, String> result = PlayerColorAssigner.assign(
                List.of("Alice", "Bob"), "Charlie");
        assertFalse(result.containsValue("red"));
    }

    @Test
    void assign_preservesInsertionOrder() {
        List<String> names = List.of("Alice", "Bob", "Charlie");
        Map<String, String> result = PlayerColorAssigner.assign(names, "Alice");
        assertEquals(names, List.copyOf(result.keySet()));
    }

    @Test
    void assign_knownColorOrder() {
        List<String> names = List.of("local", "p1", "p2", "p3");
        Map<String, String> result = PlayerColorAssigner.assign(names, "local");

        assertEquals("red",    result.get("local"));
        assertEquals("green",  result.get("p1"));
        assertEquals("blue",   result.get("p2"));
        assertEquals("yellow", result.get("p3"));
    }
}
