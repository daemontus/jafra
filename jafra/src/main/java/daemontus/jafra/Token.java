package daemontus.jafra;

/**
 * Represents one instance of a token passed during termination detection.
 */
public class Token {
    public final int count;
    public final int flag;

    /**
     * Create new token with given properties.
     */
    public Token(int flag, int count) {
        this.count = count;
        this.flag = flag;
    }
}
