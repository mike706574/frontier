package fun.mike.frontier.impl.alpha;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

public class SftpConnector {
    private final Session session;
    private final ChannelSftp channel;

    public SftpConnector(Session session, ChannelSftp channelSftp) {
        this.session = session;
        this.channel = channelSftp;
    }

    public ChannelSftp getChannel() {
        return channel;
    }

    public Session getSession() {
        return session;
    }
}
