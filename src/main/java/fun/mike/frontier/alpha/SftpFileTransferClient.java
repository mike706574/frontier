package fun.mike.frontier.alpha;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import fun.mike.frontier.impl.alpha.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class SftpFileTransferClient implements FileTransferClient {
    private static final Logger log = LoggerFactory.getLogger(FtpFileTransferClient.class);

    private final String host;
    private final Integer port;
    private final String username;
    private final String password;
    private final String privateKeyPath;
    private final String publicKeyPath;
    private final byte[] passphrase;

    public SftpFileTransferClient(String host,
            Integer port,
            String username,
            String password,
            String privateKeyPath,
            String publicKeyPath,
            byte[] passphrase) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKeyPath = privateKeyPath;
        this.publicKeyPath = publicKeyPath;
        this.passphrase = passphrase;
    }

    public static SftpFileTransferClient withKeys(String host,
            Integer port,
            String username,
            String privateKeyPath,
            String publicKeyPath,
            byte[] passphrase) {
        return new SftpFileTransferClient(host, port, username, null, privateKeyPath, publicKeyPath, null);
    }

    public static SftpFileTransferClient withPassphrase(String host,
            Integer port,
            String username,
            String privateKeyPath,
            String publicKeyPath,
            byte[] passphrase) {
        return new SftpFileTransferClient(host, port, username, null, privateKeyPath, publicKeyPath, passphrase);
    }

    public static SftpFileTransferClient withPassword(String host, Integer port, String username, String password) {
        return new SftpFileTransferClient(host, port, username, password, null, null, null);
    }

    @Override
    public String upload(String source, String dest) throws FileTransferException {
        String locationLabel = getLocationLabel(dest);
        log.debug(String.format("Uploading local file %s to %s.", source, locationLabel));

        SftpConnector con = connect();

        try (InputStream is = new FileInputStream(source)) {
            ChannelSftp chan = con.getChannel();
            chan.put(is, dest);
            log.info("File transferred successfully to host.");
            return null;
        } catch (SftpException e) {
            String message = String.format("Failed to access path \"%s\".", dest);
            log.warn(message);
            throw new FileTransferException(message, e);
        } catch (java.io.FileNotFoundException e) {
            String message = String.format("Failed to read file at \"%s\".", source);
            log.warn(message);
            throw new FileTransferException(message, e);
        } catch (IOException e) {
            String message = String.format("Failed to read file at \"%s\".", source);
            log.warn(message);
            throw new FileTransferException(message, e);
        } finally {
            disconnect(con);
        }
    }

    @Override
    public Optional<InputStream> optionalStream(String path) throws FileTransferException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public InputStream stream(String path) throws FileTransferException, MissingFileException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Boolean dirExists(String path) throws FileTransferException {
        String locationLabel = getLocationLabel(path);
        log.debug(String.format("Checking if directory %s exists.",
                                locationLabel));

        SftpConnector con = connect();
        ChannelSftp chan = con.getChannel();

        try {
            SftpATTRS attrs = chan.lstat(path);

            if (attrs.isDir()) {
                return true;
            }

            String message = String.format("%s exists, but is not a directory.",
                                           locationLabel);
            throw new FileTransferException(message);
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            } else {
                String message = String.format("Error asserting if directory %s exists.", locationLabel);
                throw new FileTransferException(message);
            }
        } finally {
            disconnect(con);
        }
    }

    @Override
    public Boolean fileExists(String path) throws FileTransferException {
        String locationLabel = getLocationLabel(path);
        log.debug(String.format("Checking if directory %s exists.",
                                locationLabel));

        SftpConnector con = connect();
        ChannelSftp chan = con.getChannel();

        try {
            SftpATTRS attrs = chan.lstat(path);

            if (attrs.isDir()) {
                String message = String.format("%s exists, but is a directory.",
                                               locationLabel);
                throw new FileTransferException(message);
            }

            return true;
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            } else {
                String message = String.format("Error asserting if file %s exists.", locationLabel);
                throw new FileTransferException(message);
            }
        } finally {
            disconnect(con);
        }
    }

    @Override
    public Optional<String> optionalSlurp(String path) throws FileTransferException, MissingFileException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public String slurp(String path) throws FileTransferException, MissingFileException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public List<FileInfo> list(String path) throws FileTransferException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Boolean optionalDownload(String path, String localPath) throws FileTransferException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Optional<OutputStream> optionalDownload(String path, OutputStream stream) throws FileTransferException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public void download(String path, String localPath) throws FileTransferException {
        SftpConnector con = connect();
        ChannelSftp chan = con.getChannel();

        try {
            chan.get(path, localPath);
        } catch (SftpException e) {
            String message = String.format("Failed to retrieve file at path \"%s\".", path);
            log.warn(message);
            throw new FileTransferException(message, e);
        } finally {
            disconnect(con);
        }
    }

    @Override
    public OutputStream download(String path, OutputStream stream) throws FileTransferException, MissingFileException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Map<String, Boolean> downloadAll(Map<String, OutputStream> targets) throws FileTransferException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public String upload(InputStream is, String path) throws FileTransferException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    private SftpConnector connect() {
        try {
            JSch.setLogger(new SimpleJschLogger());
            JSch jsch = new JSch();


            if (nonNull(privateKeyPath)) {
                log.debug("Using public key authentication.");
                log.debug("Private key path: " + privateKeyPath);
                log.debug("Public key path: " + publicKeyPath);
                if (isNull(passphrase)) {
                    log.debug("No passphrase given.");
                    jsch.addIdentity(privateKeyPath, publicKeyPath);
                } else {
                    log.debug("Using given passphrase.");
                    jsch.addIdentity(privateKeyPath, publicKeyPath, passphrase);
                }
            }

            Session session = jsch.getSession(this.username, this.host, this.port);
            if(this.password != null) {
                log.debug("Using password.");
                session.setPassword(this.password);
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            return new SftpConnector(session, channelSftp);
        } catch (JSchException e) {
            String message = "Jsch failed to set up connection.";
            log.warn(message);
            throw new FileTransferException(message, e);
        }
    }

    private void disconnect(SftpConnector conn) {
        conn.getSession().disconnect();
        conn.getChannel().disconnect();
    }

    private String getLocationLabel(String path) {
        return String.format("%s:%s", getHostLabel(), path);
    }

    private String getHostLabel() {
        if (port == 21) {
            return host;
        }
        return String.format("%s:%d", host, port);
    }
}
