package fun.mike.frontier.alpha;

import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class SftpFileTransferClientTest {
    private final String LOCAL_FILE = "src/test/resources/local/foo.txt";
    private final SftpFileTransferClient sftpClient = new SftpFileTransferClient("localhost",
                                                                            8080,
                                                                            "foo",
                                                                            "baz");
    @Rule
    public final FakeSftpServerRule sftpServer = new FakeSftpServerRule().setPort(8080);

    @Test
    public void uploadLocalFile() throws FileTransferException, FileNotFoundException, IOException {
        
        final String PATH = "test/baz.txt";
        final String CONTENT = "baz.";
        
        
        String content = "this is a test";
        IO.spit(LOCAL_FILE,content);
        sftpClient.upload(LOCAL_FILE,"newTest","/");
        
        String serverContent = sftpServer.getFileContent("/newTest", UTF_8);
        
        assertTrue("File wasn't found at the root", sftpServer.existsFile("/newTest"));
        assertEquals(content, serverContent);
    }
    
    @Test
    public void downloadRemoteFile() throws IOException {
        String content = "this is a test";
        sftpServer.putFile("/bar",content,UTF_8);

        sftpClient.download("/bar","src/test/resources/local");
        
        try{
           IO.slurp("src/test/resources/local/bar");
        }catch(UncheckedIOException e){
            fail("SftpFileTransferClient's download method failed");
        }
        IO.nuke("src/test/resources/local/bar");
    }
    
}
