import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/** Run with: java -cp core/build/libs/AabResGuard-VERSION.jar scripts/SmokeImageTools.java */
public class SmokeImageTools {
    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("aabresguard-jar-smoke-");
        try {
            Class<?> tools = Class.forName("com.bytedance.android.aabresguard.executors.BundledImageTools");
            java.lang.reflect.Method extract = tools.getDeclaredMethod("extract", Path.class);
            extract.setAccessible(true);
            Path directory = (Path) extract.invoke(null, work);
            for (String tool : Arrays.asList("optipng", "jpegtran", "gifsicle", "cwebp", "dwebp")) {
                String suffix = System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
                ProcessBuilder builder = new ProcessBuilder(directory.resolve(tool + suffix).toString(),
                        tool.equals("gifsicle") ? "--version" : "-version");
                builder.environment().put("PATH", "");
                if (System.getProperty("os.name").startsWith("Linux")) {
                    builder.environment().put("LD_LIBRARY_PATH", directory.toString());
                }
                Process process = builder.inheritIO().start();
                try {
                    if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
                        throw new IllegalStateException("Bundled tool failed: " + tool);
                    }
                } finally {
                    process.destroyForcibly();
                }
            }
            System.out.println("Packaged tools work without PATH or external installations.");
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(work)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
            }
        }
    }
}
