package com.github.daemontus.jafra


//flag is int because we use it in the poison pill
data class Token(val flag: Int, val count: Int)

/**
 * Represents a communication device used by termination detection algorithm.

 * It requires a total ordering on participating processes,
 * so that predecessor/successor can be determined.
 * Also, communication should preserve order and be reliable.
 * (There is zero message loss tolerance)
 */
interface TokenMessenger {

    /**
     * Send token to closest higher process according to the process ordering.
     * If this is the highest process in the ordering, send token to
     * the lowest process.
     * @param token Token to be sent to successor.
     */
    fun sendToNextAsync(token: Token)

    /**
     * Block until token is received from closest lower process.
     * If this is the lowest process in the ordering, receive token
     * from the highest one.
     * @return Token received from predecessor.
     */
    fun receiveFromPrevious(): Token

    /**
     * Only one process should declare itself master.
     * @return True if this process should initiate termination detection.
     */
    fun isMaster(): Boolean

}

/**
 * Abstract implementation of token messenger based on ID ordering.
 *
 * Master process is marked with ID 0.
 * Other processes are marked with consecutive integers (1,2,3,4...).
 *
 * This is suitable for usage with various MPI-like communication systems.
 */
abstract class IdTokenMessenger(val myId: Int, val processCount: Int) : TokenMessenger {

    private val successor: Int
    private val predecessor: Int

    init {
        if (processCount < 1) {
            throw IllegalArgumentException("Process count must be at least 1")
        }
        if (myId < 0) {
            throw IllegalArgumentException("Process ID must be non negative.")
        }
        successor = (myId + 1) % processCount
        if (myId - 1 < 0) {
            predecessor = processCount - 1
        } else {
            predecessor = myId - 1
        }
    }

    override fun sendToNextAsync(token: Token) {
        sendTokenAsync(successor, token)
    }

    override fun receiveFromPrevious(): Token {
        return waitForToken(predecessor)
    }

    override fun isMaster(): Boolean {
        return myId == 0
    }

    /**
     * Send a token to a process asynchronously.
     * @param destination Id of the destination process.
     * *
     * @param token Token that should be sent to given destination.
     */
    protected abstract fun sendTokenAsync(destination: Int, token: Token)

    /**
     * Block until a token is received from a source.
     * @param source Id of the source process.
     * *
     * @return Token received from the process.
     */
    protected abstract fun waitForToken(source: Int): Token

}
