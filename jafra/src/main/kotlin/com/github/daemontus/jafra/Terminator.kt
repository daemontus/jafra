package com.github.daemontus.jafra


/**
 *
 * Manages termination detection in current process using given messenger.
 *
 *
 * Terminators are created either by static method or factory. This ensures
 * that appropriate terminator implementations are used.
 *
 *
 * All terminators are initially marked as working. So even if no
 * messages are sent/received and only local work has been performed,
 * you should call setDone at least once. (This prevents "immediate" termination after creation)
 *
 *
 * Terminators are not reusable. Once [waitForTermination][.waitForTermination] returns,
 * terminator is marked as finished and should not be used again.
 */
abstract class Terminator
/** package private constructor - new terminators should be created using static factory.  */
constructor(protected val messenger: TokenMessenger) {

    protected var flag: Int = 0
    protected var count: Int = 0

    protected var finalized: Boolean = false
    var working: Boolean = true
        private set

    /**
     * Tell terminator that local work is done and he can resume operations. (i.e. after message has been received and processed)
     */
    @Synchronized open fun setDone() {
        throwIfFinalized()
        throwIfNotWorking()
        working = false
    }

    /**
     * Indicate that message has been sent from this process.
     */
    @Synchronized fun messageSent() {
        throwIfFinalized()
        throwIfNotWorking()
        count++
    }

    /**
     * Indicate that message has been received and this process is currently processing it.
     * WARNING: In order for terminator to properly finish, any sequence of
     * messageReceived calls must end with at least one setDone call.
     */
    @Synchronized fun messageReceived() {
        throwIfFinalized()
        count--
        //status = Black
        flag = 1
        working = true
    }

    /**
     * Start termination detection process and wait for successful termination.
     * Terminator can receive messages even before this call and will preserve them as internal state,
     * but only after this call will it start to exchange info with other processes.
     */
    fun waitForTermination() {
        throwIfFinalized()
        terminationLoop()
        finalized = true
    }

    /**
     * This method actually detects the termination by exchanging info with other processes until fix point is reached.
     */
    protected abstract fun terminationLoop()

    private fun throwIfFinalized(): Unit = if (finalized) throw IllegalStateException("This terminator has been already finalized.") else {}
    private fun throwIfNotWorking(): Unit = if (!working) throw IllegalStateException("This terminator is not working.") else {}

    companion object {

        /**
         * Create new terminator according to given messenger.
         * @param messenger Messenger that should be used for communication.
         * *
         * @return Usable terminator instance.
         */
        fun createNew(messenger: TokenMessenger): Terminator {
            if (messenger.isMaster()) {
                return MasterTerminator(messenger)
            } else {
                return SlaveTerminator(messenger)
            }
        }
    }

    /**
     * Creates new terminators based on given token passing interface.

     * (Great for multi-round algorithms, where each round uses
     * same messenger, but new terminator.)
     */
    class Factory(private val messenger: TokenMessenger) {

        fun createNew(): Terminator {
            return Terminator.createNew(messenger)
        }
    }

}

/**
 * Terminator running on a slave machine.
 * Implements Safra's algorithm(http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) for termination detection.
 */
private class SlaveTerminator(m: TokenMessenger) : Terminator(m) {

    private var pendingToken: Token? = null

    @Synchronized
    private fun processToken(token: Token?) {
        //working is false here
        if (token != null) { //node is idle and has unprocessed tokens
            //non blocking send (so that we do not end up stuck in synchronized section)
            messenger.sendToNextAsync(Token(Math.min(token.flag + flag, 1), token.count + count))
            //status = White
            flag = 0
        }
    }

    override fun terminationLoop() {
        //wait for token from master
        var token = messenger.receiveFromPrevious()
        while (token.flag < 2) {   //while this is not a terminating token
            synchronized (this@SlaveTerminator) {
                if (working) {
                    //if node is active, save token for later
                    pendingToken = token
                } else {
                    //else pass token to next node right away
                    processToken(token)
                }
            }
            token = messenger.receiveFromPrevious()
        }
        //termination message has been received - we pass this to next node and finish ourselves
        messenger.sendToNextAsync(token)
    }

    @Synchronized
    override fun setDone() {
        super.setDone()
        processToken(pendingToken)
        pendingToken = null
    }

}

/**
 * Terminator running on a master machine.
 * Implements Safra's algorithm(http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) for termination detection.
 */
private class MasterTerminator(m: TokenMessenger) : Terminator(m) {

    private var waitingForToken = false

    private fun initProbe() {
        //status = White
        flag = 0
        waitingForToken = true
        messenger.sendToNextAsync(Token(0, 0))
    }

    override fun setDone() {
        super.setDone()
        if (!waitingForToken) {
            //at this point, working is false
            //if node is idle and no token is already in the system
            //Note: It is ok to send a probe even when main task has not finished yet, because
            //probe will return only after all main tasks are finished and if any messages
            //are sent/received during this period, flag/count will reflect this and process won't terminate
            initProbe()
        }
    }

    var last = Int.MIN_VALUE

    override fun terminationLoop() {
        //we don't try to send a probe here, because setDone
        //has to be called at least once, therefore initiating probe

        //wait for token to go through all other processes.
        var token = messenger.receiveFromPrevious()
        while (token.flag < 2) {  //while this is not a termination token
            synchronized (this) {
                if (!waitingForToken) {
                    //if we are not expecting a token, something is really wrong
                    throw IllegalStateException("Master received a token he was not waiting for!")
                }
                waitingForToken = false
                if (flag == 0 && token.flag == 0 && token.count + count == 0) {
                    //if termination criteria are met, finish this whole thing
                    //Slaves should pass this and return it to us (that will terminate this while loop)
                    messenger.sendToNextAsync(Token(2, 0))
                } else {
                    if (!working) {
                        //if node is idle, just go for another round
                        initProbe()
                    } //if we are working, just wait - setDone will init the probe
                }
                if (last == token.count) {
                  //  throw IllegalStateException("aaaa")
                }
                last = token.count
            }
            //wait for another token
            token = messenger.receiveFromPrevious()
        }
    }
}
