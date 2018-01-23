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

public class FtpFileTransferClientTest extends FileTransferClientTest {
    private static final String USER = "bob";
    private static final String PASSWORD = "password";
    final String LOCAL_FILE = "local/foo.txt";

    private FakeFtpServer ftpServer;
    private FtpFileTransferClient client;
    private int port;

    @Before
    public void setUp() throws Exception {
        ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(0);
        ftpServer.addUserAccount(new UserAccount(USER, PASSWORD, "c:\\home"));

        FileSystem fileSystem = new WindowsFakeFileSystem();
        fileSystem.add(new DirectoryEntry("c:\\home"));
        fileSystem.add(new FileEntry("c:\\home\\test\\foo.txt", "foo."));
        fileSystem.add(new FileEntry("c:\\home\\test\\bar.txt", "bar!!"));
        ftpServer.setFileSystem(fileSystem);

        ftpServer.start();

        port = ftpServer.getServerControlPort();
    }

    @Override
    public FileTransferClient client() {
        return new FtpFileTransferClient("localhost",
                                           port,
                                           USER,
                                           PASSWORD);
    }

    @Test
    public void downloadFailure() throws FileTransferException {
        thrown.expect(FileTransferException.class);
        OutputStream os = new ByteArrayOutputStream();
        new FtpFileTransferClient("eakw", "foo", "bar").optionalDownload("foo", os);
    }


    @Test
    public void listDirDoesNotExist() throws Exception {
        thrown.expect(FileTransferException.class);
        thrown.expectMessage(String.format("Directory localhost:%d:kelawjrlka does not exist.", port));
        client().list("kelawjrlka");
    }
}
