package daemontus.jafra;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TerminatorIntegrationTest {

    @NotNull
    private final List<BlockingQueue<Token>> queues = new ArrayList<>();
    private int msgCount = 0;

    private class Messenger extends IdTokenMessenger {

        public Messenger(int myId, int processCount) {
            super(myId, processCount);
        }

        @Override
        public void sendTokenAsync(int destination, @NotNull Token token) {
            queues.get(destination).add(token);
            msgCount++;
        }

        @NotNull
        @Override
        public Token waitForToken(int source) {
            try {
                return queues.get(getMyId()).take();
            } catch (InterruptedException e) {
                return new Token(0,0);
            }
        }
    }

    @Test
    public void complexTest() throws InterruptedException {
        queues.clear();
        queues.add(new LinkedBlockingQueue<Token>());
        queues.add(new LinkedBlockingQueue<Token>());
        queues.add(new LinkedBlockingQueue<Token>());
        queues.add(new LinkedBlockingQueue<Token>());
        queues.add(new LinkedBlockingQueue<Token>());

        @NotNull final MasterTerminator masterTerminator = new MasterTerminator(new Messenger(0,5));
        @NotNull final SlaveTerminator slaveTerminator1 = new SlaveTerminator(new Messenger(1,5));
        @NotNull final SlaveTerminator slaveTerminator2 = new SlaveTerminator(new Messenger(2,5));
        @NotNull final SlaveTerminator slaveTerminator3 = new SlaveTerminator(new Messenger(3,5));
        @NotNull final SlaveTerminator slaveTerminator4 = new SlaveTerminator(new Messenger(4,5));

        slaveTerminator1.messageSent();
        slaveTerminator3.messageReceived();
        slaveTerminator1.messageSent();
        slaveTerminator2.messageSent();

        slaveTerminator3.setDone();

        slaveTerminator3.messageReceived();
        slaveTerminator4.messageReceived();

        masterTerminator.messageSent();
        masterTerminator.messageSent();

        masterTerminator.setDone();

        @NotNull Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                masterTerminator.waitForTermination();
            }
        });
        t1.start();

        @NotNull Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                slaveTerminator1.waitForTermination();
            }
        });
        t2.start();

        @NotNull Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                slaveTerminator2.waitForTermination();
            }
        });
        t3.start();

        @NotNull Thread t4 = new Thread(new Runnable() {
            @Override
            public void run() {
                slaveTerminator3.waitForTermination();
            }
        });
        t4.start();

        @NotNull Thread t5 = new Thread(new Runnable() {
            @Override
            public void run() {
                slaveTerminator4.waitForTermination();
            }
        });
        t5.start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    slaveTerminator2.setDone();
                    slaveTerminator3.messageReceived();
                    slaveTerminator3.messageSent();
                    slaveTerminator1.messageSent();
                    Thread.sleep(150);
                    masterTerminator.messageReceived();
                    slaveTerminator4.messageSent();
                    slaveTerminator4.setDone();
                    slaveTerminator2.messageReceived();
                    Thread.sleep(50);
                    masterTerminator.messageReceived();
                    slaveTerminator1.messageReceived();
                    Thread.sleep(120);
                    slaveTerminator3.setDone();
                    masterTerminator.setDone();
                    slaveTerminator1.setDone();
                    Thread.sleep(44);
                    slaveTerminator2.setDone();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();
        System.out.println("Token message count: "+msgCount);
    }
}
