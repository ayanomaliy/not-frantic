package ch.unibas.dmi.dbis.cs108.example.client.ui;

import javafx.scene.image.Image;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for rendering complex SVG resources into JavaFX images.
 *
 * <p>JavaFX does not reliably load complex SVG files directly, especially when
 * they contain gradients, filters, symbols, reused shapes, or large view boxes.
 * This helper uses Apache Batik to rasterize an SVG resource into an in-memory
 * transparent PNG and then exposes it as a normal JavaFX {@link Image}.</p>
 *
 * <p>The rendered image is cached by resource path and render size, so repeated
 * UI components do not re-render the same SVG every time.</p>
 */
final class SvgImageLoader {

    private static final Map<String, Image> CACHE = new HashMap<>();

    private SvgImageLoader() {
        // Utility class.
    }

    static Image loadSvgAsImage(Class<?> owner, String resourcePath, float renderWidth, float renderHeight) {
        String cacheKey = resourcePath + "@" + renderWidth + "x" + renderHeight;

        if (CACHE.containsKey(cacheKey)) {
            return CACHE.get(cacheKey);
        }

        URL svgUrl = owner.getResource(resourcePath);

        if (svgUrl == null) {
            System.err.println("[SvgImageLoader] SVG resource not found: " + resourcePath);
            return null;
        }

        try {
            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, renderWidth);
            transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, renderHeight);

            TranscoderInput input = new TranscoderInput(svgUrl.toExternalForm());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(outputStream);

            transcoder.transcode(input, output);
            outputStream.flush();

            byte[] pngBytes = outputStream.toByteArray();

            if (pngBytes.length == 0) {
                System.err.println("[SvgImageLoader] Batik produced an empty PNG for: " + resourcePath);
                return null;
            }

            Image image = new Image(new ByteArrayInputStream(pngBytes));

            if (image.isError()) {
                System.err.println("[SvgImageLoader] JavaFX could not decode rendered image: " + resourcePath);
                if (image.getException() != null) {
                    image.getException().printStackTrace();
                }
                return null;
            }

            CACHE.put(cacheKey, image);
            return image;
        } catch (Exception e) {
            System.err.println("[SvgImageLoader] Could not render SVG: " + resourcePath);
            e.printStackTrace();
            return null;
        }
    }
}