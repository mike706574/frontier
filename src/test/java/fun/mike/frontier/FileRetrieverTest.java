package fun.mike.frontier;

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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class FileRetrieverTest {
    private static final String USER = "bob";
    private static final String PASSWORD = "password";
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private FakeFtpServer fakeFtpServer;
    private FileRetriever retriever;

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

        retriever = new FileRetriever("localhost",
                fakeFtpServer.getServerControlPort(),
                USER,
                PASSWORD);
    }

    @After
    public void tearDown() {
        fakeFtpServer.stop();
    }

    @Test
    public void stream() {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals("foo.",
                IO.slurp(retriever.stream("test\\foo.txt").get()));
    }

    @Test
    public void streamFailure() {
        assertFalse(retriever.stream("elkawrjwa").isPresent());
    }

    @Test
    public void list() throws Exception {
        List<FileInfo> files = retriever.list("test");

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
        retriever.download("test\\foo.txt",
                out);
        assertEquals("foo.", out.toString());
    }

    @Test
    public void downloadNonexistentFile() {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals(Optional.empty(), retriever.download("foo", os));
    }


    @Test
    public void downloadFailure() {
        thrown.expect(FileRetrieverException.class);

        OutputStream os = new ByteArrayOutputStream();
        new FileRetriever("eakw", "foo", "bar")
                .download("foo", os);
    }

    @Test
    public void downloadToFile() {
        String localPath = "local/foo.txt";
        try {
            OutputStream out = new ByteArrayOutputStream();
            assertTrue(retriever.download("test\\foo.txt", localPath));
            assertEquals("foo.", IO.slurp(localPath));
        } finally {
            IO.deleteQuietly(localPath);
        }
    }
}
