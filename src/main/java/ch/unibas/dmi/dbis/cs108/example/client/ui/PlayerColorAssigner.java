package ch.unibas.dmi.dbis.cs108.example.client.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless utility that maps player names to display colors.
 *
 * <p>The local user is always assigned {@code "red"}. All other players
 * receive the remaining colors in the order they appear in the name list.
 * Colors cycle through the non-red palette if there are more than seven
 * opponents.</p>
 */
public final class PlayerColorAssigner {

    static final List<String> COLORS = List.of(
            "red", "green", "blue", "yellow", "purple", "pink", "orange", "teal"
    );

    private PlayerColorAssigner() {}

    /**
     * Assigns a color string to each player name.
     *
     * @param orderedNames all player names in turn order (may include the local user)
     * @param localUsername the name of the local user; receives {@code "red"}
     * @return a {@link LinkedHashMap} from player name to color string, preserving insertion order
     */
    public static Map<String, String> assign(List<String> orderedNames, String localUsername) {
        Map<String, String> result = new LinkedHashMap<>();
        int otherIndex = 0;
        for (String name : orderedNames) {
            if (name.equals(localUsername)) {
                result.put(name, COLORS.get(0));
            } else {
                result.put(name, COLORS.get(1 + (otherIndex % (COLORS.size() - 1))));
                otherIndex++;
            }
        }
        return result;
    }
}
