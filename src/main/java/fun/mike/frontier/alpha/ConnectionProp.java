package fun.mike.frontier.alpha;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

public class ConnectionProp {
    private Session session;
    private ChannelSftp channel;

    public ConnectionProp(Session session, ChannelSftp channelSftp) {
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
