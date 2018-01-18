package fun.mike.frontier.alpha;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SftpFileTransferClient {
    private static final Logger log = LoggerFactory.getLogger(FileTransferClient.class);

    private final String host;
    private final Integer port;
    private final String username;
    private final String password;

    public SftpFileTransferClient(String host, Integer port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public SftpFileTransferClient(String host, String username, String password) {
        this.host = host;
        this.port = 8765;
        this.username = username;
        this.password = password;
    }

    public String upload(String source, String fileName, String dest) throws FileTransferException {
        ConnectionProp con = connect();
        
        try (InputStream is = new FileInputStream(source)){
            ChannelSftp chan = con.getChannel();
            chan.cd(dest);
            
            chan.put(is, fileName);
            log.info("File transferred successfully to host.");
            return null;
        } catch (SftpException e) {
            String message = "There was an issue executing sftp commands.";
            log.warn(message);
            throw new FileTransferException(message, e);
        } catch (java.io.FileNotFoundException e) {
            String message = String.format("Failed to read file at %s .", source);
            log.warn(message);
            throw new FileTransferException(message, e);
        } catch (IOException e) {
            String message = String.format("Failed to read file at %s .", source);
            log.warn(message);
            throw new FileTransferException(message, e);
        } finally {
           disconnect(con);
        }
    }

    public void download(String path,String localPath) throws FileTransferException {
        ConnectionProp con = connect();
        ChannelSftp chan = con.getChannel();

        try {
            chan.get(path,localPath); 
        } catch (SftpException e) {
            String message = String.format("Failed to retrieve file at path %s",path);
            log.warn(message);
            throw new FileTransferException(message, e);
        }finally {
            disconnect(con);
        }
    }

    private ConnectionProp connect() {
        try {
            Session session = new JSch().getSession(this.username, this.host, this.port);
            session.setPassword(this.password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            return new ConnectionProp(session, channelSftp);
        } catch (JSchException e) {
            String message = "Jsch failed to set up connection.";
            log.warn(message);
            throw new FileTransferException(message, e);
        }
    }
    
    private void disconnect(ConnectionProp connectionProp){
        connectionProp.getSession().disconnect();
        connectionProp.getChannel().disconnect();
    }

}
