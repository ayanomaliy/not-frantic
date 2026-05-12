package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

/**
 * A card-sized pane displaying the backside of a card.
 *
 * <p>This view is used for opponent hands, the draw pile, and draw animations.
 * The card surface itself is styled through CSS using {@code card-backside}.
 * The center logo is loaded from {@code logo.svg}.</p>
 *
 * <p><strong>Implementation note:</strong> Normally, visual styling belongs in
 * {@code frantic-theme.css}. The logo rendering is an exception because the logo
 * SVG is complex and uses features such as gradients, filters, reusable
 * definitions, and a large view box. The lightweight SVG loader used elsewhere in
 * the project is reliable for simple card icons, but not for this full logo. A
 * {@code WebView} can render it, but introduced a white background rectangle.
 * Therefore this class uses Apache Batik to render the SVG once into an in-memory
 * transparent PNG, then displays that result with a normal JavaFX {@link ImageView}.
 * The size, opacity, and card surface styling still remain controlled by CSS.</p>
 *
 * <p>Must be created on the JavaFX Application Thread.</p>
 */
public class CardBacksideView extends StackPane {

    public static final double CARD_WIDTH = 90;
    public static final double CARD_HEIGHT = 130;

    private static final String LOGO_PATH = "/icons/logo.svg";

    /**
     * Render larger than displayed so the small card-back logo stays sharp.
     */
    private static final float RENDERED_LOGO_SIZE = 512f;

    private static Image cachedLogoImage;

    private static final double LOGO_DISPLAY_SIZE = 82;

    /**
     * Creates a new card backside view.
     */
    public CardBacksideView() {
        getStyleClass().addAll("card-view", "card-backside", "game-card-button");

        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);

        setFocusTraversable(false);
        setMouseTransparent(true);
        setAlignment(Pos.CENTER);

        addLogoOrFallback();
    }

    private void addLogoOrFallback() {
        Image logoImage = getOrLoadLogoImage();

        if (logoImage == null) {
            addFallbackLogo();
            return;
        }

        StackPane logoBox = new StackPane();
        logoBox.getStyleClass().add("card-backside-logo-box");
        logoBox.setMouseTransparent(true);

        logoBox.setPrefSize(LOGO_DISPLAY_SIZE, LOGO_DISPLAY_SIZE);
        logoBox.setMinSize(LOGO_DISPLAY_SIZE, LOGO_DISPLAY_SIZE);
        logoBox.setMaxSize(LOGO_DISPLAY_SIZE, LOGO_DISPLAY_SIZE);

        ImageView logo = new ImageView(logoImage);
        logo.getStyleClass().add("card-backside-logo-image");
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setMouseTransparent(true);

        logo.fitWidthProperty().bind(logoBox.widthProperty());
        logo.fitHeightProperty().bind(logoBox.heightProperty());

        logoBox.getChildren().add(logo);
        getChildren().add(logoBox);
    }

    private static Image getOrLoadLogoImage() {
        if (cachedLogoImage != null) {
            return cachedLogoImage;
        }

        URL logoUrl = CardBacksideView.class.getResource(LOGO_PATH);

        if (logoUrl == null) {
            System.err.println("[CardBacksideView] Logo resource not found: " + LOGO_PATH);
            return null;
        }

        try {
            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, RENDERED_LOGO_SIZE);
            transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, RENDERED_LOGO_SIZE);

            TranscoderInput input = new TranscoderInput(logoUrl.toExternalForm());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(outputStream);

            transcoder.transcode(input, output);
            outputStream.flush();

            byte[] pngBytes = outputStream.toByteArray();

            if (pngBytes.length == 0) {
                System.err.println("[CardBacksideView] Batik produced an empty PNG for: " + LOGO_PATH);
                return null;
            }

            cachedLogoImage = new Image(new ByteArrayInputStream(pngBytes));
            return cachedLogoImage;
        } catch (Exception e) {
            System.err.println("[CardBacksideView] Could not render SVG logo with Batik: " + logoUrl);
            e.printStackTrace();
            return null;
        }
    }

    private void addFallbackLogo() {
        Label fallbackLogo = new Label("F");
        fallbackLogo.getStyleClass().add("card-backside-logo-text");
        fallbackLogo.setMouseTransparent(true);
        getChildren().add(fallbackLogo);
    }
}