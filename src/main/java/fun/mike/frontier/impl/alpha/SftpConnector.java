package fun.mike.frontier.impl.alpha;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

public class SftpConnector {
    private final Session session;
    private final ChannelSftp channel;
    private final String host;
    private final Integer port;

    public SftpConnector(Session session, ChannelSftp channelSftp, String host, Integer port) {
        this.session = session;
        this.channel = channelSftp;
        this.host = host;
        this.port = port;
    }

    public ChannelSftp getChannel() {
        return channel;
    }

    public Session getSession() {
        return session;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }
}
