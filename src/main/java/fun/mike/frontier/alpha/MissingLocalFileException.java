package fun.mike.frontier.alpha;

public class MissingLocalFileException extends RuntimeException{
    public MissingLocalFileException(String msg) {
        super(msg);
    }

    public MissingLocalFileException(Throwable t) {
        super(t);
    }

    public MissingLocalFileException(String msg, Throwable t) {
        super(msg, t);
    }
}
