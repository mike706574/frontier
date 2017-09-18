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

public class FileRetriever {
    private static final Logger log = LoggerFactory.getLogger(FileRetriever.class);

    private final String host;
    private final Integer port;
    private final String username;
    private final String password;

    public FileRetriever(String host,
                         String username,
                         String password) {
        this(host, 21, username, password);
    }

    public FileRetriever(String host,
                         Integer port,
                         String username,
                         String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public Optional<InputStream> stream(String path) {
        FTPClient client = new FTPClient();
        try {
            connect(client);
            InputStream is = client.retrieveFileStream(path);
            if (is == null) {
                if (client.getReplyCode() == 550) {
                    return Optional.empty();
                }
                throw new FileRetrieverException(client.getReplyString());
            }
            return Optional.of(is);
        } catch (IOException ex) {
            throw new FileRetrieverException(ex);
        } finally {
            disconnect(client);
        }
    }

    public String slurp(String path) {
        return IO.slurp(stream(path).orElseThrow(() -> new FileNotFoundException(path)));
    }

    public List<FileInfo> list(String path) {
        FTPClient client = new FTPClient();
        try {
            connect(client);
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
            throw new FileRetrieverException(ex);
        } finally {
            disconnect(client);
        }
    }

    public Boolean download(String path, String localPath) {
        try (OutputStream os = new FileOutputStream(localPath)) {
            return download(path, os).isPresent();

        } catch (IOException ex) {
            throw new FileRetrieverException(ex);
        }
    }

    public Optional<OutputStream> download(String path, OutputStream stream) {
        FTPClient client = new FTPClient();
        try {
            connect(client);
            return retrieveFile(client, path, stream)
                    .map(replyString -> stream);
        } finally {
            disconnect(client);
        }
    }

    public Map<String, Boolean> downloadAll(Map<String, OutputStream> targets) {
        Map<String, Boolean> results = new HashMap<>();
        FTPClient client = new FTPClient();
        try {
            connect(client);
            for (Map.Entry<String, OutputStream> target : targets.entrySet()) {
                String path = target.getKey();
                OutputStream stream = target.getValue();
                Boolean successful = retrieveFile(client, path, stream)
                        .map(replyString -> true)
                        .orElse(false);
                results.put(path, successful);
            }
            return results;
        } finally {
            disconnect(client);
        }
    }

    private Optional<String> retrieveFile(FTPClient client,
                                          String path,
                                          OutputStream stream) {
        boolean successful;
        try {
            successful = client.retrieveFile(path, stream);
        } catch (IOException ex) {
            throw new FileRetrieverException(ex);
        }
        if (!successful) {
            if (client.getReplyCode() == 550) {
                return Optional.empty();
            }

            throw new FileRetrieverException(client.getReplyString());
        }
        return Optional.of(client.getReplyString());
    }

    private void disconnect(FTPClient client) {
        try {
            client.logout();
        } catch (IOException ex) {
        }

        if (client.isConnected()) {
            try {
                client.disconnect();
            } catch (IOException ex) {
                throw new FileRetrieverException(ex);
            }
        }
    }

    private void connect(FTPClient client) {
        try {
            client.connect(host, port);
            client.login(username, password);
            System.out.println("Connected to " + host + ".");
            System.out.print(client.getReplyString());

            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                client.disconnect();
                throw new FileRetrieverException("FTP server refused connection.");
            }
        } catch (IOException ex) {
            throw new FileRetrieverException(ex);
        }
    }
}
