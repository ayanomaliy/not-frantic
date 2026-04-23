package ch.unibas.dmi.dbis.cs108.example.client.ui;

import com.fluxvend.svgfx.utils.SvgLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * A card-sized pane displaying the card backside asset.
 *
 * <p>Has the same fixed dimensions (90×130 px) as {@link CardView} so it can be
 * used as a face-down placeholder wherever a card slot is needed.</p>
 *
 * <p>Must be created on the JavaFX Application Thread.</p>
 */
public class CardBacksideView extends StackPane {

    static final double CARD_WIDTH = 90;
    static final double CARD_HEIGHT = 130;

    private static final String ASSET_PATH = "/icons/card_backside.svg";

    /**
     * Creates a new card backside view.
     *
     * <p>If the backside SVG asset is present on the classpath it is rendered inside the
     * pane. Construction succeeds silently if the asset is missing.</p>
     */
    public CardBacksideView() {
        getStyleClass().addAll("card-view", "card-backside");
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);

        if (getClass().getResource(ASSET_PATH) != null) {
            try {
                Image img = SvgLoader.getInstance().loadSvgImage(ASSET_PATH, null, false, CARD_WIDTH, null);
                ImageView imgView = new ImageView(img);
                imgView.setPreserveRatio(true);
                imgView.setFitWidth(CARD_WIDTH);
                imgView.setFitHeight(CARD_HEIGHT);
                imgView.setMouseTransparent(true);
                getChildren().add(imgView);
            } catch (Exception e) {
                System.err.println("[CardBacksideView] Cannot load backside asset: " + e.getMessage());
            }
        }
    }
}
