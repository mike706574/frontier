package fun.mike.frontier.impl.alpha;

import org.apache.commons.net.ftp.FTPClient;

public class FtpConnector {
    private final FTPClient client;
    private final String host;
    private final Integer port;

    public FtpConnector(FTPClient client, String host, Integer port) {
        this.client = client;
        this.host = host;
        this.port = port;
    }

    public FTPClient getClient() {
        return client;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }
}
