package daemontus.jafra;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test class for slave terminator.
 */
public class SlaveTerminatorTest {


    @NotNull
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private int flag;

    @Test
    public void manyForwardsMessagesReceivedAfter() {
        flag = 0;
        @NotNull TokenMessenger messenger = new IdTokenMessenger(1, 2) {
            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                if (flag == 1 && token.flag < 1) {
                    throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                }
                if (token.count == -2) {
                    flag = 2;
                }
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                if (flag == 2) {
                    return new Token(2,0);
                } else {
                    return new Token(0,0);
                }
            }
        };
        @NotNull final SlaveTerminator terminator = new SlaveTerminator(messenger);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    terminator.messageReceived();
                    terminator.messageReceived();
                    terminator.setDone();
                    flag = 1;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        terminator.setDone();
        terminator.waitForTermination();
    }


    @Test
    public void manyForwardsMessagesSentAfter() {
        flag = 0;
        @NotNull TokenMessenger messenger = new IdTokenMessenger(1, 2) {

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                if (    (flag != 2 && token.flag == 2) ||   //terminates too soon
                        (flag == 2 && token.flag != 2 && token.count != 2) || //count decreased
                        (flag == 2 && token.flag == 2 && token.count != 0) //termination token has been tempered with
                    ) {
                    throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                }
                if (token.count == 2) {
                    flag = 2;
                }
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                if (flag == 2) {
                    return new Token(2,0);
                } else {
                    return new Token(0,0);
                }
            }
        };
        @NotNull final SlaveTerminator terminator = new SlaveTerminator(messenger);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    terminator.messageSent();
                    terminator.messageSent();
                    flag = 1;
                    terminator.setDone();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        terminator.waitForTermination();
    }

    @Test
    public void oneForwardMessagesSentAndReceivedBefore() {
        @NotNull TokenMessenger messenger = new IdTokenMessenger(1, 2) {
            int counter = 0;

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                switch (counter) {
                    case 0:
                        throw new UnsupportedOperationException("Sending token but nothing has been received");
                    case 1:
                        if (token.count != -1 || token.flag != 1) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    case 3:
                        if (token.count != 0 || token.flag != 2) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
                counter++;
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                switch (counter) {
                    case 0:
                        counter++;
                        return new Token(0,0);
                    case 2:
                        counter++;
                        return new Token(2,0);
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
            }
        };
        @NotNull SlaveTerminator terminator = new SlaveTerminator(messenger);
        terminator.messageReceived();
        terminator.messageSent();
        terminator.messageReceived();
        terminator.messageSent();
        terminator.messageReceived();
        terminator.setDone();
        terminator.waitForTermination();
    }

    @Test
    public void oneForwardMessagesReceivedBefore() {
        @NotNull TokenMessenger messenger = new IdTokenMessenger(1, 2) {
            int counter = 0;

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                switch (counter) {
                    case 0:
                        throw new UnsupportedOperationException("Sending token but nothing has been received");
                    case 1:
                        if (token.count != -3 || token.flag != 1) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    case 3:
                        if (token.count != 0 || token.flag != 2) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
                counter++;
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                switch (counter) {
                    case 0:
                        counter++;
                        return new Token(0,0);
                    case 2:
                        counter++;
                        return new Token(2,0);
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
            }
        };
        @NotNull SlaveTerminator terminator = new SlaveTerminator(messenger);
        terminator.messageReceived();
        terminator.messageReceived();
        terminator.messageReceived();
        terminator.setDone();
        terminator.waitForTermination();
    }

    @Test
    public void oneForwardMessagesSentBefore() {
        @NotNull TokenMessenger messenger = new IdTokenMessenger(1, 2) {
            int counter = 0;

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                switch (counter) {
                    case 0:
                        throw new UnsupportedOperationException("Sending token but nothing has been received");
                    case 1:
                        if (token.count != 2 || token.flag != 0) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    case 3:
                        if (token.count != 0 || token.flag != 2) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
                counter++;
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                switch (counter) {
                    case 0:
                        counter++;
                        return new Token(0,0);
                    case 2:
                        counter++;
                        return new Token(2,0);
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
            }
        };
        @NotNull SlaveTerminator terminator = new SlaveTerminator(messenger);
        terminator.messageSent();
        terminator.messageSent();
        terminator.setDone();
        terminator.waitForTermination();
    }

    @Test
    public void oneForwardNoMessages() {
        @NotNull TokenMessenger messenger = new IdTokenMessenger(1, 2) {
            int counter = 0;

            @Override
            public synchronized void sendTokenAsync(int destination, @NotNull Token token) {
                switch (counter) {
                    case 0:
                        throw new UnsupportedOperationException("Sending token but nothing has been received");
                    case 1:
                        if (token.count != 0 || token.flag != 0) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    case 3:
                        if (token.count != 0 || token.flag != 2) {
                            throw new IllegalStateException("Wrong token: "+token.flag+" "+token.count);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
                counter++;
            }

            @NotNull
            @Override
            public synchronized Token waitForToken(int source) {
                switch (counter) {
                    case 0:
                        counter++;
                        return new Token(0,0);
                    case 2:
                        counter++;
                        return new Token(2,0);
                    default:
                        throw new UnsupportedOperationException("C: "+counter);
                }
            }
        };
        @NotNull SlaveTerminator terminator = new SlaveTerminator(messenger);
        terminator.setDone();
        terminator.waitForTermination();
    }

    @Test
    public void wrongUse() {
        @NotNull TokenMessenger messenger = new IdTokenMessenger(1, 2) {

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                if (token.flag != 2 && destination != 0) {
                    throw new UnsupportedOperationException("Token was forwarded but no token was received");
                }
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                return new Token(2,0);
            }
        };
        @NotNull SlaveTerminator terminator = new SlaveTerminator(messenger);
        terminator.setDone();
        exception.expect(IllegalStateException.class);
        terminator.messageSent();
        terminator.waitForTermination();
        exception.expect(IllegalStateException.class);
        terminator.messageReceived();
        exception.expect(IllegalStateException.class);
        terminator.messageSent();
        exception.expect(IllegalStateException.class);
        terminator.setDone();
        exception.expect(IllegalStateException.class);
        terminator.waitForTermination();
        exception.expect(IllegalStateException.class);
        new SlaveTerminator(new IdTokenMessenger(0, 4) {

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                return new Token(0,0);
            }
        });
    }


}
