package com.daemontus.jafra;

import org.jetbrains.annotations.NotNull;

/**
 * <p>Manages termination detection in current process using given messenger.</p>
 *
 * <p>Terminators are created either by static method or factory. This ensures
 * that appropriate terminator implementations are used.</p>
 *
 * <p>All terminators are initially marked as working. So even if no
 * messages are sent/received and only local work has been performed,
 * you should call setDone at least once.</p>
 *
 * <p>Terminators are not reusable. Once {@link #waitForTermination() waitForTermination} returns,
 * terminator is marked as finished and should not be used again.</p>
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class Terminator {

    protected int flag = 0;
    protected int count = 0;

    protected boolean finalized = false;
    protected boolean working = true;

    @NotNull
    protected final TokenMessenger messenger;

    /**
     * Creates new terminators based on given token passing interface.
     *
     * (Great for multi-round algorithms, where each round uses
     * same messenger, but new terminator.)
     */
    public static class TerminatorFactory {

        @NotNull
        private final TokenMessenger messenger;

        public TerminatorFactory(@NotNull TokenMessenger messenger) {
            this.messenger = messenger;
        }

        @NotNull
        public Terminator createNew() {
            return Terminator.createNew(messenger);
        }
    }

    /**
     * Create new terminator according to given messenger.
     * @param messenger Messenger that should be used for communication.
     * @return Usable terminator instance.
     */
    public static Terminator createNew(@NotNull TokenMessenger messenger) {
        if (messenger.isMaster()) {
            return new MasterTerminator(messenger);
        } else {
            return new SlaveTerminator(messenger);
        }
    }

    /** Package private constructor - new terminators should be created using static factory. */
    Terminator(@NotNull TokenMessenger messenger) {
        this.messenger = messenger;
    }

    /**
     * Tell terminator that local work is done and he can resume operations. (i.e. after message has been received and processed)
     */
    public synchronized void setDone() {
        if (finalized) throw new IllegalStateException("Called setDone on finalized master terminator");
        this.working = false;
    }

    /**
     * Indicate that message has been sent from this process.
     */
    public synchronized void messageSent() throws IllegalStateException {
        if (finalized) throw new IllegalStateException("Called messageSent on finalized master terminator");
        if (!working) throw new IllegalStateException("Terminator that is not working is sending messages. This is suspicious.");
        count++;
    }

    /**
     * Indicate that message has been received and this process is currently processing it.
     * WARNING: In order for terminator to properly finish, any sequence of
     * messageReceived calls must end with at least one setDone call.
     */
    public synchronized void messageReceived() {
        if (finalized) throw new IllegalStateException("Called messageReceived on finalized master terminator");
        count--;
        //status = Black
        flag = 1;
        working = true;
    }

    /**
     * Start termination detection process and wait for successful termination.
     * Terminator can receive messages even before this call and will preserve them as internal state,
     * but only after this call will it start to exchange info with other processes.
     */
    public void waitForTermination() {
        if (finalized) throw new IllegalStateException("Called waitForTermination on finalized master terminator");
        terminationLoop();
        finalized = true;
    }

    /**
     * This method actually detects the termination by exchanging info with other processes until fix point is reached.
     */
    protected abstract void terminationLoop();

}
