package fun.mike.frontier.alpha;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SftpFileTransferClientTest {
    private final String LOCAL_FILE = "local/foo.txt";
    private int PORT = 8080;
    @Rule
    public final FakeSftpServerRule server = new FakeSftpServerRule().setPort(PORT);

    public FileTransferClient client() {
        return SftpFileTransferClient.withPassword("localhost",
                                                   PORT,
                                                   "foo",
                                                   "baz");
    }

    @Before
    public void setUp() {
        IO.mkdir("local");
    }

    @After
    public void tearDown() {
        IO.nuke("local");
    }

    @Test
    public void uploadLocalFile() {
        final String PATH = "test/baz.txt";
        final String CONTENT = "baz.";

        String content = "this is a test";
        IO.spit(LOCAL_FILE, content);
        client().upload(LOCAL_FILE, "/newTest");

        String serverContent = getFileContent("/newTest");

        assertTrue("File wasn't found at the root.", server.existsFile("/newTest"));
        assertEquals(content, serverContent);
    }

    @Test
    public void downloadRemoteFile() throws IOException {
        String content = "this is a test";
        server.putFile("/bar", content, UTF_8);

        client().download("/bar", "local/bar");

        assertEquals(content, IO.slurp("local/bar"));
        IO.nuke("local/bar");
    }

    @Test
    public void assertFileExists() throws IOException {
        String content = "this is a test";
        server.putFile("/bar", content, UTF_8);

        boolean shouldBeTrue = client().fileExists("/bar");
        assertEquals(true, shouldBeTrue);

        boolean shouldBeFalse = client().fileExists("baboo");
        assertEquals(false, shouldBeFalse);

    }

    @Test
    public void delete() {
        FileTransferClient client = client();

        final String PATH = "baz.txt";
        final String CONTENT = "baz.";

        InputStream in = new ByteArrayInputStream(CONTENT.getBytes());
        assertEquals(PATH, client.upload(in, PATH));
        assertEquals(CONTENT, client.slurp(PATH));

        assertTrue(client.fileExists(PATH));

        client.delete(PATH);

        assertFalse(client.fileExists(PATH));
    }

    @Test
    public void list() {
        FileTransferClient client = client();
        final String CONTENT = "baz.";

        InputStream in = new ByteArrayInputStream(CONTENT.getBytes());

        client.upload(in, "/baz.txt");
        client.upload(in, "/foo.txt");
        client.upload(in, "/bar.txt");
        client.upload(in, "/qux.txt");

        List<FileInfo> results = client.list("/");

        List<String> fileNames = results.stream().map(result -> {
            return result.getName();
        }).collect(Collectors.toList());

        assertEquals(4, results.size() - 2); //taking into account "." and "home" directories that are always at the root
        assertTrue(fileNames.contains("baz.txt"));
        assertTrue(fileNames.contains("foo.txt"));
        assertTrue(fileNames.contains("bar.txt"));
        assertTrue(fileNames.contains("qux.txt"));
    }

    private String getFileContent(String path) {
        try {
            return server.getFileContent(path, UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
