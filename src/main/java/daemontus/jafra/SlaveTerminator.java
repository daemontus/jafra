package daemontus.jafra;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Terminator running on a slave machine.
 * Implements Safra's algorithm(http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) for termination detection.
 */
class SlaveTerminator extends Terminator {

    @Nullable
    private Token pendingToken = null;

    SlaveTerminator(@NotNull TokenMessenger m) {
        super(m);
        if (m.isMaster()) {
            throw new IllegalStateException("Cannot launch slave terminator on master machine");
        }
    }

    private void processToken(@NotNull Token token) {
        //non blocking send (so that we do not end up stuck in synchronized section)
        messenger.sendTokenAsync(new Token(Math.min(token.flag + flag, 1), token.count + count));
        //status = White
        flag = 0;
    }

    @Override
    public void terminationLoop() {
        //wait for token from master
        @NotNull Token token = messenger.waitForToken();
        while (token.flag < 2) {    //while this is not terminating token
            synchronized (SlaveTerminator.this) {
                if (working) {  //if node is active, save token for later
                    pendingToken = token;
                } else { //else pass token to next node right away
                    processToken(token);
                }
            }
            token = messenger.waitForToken();
        }
        //termination message has been received - we pass this to next node and finish ourselves
        messenger.sendTokenAsync(token);
    }

    @Override
    public synchronized void setDone() {
        super.setDone();
        if (pendingToken != null) { //working is false here
            //node is idle and has unprocessed tokens
            processToken(pendingToken);
            pendingToken = null;
        }
    }

}
