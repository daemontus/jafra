package com.daemontus.jafra;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Test class for master terminator.
 */
public class MasterTerminatorTest {

    /* We are not testing for situations when process count is 1 but terminator sends/receives messages,
       because in such cases, the behaviour is undefined. */

    @NotNull
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private int flag;
    private int count;

    @Test
    public void complexTest() {
        flag = 1;
        count = 0;
        @NotNull TokenMessenger messenger = new IdTokenMessenger(0, 2) {

            @NotNull
            final BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    Token token = queue.take();
                    if (token.flag == 2) return token;
                    return new Token(Math.min(token.flag + flag, 1), token.count + count);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        @NotNull final MasterTerminator terminator = new MasterTerminator(messenger);
        terminator.messageSent();
        count++;    //message created in system
        terminator.messageReceived();   //message received
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    count--;    //pair for first message Sent
                    terminator.messageSent();
                    terminator.setDone();
                    Thread.sleep(100);
                    count++; //message created in system
                    terminator.messageReceived();   //message received
                    Thread.sleep(100);
                    count--;    //pair for second message Sent
                    Thread.sleep(100);
                    flag = 0;
                    terminator.setDone();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        terminator.waitForTermination();
    }

    @Test
    public void receivedMessagesAfterStart() throws InterruptedException {
        flag = 1;
        @NotNull TokenMessenger messenger = new IdTokenMessenger(0, 2) {

            @NotNull
            final BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    Token token = queue.take();
                    if (token.flag == 2) return token;
                    return new Token(Math.min(token.flag + flag, 1), token.count + 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        @NotNull final MasterTerminator terminator = new MasterTerminator(messenger);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    terminator.messageReceived();
                    Thread.sleep(100);
                    terminator.messageReceived();
                    Thread.sleep(100);
                    flag = 0;
                    terminator.setDone();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        terminator.waitForTermination();
    }

    @Test
    public void sentMessagesAfterStart() throws InterruptedException {
        flag = 1;
        @NotNull TokenMessenger messenger = new IdTokenMessenger(0, 2) {

            @NotNull
            final BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    Token token = queue.take();
                    if (token.flag == 2) return token;
                    return new Token(Math.min(token.flag + flag, 1), token.count - 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        @NotNull final MasterTerminator terminator = new MasterTerminator(messenger);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50);
                    terminator.messageSent();
                    Thread.sleep(50);
                    terminator.messageSent();
                    flag = 0;
                    terminator.setDone();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        terminator.waitForTermination();
    }

    @Test
    public void receivedMessagesBeforeStart() throws InterruptedException {
        @NotNull TokenMessenger messenger = new IdTokenMessenger(0, 2) {

            @NotNull
            final BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    Token token = queue.take();
                    if (token.flag == 2) return token;
                    return new Token(token.flag, token.count + 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        //receive and finish work before start
        @NotNull MasterTerminator terminator = new MasterTerminator(messenger);
        terminator.messageReceived();
        Thread.sleep(100);
        terminator.messageReceived();
        terminator.setDone();
        terminator.waitForTermination();
        //receive before start and finish after start
        @NotNull final MasterTerminator terminator2 = new MasterTerminator(messenger);
        terminator2.messageReceived();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(400);
                    terminator2.setDone();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Thread.sleep(100);
        terminator2.messageReceived();
        terminator2.waitForTermination();
    }

    @Test
    public void sentMessagesBeforeStart() throws InterruptedException {
        @NotNull TokenMessenger messenger = new IdTokenMessenger(0, 2) {

            @NotNull
            final BlockingQueue<Token> queue = new LinkedBlockingQueue<>();
            int flag = 1;

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    Token token = queue.take();
                    if (token.flag == 2) return token;
                    @NotNull Token ret = new Token(Math.min(token.flag + flag, 1), token.count - 2);
                    flag = 0;
                    return ret;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        @NotNull MasterTerminator terminator = new MasterTerminator(messenger);
        terminator.messageSent();
        Thread.sleep(100);
        terminator.messageSent();
        terminator.setDone();
        terminator.waitForTermination();
    }

    @Test
    public void wrongUse() {
        @NotNull TokenMessenger messenger = new IdTokenMessenger(0, 1) {

            @NotNull
            final BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    return queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        @NotNull MasterTerminator terminator = new MasterTerminator(messenger);
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
        new MasterTerminator(new IdTokenMessenger(2, 4) {

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

    @Test
    public void noMessages() {
        @NotNull TokenMessenger messenger = new IdTokenMessenger(0, 1) {

            @NotNull
            final BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    return queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        @NotNull MasterTerminator terminator = new MasterTerminator(messenger);
        terminator.setDone();
        terminator.waitForTermination();
        messenger = new IdTokenMessenger(0, 3) {

            @NotNull
            final BlockingQueue<Token> queue = new LinkedBlockingQueue<>();

            @Override
            public void sendTokenAsync(int destination, @NotNull Token token) {
                queue.add(token);
            }

            @NotNull
            @Override
            public Token waitForToken(int source) {
                try {
                    return queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Token(0,0);
                }
            }
        };
        terminator = new MasterTerminator(messenger);
        terminator.setDone();
        terminator.waitForTermination();
    }

}
