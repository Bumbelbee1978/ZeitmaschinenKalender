import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class ImageLoader {
    private ImageLoader() {}

    public static BufferedImage loadOrThrow(String path) {
        BufferedImage img = loadOptional(path);
        if (img == null) throw new IllegalStateException("Bild nicht gefunden/lesbar: " + path);
        return img;
    }

    public static BufferedImage loadOptional(String path) {
        if (path == null || path.isBlank()) return null;

        // 1) Erst als Resource laden (funktioniert in JAR/jpackage stabil)
        String resPath = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = ImageLoader.class.getClassLoader();

        try (InputStream in = cl.getResourceAsStream(resPath)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (IOException ignored) {
            return null;
        }

        // 2) Fallback: als Datei laden (praktisch während Entwicklung oder wenn assets neben der App liegen)
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            return ImageIO.read(f);
        } catch (IOException ignored) {
            return null;
        }
    }
}