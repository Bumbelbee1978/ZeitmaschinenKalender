import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class EntryStoreIO {
    private EntryStoreIO() {}

    private static final String DIR_NAME = ".zeitmaschine-kalender";
    private static final String FILE_NAME = "entries.properties";

    public static Path defaultFilePath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, DIR_NAME, FILE_NAME);
    }

    public static Map<LocalDate, List<String>> loadOrEmpty() {
        Path file = defaultFilePath();
        if (!Files.exists(file)) return new HashMap<>();

        Properties p = new Properties();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            p.load(in);
        } catch (IOException ignored) {
            return new HashMap<>();
        }

        Map<LocalDate, List<String>> result = new HashMap<>();
        for (String key : p.stringPropertyNames()) {
            try {
                LocalDate date = LocalDate.parse(key);
                String encoded = p.getProperty(key, "");
                if (encoded.isBlank()) continue;

                String decoded = new String(
                        Base64.getDecoder().decode(encoded),
                        java.nio.charset.StandardCharsets.UTF_8
                );
                if (decoded.isBlank()) continue;

                List<String> items = new ArrayList<>();
                for (String line : decoded.split("\n", -1)) {
                    String s = line.strip();
                    if (!s.isEmpty()) items.add(s);
                }
                if (!items.isEmpty()) result.put(date, items);
            } catch (Exception ignored) {
                // ungültige Keys/Values ignorieren
            }
        }
        return result;
    }

    public static void save(Map<LocalDate, List<String>> store) {
        Path file = defaultFilePath();
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException ignored) {
            return;
        }

        Properties p = new Properties();
        for (Map.Entry<LocalDate, List<String>> e : store.entrySet()) {
            LocalDate date = e.getKey();
            List<String> items = e.getValue();
            if (date == null || items == null || items.isEmpty()) continue;

            String joined = String.join("\n", items);
            String encoded = Base64.getEncoder().encodeToString(
                    joined.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            p.setProperty(date.toString(), encoded);
        }

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(
                file,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        ))) {
            p.store(out, "Zeitmaschine Kalender - Day Entries");
        } catch (IOException ignored) {
            // bei Fehlern still bleiben
        }
    }
}