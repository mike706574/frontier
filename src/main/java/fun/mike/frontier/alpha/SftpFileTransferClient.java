package fun.mike.frontier.alpha;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import fun.mike.frontier.impl.alpha.JschSftp;
import fun.mike.frontier.impl.alpha.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class SftpFileTransferClient implements FileTransferClient {
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    private static final int DEFAULT_SERVER_KEEP_ALIVE_COUNT_MAX = 6;
    private static final int DEFAULT_SERVER_KEEP_ALIVE_INTERVAL = 10000;

    private static final Logger log = LoggerFactory.getLogger(FtpFileTransferClient.class);

    private final String host;
    private final Integer port;
    private final String username;
    private final String password;
    private final String privateKeyPath;
    private final String publicKeyPath;
    private final String knownHostsPath;
    private final byte[] passphrase;
    private final boolean strictHostChecking;
    private final int connectTimeout;
    private final int serverKeepAliveCountMax;
    private final int serverKeepAliveInterval;

    public SftpFileTransferClient(String host,
            Integer port,
            String username,
            String password,
            String privateKeyPath,
            String publicKeyPath,
            String knownHostsPath,
            byte[] passphrase) {
        this(host, port, username, password, privateKeyPath, publicKeyPath,
             knownHostsPath, passphrase, true);
    }

    public SftpFileTransferClient(String host,
                                  Integer port,
                                  String username,
                                  String password,
                                  String privateKeyPath,
                                  String publicKeyPath,
                                  String knownHostsPath,
                                  byte[] passphrase,
                                  boolean strictHostChecking) {
        this(host, port, username, password, privateKeyPath, publicKeyPath,
             knownHostsPath, passphrase, strictHostChecking, DEFAULT_CONNECT_TIMEOUT,
             DEFAULT_SERVER_KEEP_ALIVE_COUNT_MAX, DEFAULT_SERVER_KEEP_ALIVE_INTERVAL);
    }

    public SftpFileTransferClient(String host,
                                  Integer port,
                                  String username,
                                  String password,
                                  String privateKeyPath,
                                  String publicKeyPath,
                                  String knownHostsPath,
                                  byte[] passphrase,
                                  boolean strictHostChecking,
                                  int connectTimeout,
                                  int serverKeepAliveCountMax,
                                  int serverKeepAliveInterval) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKeyPath = privateKeyPath;
        this.publicKeyPath = publicKeyPath;
        this.knownHostsPath = knownHostsPath;
        this.passphrase = passphrase;
        this.strictHostChecking = strictHostChecking;
        this.connectTimeout = connectTimeout;
        this.serverKeepAliveCountMax = serverKeepAliveCountMax;
        this.serverKeepAliveInterval = serverKeepAliveInterval;
    }

    public static SftpFileTransferClient withKeys(String host,
            Integer port,
            String username,
            String privateKeyPath,
            String publicKeyPath,
            String knownHostsPath,
            byte[] passphrase) {
        return new SftpFileTransferClient(host, port, username, null, privateKeyPath, publicKeyPath, knownHostsPath, passphrase);
    }

    public static SftpFileTransferClient withKeys(String host,
            Integer port,
            String username,
            String privateKeyPath,
            String publicKeyPath,
            String knownHostsPath,
            byte[] passphrase,
            boolean strictHostChecking) {
        return new SftpFileTransferClient(host, port, username, null, privateKeyPath, publicKeyPath, knownHostsPath, passphrase, strictHostChecking);
    }

    public static SftpFileTransferClient withKeys(String host,
                                                  Integer port,
                                                  String username,
                                                  String privateKeyPath,
                                                  String publicKeyPath,
                                                  String knownHostsPath,
                                                  byte[] passphrase,
                                                  boolean strictHostChecking,
                                                  int connectTimeout,
                                                  int serverKeepAliveCountMax,
                                                  int serverKeepAliveInterval) {
        return new SftpFileTransferClient(host, port, username, null, privateKeyPath, publicKeyPath, knownHostsPath, passphrase, strictHostChecking, connectTimeout, serverKeepAliveCountMax, serverKeepAliveInterval);
    }

    public static SftpFileTransferClient withPassphrase(String host,
            Integer port,
            String username,
            String privateKeyPath,
            String publicKeyPath,
            String knownHostsPath,
            byte[] passphrase) {
        return new SftpFileTransferClient(host, port, username, null, privateKeyPath, publicKeyPath, knownHostsPath, passphrase);
    }

    public static SftpFileTransferClient withPassphrase(String host,
                                                        Integer port,
                                                        String username,
                                                        String privateKeyPath,
                                                        String publicKeyPath,
                                                        String knownHostsPath,
                                                        byte[] passphrase,
                                                        boolean strictHostChecking,
                                                        int connectTimeout,
                                                        int serverKeepAliveCountMax,
                                                        int serverKeepAliveInterval) {
        return new SftpFileTransferClient(host, port, username, null, privateKeyPath, publicKeyPath, knownHostsPath, passphrase, strictHostChecking, connectTimeout, serverKeepAliveCountMax, serverKeepAliveInterval);
    }

    public static SftpFileTransferClient withPassword(String host, Integer port, String username, String password) {
        return new SftpFileTransferClient(host, port, username, password, null, null, null, null);
    }

    public static SftpFileTransferClient withPassword(String host, Integer port, String username, String password, boolean strictHostChecking) {
        return new SftpFileTransferClient(host, port, username, password, null, null, null, null, strictHostChecking);
    }

    public static SftpFileTransferClient withPassword(String host,
                                                      Integer port,
                                                      String username,
                                                      String password,
                                                      boolean strictHostChecking,
                                                      int connectTimeout,
                                                      int serverKeepAliveCountMax,
                                                      int serverKeepAliveInterval) {
        return new SftpFileTransferClient(host, port, username, password, null, null, null, null, strictHostChecking, connectTimeout, serverKeepAliveCountMax, serverKeepAliveInterval);
    }

    @Override
    public String upload(String source, String dest) {
        String locationLabel = getLocationLabel(dest);
        log.debug(String.format("Uploading local file %s to %s.", source, locationLabel));

        if (!IO.exists(source)) {
            String message = String.format("Local source file %s does not exist.", source);
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
        SftpConnector conn = connect();
        ChannelSftp chan = conn.getChannel();

        try {
            Vector<LsEntry> resultVector = (Vector<LsEntry>) chan.ls(path);
            return resultVector.stream()
                    .map(entry -> {
                        Date fileDate = new Date(entry.getAttrs().getATime() * 1000L);
                        return new FileInfo(entry.getFilename(),
                                            entry.getAttrs().getSize(),
                                            fileDate,
                                            entry.getAttrs().isDir());
                    })
                    .collect(Collectors.toList());
        } catch (SftpException e) {
            String message = String.format("Failed to list files at \"%s\".", path);
            log.warn(message);
            throw new FileTransferException(message, e);
        } finally {
            disconnect(conn);
        }
    }

    @Override
    public Boolean optionalDownload(String path, String localPath) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Optional<OutputStream> optionalDownload(String path, OutputStream stream) throws FileTransferException {
        return withConnector(conn -> {
                try {
                    conn.getChannel().get(path, stream);
                    return Optional.of(stream);
                }
                catch (SftpException e) {
                    if(e.id == 2) {
                        return Optional.empty();
                    }

                    String message = String.format("Failed to retrieve file at path \"%s\".", path);
                    log.warn(message);
                    throw new FileTransferException(message, e);
                }
            });
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
        useConnector(conn -> {
                try {
                    conn.getChannel().get(path, stream);
                }
                catch (SftpException e) {
                    String message = String.format("Failed to retrieve file at path \"%s\".", path);
                    log.warn(message);
                    throw new FileTransferException(message, e);
                }
            });
        return stream;
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
                log.trace("Using public key authentication.");
                log.trace("Private key path: " + privateKeyPath);
                log.trace("Public key path: " + publicKeyPath);
                if (isNull(passphrase)) {
                    log.trace("No passphrase given.");
                    jsch.addIdentity(privateKeyPath, publicKeyPath);
                } else {
                    log.trace("Using given passphrase.");
                    jsch.addIdentity(privateKeyPath, publicKeyPath, passphrase);
                }
            }

            Session session = jsch.getSession(this.username, this.host, this.port);

            session.setServerAliveCountMax(serverKeepAliveCountMax);
            session.setServerAliveInterval(serverKeepAliveInterval);

            if (this.password != null) {
                log.trace("Using password.");
                session.setPassword(this.password);
            }

            Properties config = new Properties();
            if(!strictHostChecking) {
                log.warn("Strict host key checking is set to \"false\".");
                config.put("StrictHostKeyChecking", "no");
            }

            if (this.knownHostsPath != null) {
                log.trace("Known hosts path: " + knownHostsPath);
                jsch.setKnownHosts(knownHostsPath);
            } else {
                log.warn("No known hosts file path provided.");
            }

            session.setConfig(config);
            session.connect(connectTimeout);
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            return new SftpConnector(session, channelSftp, host, port);
        } catch (JSchException e) {
            String message = "Jsch failed to set up connection.";
            log.warn(message);
            e.printStackTrace();
            throw new FileTransferException(message, e);
        }
    }

    private void disconnect(SftpConnector conn) {
        if (conn != null) {
            conn.getChannel().disconnect();
            conn.getSession().disconnect();
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
