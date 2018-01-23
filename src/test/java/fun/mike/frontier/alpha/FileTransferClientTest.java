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
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.WindowsFakeFileSystem;

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

    @After
    public void superTearDown() {
        IO.deleteQuietly(LOCAL_FILE);
    }

    @Test
    public void stream() throws FileTransferException,
            MissingFileException {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals("foo.", IO.slurp(client().stream("test/foo.txt")));
    }

    @Test
    public void optionalStream() throws FileTransferException {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals("foo.", IO.slurp(client().optionalStream("test/foo.txt").get()));
    }

    @Test
    public void optionalStreamNotFound() throws FileTransferException {
        assertFalse(client().optionalStream("elkawrjwa").isPresent());
    }

    @Test
    public void streamNotFound() throws FileTransferException,
            MissingFileException {
        thrown.expect(MissingFileException.class);
        thrown.expectMessage("File elkawrjwa not found.");
        client().stream("elkawrjwa");
    }

    @Test
    public void list() throws Exception {
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
    public void download() throws FileTransferException {
        OutputStream out = new ByteArrayOutputStream();
        client().optionalDownload("test/foo.txt",
                                out);
        assertEquals("foo.", out.toString());
    }

    @Test
    public void downloadOptionalNonexistentFile() throws FileTransferException {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals(Optional.empty(), client().optionalDownload("foo", os));
    }

    @Test
    public void downloadNonexistentFile() throws FileTransferException,
            MissingFileException {
        thrown.expect(MissingFileException.class);
        thrown.expectMessage("foo");
        OutputStream os = new ByteArrayOutputStream();
        client().download("foo", os);
    }

    @Test
    public void dirExists() throws FileTransferException {
        FileTransferClient client = client();
        assertTrue(client.dirExists("test"));
        assertFalse(client.dirExists("fake"));
    }

    @Test
    public void downloadToFile() throws FileTransferException {
        final String FTP_PATH = "test/foo.txt";

        OutputStream out = new ByteArrayOutputStream();
        assertTrue(client().optionalDownload(FTP_PATH, LOCAL_FILE));
        assertEquals("foo.", IO.slurp(LOCAL_FILE));
    }

    @Test
    public void downloadToPath() throws FileTransferException,
            MissingFileException {
        final String FTP_PATH = "test/foo.txt";

        client().download(FTP_PATH, LOCAL_FILE);
        assertEquals("foo.", IO.slurp(LOCAL_FILE));
    }

    @Test
    public void downloadToPathFileNotFoundOnHost() throws FileTransferException,
            MissingFileException {
        thrown.expect(MissingFileException.class);
        thrown.expectMessage("File test/ekajrka.txt not found.");

        final String FTP_PATH = "test/ekajrka.txt";
        final String LOCAL_FILE = "local/foo.txt";
        client().download(FTP_PATH, LOCAL_FILE);
    }

    @Test
    public void upload() throws FileTransferException, MissingFileException {
        FileTransferClient client = client();

        final String PATH = "test/baz.txt";
        final String CONTENT = "baz.";

        InputStream in = new ByteArrayInputStream(CONTENT.getBytes());
        assertEquals(PATH, client.upload(in, PATH));
        assertEquals(CONTENT, client.slurp(PATH));
    }

    @Test
    public void uploadLocalFile() throws FileTransferException, MissingFileException {
        FileTransferClient client = client();

        final String PATH = "test/baz.txt";
        final String CONTENT = "baz.";

        IO.spit(LOCAL_FILE, CONTENT);
        InputStream in = new ByteArrayInputStream(CONTENT.getBytes());
        assertEquals(PATH, client.upload(in, PATH));
        assertEquals(CONTENT, client.slurp(PATH));
    }

    @Test
    public void uploadPathNotFound() throws FileTransferException {
        thrown.expect(FileTransferException.class);
        thrown.expectMessage("Unexpected reply: 553 [c:\\home\\blaoewa] is not a directory or does not exist.");
        InputStream in = new ByteArrayInputStream("lekajwel".getBytes());
        client().upload(in, "blaoewa/elaker.txt");
    }
}
