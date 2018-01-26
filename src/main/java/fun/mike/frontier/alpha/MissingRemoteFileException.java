package fun.mike.frontier.alpha;

public class MissingRemoteFileException extends RuntimeException {
    public MissingRemoteFileException(String msg) {
        super(msg);
    }

    public MissingRemoteFileException(Throwable t) {
        super(t);
    }

    public MissingRemoteFileException(String msg, Throwable t) {
        super(msg, t);
    }
}
