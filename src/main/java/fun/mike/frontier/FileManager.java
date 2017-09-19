package fun.mike.frontier;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class FileManager {
    private static final Logger log = LoggerFactory.getLogger(FileManager.class);

    private final String host;
    private final Integer port;
    private final String username;
    private final String password;

    public FileManager(String host,
                         String username,
                         String password) {
        this(host, 21, username, password);
    }

    public FileManager(String host,
                         Integer port,
                         String username,
                         String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public Optional<InputStream> stream(String path) {
        FTPClient client = null;
        try {
            client = connect();
            return stream(client, path);
        } finally {
            disconnect(client);
        }
    }

    public Optional<InputStream> stream(FTPClient client, String path) {
        try {
            InputStream is = client.retrieveFileStream(path);
            if (is == null) {
                if (client.getReplyCode() == 550) {
                    return Optional.empty();
                }
                throw new FileManagerException(client.getReplyString());
            }
            return Optional.of(is);
        } catch (IOException ex) {
            throw new FileManagerException(ex);
        }
    }

    public Boolean dirExists(String dirPath) {
        FTPClient client = null;
        try {
            client = connect();
            return dirExists(client, dirPath);
        } finally {
            disconnect(client);
        }
    }

    public Boolean dirExists(FTPClient client, String dirPath) {
        try {
            client.changeWorkingDirectory(dirPath);
            if (client.getReplyCode() == 550) {
                return false;
            }
            return true;
        } catch (IOException ex) {
            throw new FileManagerException(ex);
        }
    }

    public String slurp(String path) {
        return IO.slurp(stream(path).orElseThrow(() -> new FileNotFoundException(path)));
    }

    public String slurp(FTPClient client, String path) {
        return IO.slurp(stream(client, path).orElseThrow(() -> new FileNotFoundException(path)));
    }

    public List<FileInfo> list(String path) {
        FTPClient client = null;
        try {
            client = connect();
            return list(client, path);
        } finally {
            disconnect(client);
        }
    }

    public List<FileInfo> list(FTPClient client, String path) {
        try {
            List<FileInfo> files = Arrays.stream(client.listFiles(path))
                .map(file -> {
                        Calendar timestamp = file.getTimestamp();
                        LocalDateTime time = LocalDateTime.ofInstant(timestamp.toInstant(),
                                                                     ZoneId.systemDefault());
                        return new FileInfo(file.getName(),
                                            file.getSize(),
                                time);
                    })
                    .collect(Collectors.toList());
            return files;
        } catch (IOException ex) {
            throw new FileManagerException(ex);
        }
    }

    public Boolean download(String path, String localPath) {
        try (OutputStream os = new FileOutputStream(localPath)) {
            return download(path, os).isPresent();

        } catch (IOException ex) {
            throw new FileManagerException(ex);
        }
    }

    public Optional<OutputStream> download(String path, OutputStream stream) {
        FTPClient client = null;
        try {
            client = connect();
            return download(client, path, stream);
        } finally {
            disconnect(client);
        }
    }

    public Optional<OutputStream> download(FTPClient client, String path, OutputStream stream) {
        return retrieveFile(client, path, stream)
                    .map(replyString -> stream);
    }

    public Map<String, Boolean> downloadAll(Map<String, OutputStream> targets) {
        Map<String, Boolean> results = new HashMap<>();
        FTPClient client = null;
        try {
            client = connect();
            return downloadAll(client, targets);
        } finally {
            disconnect(client);
        }
    }

    public Map<String, Boolean> downloadAll(FTPClient client, Map<String, OutputStream> targets) {
        Map<String, Boolean> results = new HashMap<>();

        for (Map.Entry<String, OutputStream> target : targets.entrySet()) {
            String path = target.getKey();
            OutputStream stream = target.getValue();
            Boolean successful = retrieveFile(client, path, stream)
                .map(replyString -> true)
                .orElse(false);
            results.put(path, successful);
        }
        return results;
    }

    public Optional<String> upload(String path, InputStream is) {
        FTPClient client = null;
        try {
            client = connect();
            return upload(client, path, is);
        } finally {
            disconnect(client);
        }
    }

    public Optional<String> upload(FTPClient client, String path, InputStream is) {
        try {
            boolean successful = client.storeFile(path, is);
            if(successful) {
                return Optional.of(path);
            }

            if (client.getReplyCode() == 550) {
                return Optional.empty();
            }

            throw new FileManagerException(client.getReplyString());
        }
        catch(IOException ex) {
            throw new FileManagerException(ex);
        }
    }

    private Optional<String> retrieveFile(FTPClient client,
                                          String path,
                                          OutputStream stream) {
        boolean successful;
        try {
            successful = client.retrieveFile(path, stream);
        } catch (IOException ex) {
            throw new FileManagerException(ex);
        }
        if (!successful) {
            if (client.getReplyCode() == 550) {
                return Optional.empty();
            }

            throw new FileManagerException(client.getReplyString());
        }
        return Optional.of(client.getReplyString());
    }

    public void disconnect(FTPClient client) {
        if(client == null) {
            return;
        }

        try {
            client.logout();
        } catch (IOException ex) {
        }

        if (client.isConnected()) {
            try {
                client.disconnect();
            } catch (IOException ex) {
                throw new FileManagerException(ex);
            }
        }
    }

    public FTPClient connect() {
        try {
            FTPClient client = new FTPClient();
            client.connect(host, port);
            client.login(username, password);
            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                client.disconnect();
                throw new FileManagerException("FTP server refused connection.");
            }
            return client;
        } catch (IOException ex) {
            throw new FileManagerException(ex);
        }
    }
}
