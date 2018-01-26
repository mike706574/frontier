package fun.mike.frontier.alpha;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import fun.mike.frontier.impl.alpha.JschSftp;
import fun.mike.frontier.impl.alpha.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.isNull;

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
    public String upload(String source, String dest) {
        String locationLabel = getLocationLabel(dest);
        log.debug(String.format("Uploading local file %s to %s.", source, locationLabel));

        if(!IO.exists(source)) {
            String message = String.format("Failed to read local source file \"%s\".", source);
            throw new MissingLocalFileException(message);
        }

        try (InputStream is = new FileInputStream(source)) {
            return upload(is, dest);
        } catch (IOException e) {
            String message = String.format("Failed to read local source file \"%s\".", source);
            log.warn(message);
            throw new FileTransferException(message, e);
        }
    }

    @Override
    public Optional<InputStream> optionalStream(String path) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public InputStream stream(String path) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Boolean dirExists(String path) {
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
    public Boolean fileExists(String path) {
        return withConnector(conn -> JschSftp.fileExists(conn, path));
    }

    @Override
    public Optional<String> optionalSlurp(String path) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public String slurp(String path) {
        SftpConnector con = connect();
        ChannelSftp chan = con.getChannel();

        try {
            return IO.slurp(chan.get(path));
        } catch (SftpException e) {
            String message = String.format("Failed to retrieve file at path \"%s\".", path);
            log.warn(message);
            throw new FileTransferException(message, e);
        } finally {
            disconnect(con);
        }
    }

    @Override
    public List<FileInfo> list(String path) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Boolean optionalDownload(String path, String localPath){
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
    public OutputStream download(String path, OutputStream stream) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Map<String, Boolean> downloadAll(Map<String, OutputStream> targets) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public String upload(InputStream is, String path) {
        SftpConnector conn = connect();

        try {
            ChannelSftp chan = conn.getChannel();
            chan.put(is, path);
            log.info("File successfully transferred to host.");
            return path;
        } catch (SftpException e) {
            String message = String.format("Failed to access path \"%s\".", path);
            log.warn(message);
            throw new FileTransferException(message, e);
        } finally {
            disconnect(conn);
        }
    }

    @Override
    public void delete(String path) {
        SftpConnector conn = connect();
        ChannelSftp chan = conn.getChannel();

        try {
            if (!fileExists(path)) {
                throw remoteFileNotFound(path);
            }

            chan.rm(path);
        } catch (SftpException e) {
            String message = String.format("Failed to retrieve file at path \"%s\".", path);
            log.warn(message);
            throw new FileTransferException(message, e);
        } finally {
            disconnect(conn);
        }
    }

    private SftpConnector connect() {
        try {
            JSch.setLogger(new SimpleJschLogger());
            JSch jsch = new JSch();

            if (privateKeyPath != null) {
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
            if (this.password != null) {
                log.debug("Using password.");
                session.setPassword(this.password);
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            return new SftpConnector(session, channelSftp, host, port);
        } catch (JSchException e) {
            String message = "Jsch failed to set up connection.";
            log.warn(message);
            throw new FileTransferException(message, e);
        }
    }

    private void disconnect(SftpConnector conn) {
        if(conn != null) {
            conn.getSession().disconnect();
            conn.getChannel().disconnect();
        }
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

    private MissingRemoteFileException remoteFileNotFound(String path) {
        String locationLabel = getLocationLabel(path);
        String message = String.format("File %s not found.", path);
        return new MissingRemoteFileException(message);
    }

    private <T> T withConnector(Function<SftpConnector, T> function) {
        SftpConnector conn = null;
        try {
            conn = connect();
            return function.apply(conn);
        } finally {
            disconnect(conn);
        }
    }

    private void useConnector(Consumer<SftpConnector> consumer) {
        SftpConnector conn = null;
        try {
            conn = connect();
            consumer.accept(conn);
        } finally {
            disconnect(conn);
        }
    }
}
