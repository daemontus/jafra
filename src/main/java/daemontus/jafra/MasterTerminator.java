package daemontus.jafra;

import org.jetbrains.annotations.NotNull;

/**
 * Terminator running on a master machine.
 * Implements Safra's algorithm(http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) for termination detection.
 */
class MasterTerminator extends Terminator {

    private boolean waitingForToken = false;

    MasterTerminator(@NotNull TokenMessenger m) {
        super(m);
        if (!messenger.isMaster()) {
            throw new IllegalStateException("Cannot launch master terminator on slave machine.");
        }
    }

    private void initProbe() {
        //status = White
        flag = 0;
        waitingForToken = true;
        messenger.sendTokenAsync(new Token(0,0));
    }

    @Override
    public synchronized void setDone() {
        super.setDone();
        if (!waitingForToken) { //at this point, working is false
            //if node is idle and no token is already in the system
            //Note: It is ok to send a probe even when main task has not finished yet, because
            //probe will return only after all main tasks are finished and if any messages
            //are sent/received during this period, flag/count will reflect this and process won't terminate
            initProbe();
        }
    }

    @Override
    public void terminationLoop() {
        //we don't try to send a probe here, because setDone
        //has to be called at least once, therefore initiating probe

        //wait for token to go through all other processes.
        @NotNull Token token = messenger.waitForToken();
        while (token.flag < 2) {    //while this is not a termination token
            synchronized (this) {
                if (!waitingForToken) { //if we are not expecting a token, something is really wrong
                    throw new IllegalStateException("Master received a token he was not waiting for!");
                }
                waitingForToken = false;
                if (flag == 0 && token.flag == 0 && token.count + count == 0) {
                    //if termination criteria are met, finish this whole thing
                    //Slaves should pass this and return it to us (that will terminate this while loop)
                    messenger.sendTokenAsync(new Token(2, 0));
                } else {
                    if (!working) {
                        //if node is idle, just go for another round
                        initProbe();
                    } //if we are working, just wait - setDone will init the probe
                }
            }
            //wait for another token
            token = messenger.waitForToken();
        }
    }
}
