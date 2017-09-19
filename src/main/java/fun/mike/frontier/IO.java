package fun.mike.frontier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IO {
    public static void mkdir(String path) {
        new File(path).mkdir();
    }

    public static void copy(String srcPath, String destPath) {
        try (FileInputStream is = new FileInputStream(srcPath);
             FileChannel ic = is.getChannel();
             FileOutputStream os = new FileOutputStream(destPath);
             FileChannel oc = os.getChannel()) {
            oc.transferFrom(ic, 0, ic.size());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static String slurp(String path) {
        try (InputStream is = new URL(path).openConnection().getInputStream()) {
            return slurp(is);
        } catch (MalformedURLException mue) {
            try {
                return new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static String slurp(InputStream is) {
        try (Reader isReader = new InputStreamReader(is, "UTF-8");
             Reader reader = new BufferedReader(isReader)) {
            StringBuilder stringBuilder = new StringBuilder();
            int c = 0;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }

            return stringBuilder.toString();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static void nuke(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            for (String filePath : file.list()) {
                new File(file.getPath(), filePath).delete();
            }
        }
        file.delete();
    }

    public static void deleteQuietly(String path) {
        new File(path).delete();
    }

    public static Stream<String> streamLines(String path) {
        try {
            return Files.lines(Paths.get(path));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // Everything in memory
    public static List<List<String>> slurpHeadlessDelimited(String path,
                                                            String delimiter) {

        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            return lines
                    .map(line -> Arrays.asList(line.split(delimiter)))
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static void spitHeadlessDelimited(String path,
                                             String delimiter,
                                             List<List<String>> rows) {
        try (Writer out = new PrintWriter(path)) {
            String content = rows.stream()
                    .map(row -> String.join("|", row))
                    .collect(Collectors.joining("\n"));
            out.write(content);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
