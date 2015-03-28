package daemontus.jafra;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a communication device used by termination detection algorithm.
 *
 * It requires a total ordering on participating processes,
 * so that predecessor/successor can be determined.
 * Also, communication should preserve order and be reliable.
 * (There is zero message loss tolerance)
 */
public interface TokenMessenger {

    /**
     * Send token to closest higher process according to the process ordering.
     * If this is the highest process in the ordering, send token to
     * the lowest process.
     * @param token Token to be sent to successor.
     */
    void sendTokenAsync(Token token);

    /**
     * Block until token is received from closest lower process.
     * If this is the lowest process in the ordering, receive token
     * from the highest one.
     * @return Token received from predecessor.
     */
    @NotNull Token waitForToken();

    /**
     * Only one process should declare itself master.
     * @return True if this process should initiate termination detection.
     */
    boolean isMaster();

}
