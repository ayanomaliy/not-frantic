package ch.unibas.dmi.dbis.cs108.example.debug;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

public final class SvgRenderDebug {

    private static final String LOGO_PATH = "/icons/logo.svg";

    private SvgRenderDebug() {
    }

    public static void main(String[] args) {
        URL logoUrl = SvgRenderDebug.class.getResource(LOGO_PATH);

        System.out.println("Resource URL = " + logoUrl);

        if (logoUrl == null) {
            System.err.println("logo.svg was not found on the classpath.");
            return;
        }

        File outputFile = new File("debug-logo-output.png");

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            PNGTranscoder transcoder = new PNGTranscoder();

            transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, 512f);
            transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, 512f);

            TranscoderInput input = new TranscoderInput(logoUrl.toExternalForm());
            TranscoderOutput output = new TranscoderOutput(out);

            System.out.println("Starting Batik transcode...");
            transcoder.transcode(input, output);
            System.out.println("Done. Wrote: " + outputFile.getAbsolutePath());
            System.out.println("File size: " + outputFile.length() + " bytes");
        } catch (Exception e) {
            System.err.println("Batik failed while rendering logo.svg:");
            e.printStackTrace();
        }
    }
}