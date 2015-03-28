package daemontus.jafra;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract implementation of token messenger based on ID ordering.
 *
 * Master process is marked with ID 0.
 * Other processes are marked with consecutive integers (1,2,3,4...).
 *
 * This is suitable for usage with various MPI-like communication systems.
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class IdTokenMessenger implements TokenMessenger {

    private final int myId;
    private final int processCount;

    private final int successor;
    private final int predecessor;

    public IdTokenMessenger(int myId, int processCount) {
        if (processCount < 1) {
            throw new IllegalArgumentException("Process count must be at least 1");
        }
        if (myId < 0) {
            throw new IllegalArgumentException("Process ID must be non negative.");
        }
        this.myId = myId;
        this.processCount = processCount;
        successor = (myId + 1) % processCount;
        if (myId - 1 < 0) {
            predecessor = processCount - 1;
        } else {
            predecessor = myId - 1;
        }
    }

    /**
     * @return Id of successor process.
     */
    public int getSuccessor() {
        return successor;
    }

    /**
     * @return Id of predecessor process.
     */
    public int getPredecessor() {
        return predecessor;
    }

    /**
     * @return Total number of processes participating in termination detection.
     */
    int getProcessCount() {
        return processCount;
    }

    /**
     * @return Id of process using this messenger.
     */
    int getMyId() {
        return myId;
    }

    @Override
    public void sendTokenAsync(Token token) {
        sendTokenAsync(successor, token);
    }

    @NotNull
    @Override
    public Token waitForToken() {
        return waitForToken(predecessor);
    }

    @Override
    public boolean isMaster() {
        return myId == 0;
    }

    /**
     * Send a token to a process asynchronously.
     * @param destination Id of the destination process.
     * @param token Token that should be sent to given destination.
     */
    protected abstract void sendTokenAsync(int destination, @NotNull Token token);

    /**
     * Block until a token is received from a source.
     * @param source Id of the source process.
     * @return Token received from the process.
     */
    @NotNull protected abstract Token waitForToken(int source);

}
