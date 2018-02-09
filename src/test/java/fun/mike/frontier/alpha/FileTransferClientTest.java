package fun.mike.frontier.alpha;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class FileTransferClientTest {
    private static final String USER = "bob";
    private static final String PASSWORD = "password";
    final String LOCAL_FILE = "local/foo.txt";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public abstract FileTransferClient client();

    @Before
    public void superSetUp() {
        IO.mkdir("local");
    }

    @After
    public void superTearDown() {
        IO.nuke("local");
        IO.deleteQuietly(LOCAL_FILE);
    }

    @Test
    public void stream() {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals("foo.", IO.slurp(client().stream("test/foo.txt")));
    }

    @Test
    public void optionalStream() {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals("foo.", IO.slurp(client().optionalStream("test/foo.txt").get()));
    }

    @Test
    public void optionalStreamNotFound() {
        assertFalse(client().optionalStream("elkawrjwa").isPresent());
    }

    @Test
    public void streamNotFound() {
        thrown.expect(MissingRemoteFileException.class);
        thrown.expectMessage(new RegexMatcher("Remote file localhost:[0-9]+:elkawrjwa not found."));
        client().stream("elkawrjwa");
    }

    @Test
    public void list() {
        List<FileInfo> files = client().list("test");

        assertEquals(2, files.size());

        FileInfo foo = files.get(0);
        assertEquals("foo.txt", foo.getName());
        assertEquals(new Long(4), foo.getSize());

        FileInfo bar = files.get(1);
        assertEquals("bar.txt", bar.getName());
        assertEquals(new Long(5), bar.getSize());
    }

    @Test
    public void download() {
        OutputStream out = new ByteArrayOutputStream();
        client().optionalDownload("test/foo.txt",
                                  out);
        assertEquals("foo.", out.toString());
    }

    @Test
    public void downloadOptionalNonexistentFile() {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals(Optional.empty(), client().optionalDownload("foo", os));
    }

    @Test
    public void downloadNonexistentFile() {
        thrown.expect(MissingRemoteFileException.class);
        thrown.expectMessage(new RegexMatcher("Remote file localhost:[0-9]+:foo not found."));
        OutputStream os = new ByteArrayOutputStream();
        client().download("foo", os);
    }

    @Test
    public void dirExists() {
        FileTransferClient client = client();
        assertTrue(client.dirExists("test"));
        assertFalse(client.dirExists("fake"));
    }

    @Test
    public void fileExists() {
        FileTransferClient client = client();
        assertTrue(client.dirExists("test"));
        assertFalse(client.dirExists("fake"));
    }

    @Test
    public void downloadToFile() {
        final String FTP_PATH = "test/foo.txt";

        OutputStream out = new ByteArrayOutputStream();
        assertTrue(client().optionalDownload(FTP_PATH, LOCAL_FILE));
        assertEquals("foo.", IO.slurp(LOCAL_FILE));
    }

    @Test
    public void downloadToPath() {
        final String FTP_PATH = "test/foo.txt";

        client().download(FTP_PATH, LOCAL_FILE);
        assertEquals("foo.", IO.slurp(LOCAL_FILE));
    }

    @Test
    public void downloadToPathFileNotFoundOnHost() {
        thrown.expect(MissingRemoteFileException.class);
        thrown.expectMessage(new RegexMatcher("Remote file localhost:[0-9]+:test/ekajrka.txt not found."));

        final String FTP_PATH = "test/ekajrka.txt";
        final String LOCAL_FILE = "local/foo.txt";
        client().download(FTP_PATH, LOCAL_FILE);
    }

    @Test
    public void upload() {
        FileTransferClient client = client();

        final String PATH = "test/baz.txt";
        final String CONTENT = "baz.";

        InputStream in = new ByteArrayInputStream(CONTENT.getBytes());
        assertEquals(PATH, client.upload(in, PATH));
        assertEquals(CONTENT, client.slurp(PATH));
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
    public void uploadLocalFile() {
        FileTransferClient client = client();

        final String PATH = "test/baz.txt";
        final String CONTENT = "baz.";

        IO.spit(LOCAL_FILE, CONTENT);
        InputStream in = new ByteArrayInputStream(CONTENT.getBytes());
        assertEquals(PATH, client.upload(in, PATH));
        assertEquals(CONTENT, client.slurp(PATH));
    }

    @Test
    public void uploadPathNotFound() {
        thrown.expect(FileTransferException.class);
        thrown.expectMessage("Unexpected reply: 553 [c:\\home\\blaoewa] is not a directory or does not exist.");
        InputStream in = new ByteArrayInputStream("lekajwel".getBytes());
        client().upload(in, "blaoewa/elaker.txt");
    }
}
