package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameEndViewTest {

    @BeforeAll
    static void initJfx() {
        new JFXPanel(); // initializes JavaFX toolkit
    }

    @Test
    void rankingAndWinnerCanBeSet() {
        GameEndView view = new GameEndView();

        view.setWinner("Alice");
        view.setRanking(java.util.List.of("Alice", "Bob", "Charlie"));

        assertEquals("🏆 Winner: Alice", view.getWinnerLabel().getText());
        assertEquals(3, view.getRankingList().getItems().size());
    }
}