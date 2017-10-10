package fun.mike.frontier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

public class FileTransferClientTest {
    private static final String USER = "bob";
    private static final String PASSWORD = "password";

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private FakeFtpServer fakeFtpServer;
    private FileTransferClient client;
    private int port;

    @Before
    public void setUp() {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.setServerControlPort(0);
        fakeFtpServer.addUserAccount(new UserAccount(USER, PASSWORD, "c:\\home"));

        FileSystem fileSystem = new WindowsFakeFileSystem();
        fileSystem.add(new DirectoryEntry("c:\\home"));
        fileSystem.add(new FileEntry("c:\\home\\test\\foo.txt", "foo."));
        fileSystem.add(new FileEntry("c:\\home\\test\\bar.txt", "bar!!"));
        fakeFtpServer.setFileSystem(fileSystem);

        fakeFtpServer.start();

        port = fakeFtpServer.getServerControlPort();
        client = new FileTransferClient("localhost",
                port,
                USER,
                PASSWORD);
    }

    @After
    public void tearDown() {
        fakeFtpServer.stop();
    }

    @Test
    public void stream() throws FileTransferException,
            FileNotFoundException {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals("foo.", IO.slurp(client.stream("test/foo.txt")));
    }

    @Test
    public void optionalStream() throws FileTransferException {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals("foo.", IO.slurp(client.optionalStream("test/foo.txt").get()));
    }

    @Test
    public void optionalStreamNotFound() throws FileTransferException {
        assertFalse(client.optionalStream("elkawrjwa").isPresent());
    }

    @Test
    public void streamNotFound() throws FileTransferException,
            FileNotFoundException {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("File elkawrjwa not found.");
        client.stream("elkawrjwa");
    }

    @Test
    public void list() throws Exception {
        List<FileInfo> files = client.list("test");

        assertEquals(2, files.size());

        FileInfo foo = files.get(0);
        assertEquals("foo.txt", foo.getName());
        assertEquals(new Long(4), foo.getSize());

        FileInfo bar = files.get(1);
        assertEquals("bar.txt", bar.getName());
        assertEquals(new Long(5), bar.getSize());
    }

    @Test
    public void listDirDoesNotExist() throws Exception {
        thrown.expect(FileTransferException.class);
        thrown.expectMessage(String.format("Directory localhost:%d:kelawjrlka does not exist.", port));
        client.list("kelawjrlka");
    }

    @Test
    public void download() throws FileTransferException {
        OutputStream out = new ByteArrayOutputStream();
        client.optionalDownload("test/foo.txt",
                out);
        assertEquals("foo.", out.toString());
    }

    @Test
    public void downloadOptionalNonexistentFile() throws FileTransferException {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals(Optional.empty(), client.optionalDownload("foo", os));
    }

    @Test
    public void downloadNonexistentFile() throws FileTransferException,
            FileNotFoundException {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("foo");
        OutputStream os = new ByteArrayOutputStream();
        client.download("foo", os);
    }

    @Test
    public void dirExists() throws FileTransferException {
        assertTrue(client.dirExists("test"));
        assertFalse(client.dirExists("fake"));
    }

    @Test
    public void downloadFailure() throws FileTransferException {
        thrown.expect(FileTransferException.class);
        OutputStream os = new ByteArrayOutputStream();
        new FileTransferClient("eakw", "foo", "bar").optionalDownload("foo", os);
    }

    @Test
    public void downloadToFile() throws FileTransferException {
        final String FTP_PATH = "test/foo.txt";
        final String LOCAL_PATH = "local/foo.txt";
        try {
            OutputStream out = new ByteArrayOutputStream();
            assertTrue(client.optionalDownload(FTP_PATH, LOCAL_PATH));
            assertEquals("foo.", IO.slurp(LOCAL_PATH));
        } finally {
            IO.deleteQuietly(LOCAL_PATH);
        }
    }

    @Test
    public void upload() throws FileTransferException, FileNotFoundException {
        final String PATH = "test/baz.txt";
        final String CONTENT = "baz.";

        InputStream in = new ByteArrayInputStream(CONTENT.getBytes());
        assertEquals(PATH, client.upload(in, PATH));
        assertEquals(CONTENT, client.slurp(PATH));
    }

    @Test
    public void uploadLocalFile() throws FileTransferException, FileNotFoundException {

        final String PATH = "test/baz.txt";
        final String LOCAL_PATH = "local/baz.txt";
        final String CONTENT = "baz.";

        try {
            IO.spit(LOCAL_PATH, CONTENT);
            InputStream in = new ByteArrayInputStream(CONTENT.getBytes());
            assertEquals(PATH, client.upload(in, PATH));
            assertEquals(CONTENT, client.slurp(PATH));
        }
        finally {
            IO.deleteQuietly(LOCAL_PATH);
        }
    }

    @Test
    public void uploadPathNotFound() throws FileTransferException {
        thrown.expect(FileTransferException.class);
        thrown.expectMessage("Unexpected reply: 553 [c:\\home\\blaoewa] is not a directory or does not exist.");
        InputStream in = new ByteArrayInputStream("lekajwel".getBytes());
        client.upload(in, "blaoewa/elaker.txt");
    }
}
