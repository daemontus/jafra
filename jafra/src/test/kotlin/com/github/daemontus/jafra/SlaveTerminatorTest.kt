package com.github.daemontus.jafra

import org.junit.Test
import kotlin.test.failsWith

abstract class MockSlaveMessenger : TokenMessenger {

    override fun isMaster() = false

}

class SlaveTerminatorTest {

    Test
    public fun manyForwardsMessagesReceivedAfter() {
        var flag = 0
        val messenger = object : MockSlaveMessenger() {

            synchronized
            override fun sendToNextAsync(token: Token) {
                if (flag == 1 && token.count == -2) {
                    flag = 2
                }
            }

            synchronized
            override fun receiveFromPrevious(): Token {
                if (flag == 2) {
                    return Token(2, 0)
                } else {
                    return Token(0, 0)
                }
            }

        }
        val terminator = Terminator.createNew(messenger)
        Thread(object : Runnable {
            override fun run() {
                Thread.sleep(200)
                terminator.messageReceived()
                terminator.messageReceived()
                terminator.setDone()
                flag = 1
            }
        }).start()
        terminator.setDone()
        terminator.waitForTermination()
    }


    Test
    public fun manyForwardsMessagesSentAfter() {
        var flag = 0
        val messenger = object : MockSlaveMessenger() {

            synchronized
            override fun sendToNextAsync(token: Token) {
                if (    (flag != 2 && token.flag == 2) || //terminates too soon
                        (flag == 2 && token.flag != 2 && token.count != 2) || //count decreased
                        (flag == 2 && token.flag == 2 && token.count != 0) //termination token has been tempered with
                ) {
                    throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                }
                if (token.count == 2) {
                    flag = 2
                }
            }

            synchronized
            override fun receiveFromPrevious(): Token {
                if (flag == 2) {
                    return Token(2, 0)
                } else {
                    return Token(0, 0)
                }
            }

        }
        val terminator = Terminator.createNew(messenger)
        Thread(object : Runnable {
            override fun run() {
                Thread.sleep(200)
                terminator.messageSent()
                terminator.messageSent()
                flag = 1
                terminator.setDone()
            }
        }).start()
        terminator.waitForTermination()
    }

    Test
    public fun oneForwardMessagesSentAndReceivedBefore() {
        val messenger = object : MockSlaveMessenger() {

            var counter = 0

            synchronized
            override fun sendToNextAsync(token: Token) {
                when (counter) {
                    0 -> throw UnsupportedOperationException("Sending token but nothing has been received")
                    1 -> if (token.count != -1 || token.flag != 1)
                            throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                    3 -> if (token.count != 0 || token.flag != 2)
                            throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                        else {}
                    else -> throw UnsupportedOperationException("C: " + counter)
                }
                when (counter) {
                    0 -> throw UnsupportedOperationException("Sending token but nothing has been received")
                    1 -> if (token.count != -1 || token.flag != 1)
                            throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                    3 -> if (token.count != 0 || token.flag != 2)
                            throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                        else {}
                    else -> throw UnsupportedOperationException("C: " + counter)
                }
                counter++
            }

            synchronized
            override fun receiveFromPrevious(): Token {
                when (counter) {
                    0 -> {
                        counter++
                        return Token(0, 0)
                    }
                    2 -> {
                        counter++
                        return Token(2, 0)
                    }
                    else -> throw UnsupportedOperationException("C: " + counter)
                }
            }

        }
        val terminator = Terminator.createNew(messenger)
        terminator.messageReceived()
        terminator.messageSent()
        terminator.messageReceived()
        terminator.messageSent()
        terminator.messageReceived()
        terminator.setDone()
        terminator.waitForTermination()
    }

    Test
    public fun oneForwardMessagesReceivedBefore() {
        val messenger = object : MockSlaveMessenger() {

            synchronized
            override fun sendToNextAsync(token: Token) {
                when (counter) {
                    0 -> throw UnsupportedOperationException("Sending token but nothing has been received")
                    1 -> if (token.count != -3 || token.flag != 1)
                            throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                    3 -> if (token.count != 0 || token.flag != 2)
                            throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                        else {}
                    else -> throw UnsupportedOperationException("C: " + counter)
                }
                counter++
            }

            synchronized
            override fun receiveFromPrevious(): Token {
                when (counter) {
                    0 -> {
                        counter++
                        return Token(0, 0)
                    }
                    2 -> {
                        counter++
                        return Token(2, 0)
                    }
                    else -> throw UnsupportedOperationException("C: " + counter)
                }
            }

            var counter = 0
        }
        val terminator = Terminator.createNew(messenger)
        terminator.messageReceived()
        terminator.messageReceived()
        terminator.messageReceived()
        terminator.setDone()
        terminator.waitForTermination()
    }

    Test
    public fun oneForwardMessagesSentBefore() {
        val messenger = object : MockSlaveMessenger() {

            synchronized
            override fun sendToNextAsync(token: Token) {
                when (counter) {
                    0 -> throw UnsupportedOperationException("Sending token but nothing has been received")
                    1 -> if (token.count != 2 || token.flag != 0)
                            throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                    3 -> if (token.count != 0 || token.flag != 2)
                            throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                        else {}
                    else -> throw UnsupportedOperationException("C: " + counter)
                }
                counter++
            }

            synchronized
            override fun receiveFromPrevious(): Token {
                when (counter) {
                    0 -> {
                        counter++
                        return Token(0, 0)
                    }
                    2 -> {
                        counter++
                        return Token(2, 0)
                    }
                    else -> throw UnsupportedOperationException("C: " + counter)
                }
            }

            var counter = 0

        }
        val terminator = Terminator.createNew(messenger)
        terminator.messageSent()
        terminator.messageSent()
        terminator.setDone()
        terminator.waitForTermination()
    }

    Test
    public fun oneForwardNoMessages() {
        val messenger = object : MockSlaveMessenger() {

            synchronized
            override fun sendToNextAsync(token: Token) {
                when (counter) {
                    0 -> throw UnsupportedOperationException("Sending token but nothing has been received")
                    1 -> if (token.count != 0 || token.flag != 0)
                            throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                    3 -> if (token.count != 0 || token.flag != 2)
                            throw IllegalStateException("Wrong token: " + token.flag + " " + token.count)
                        else {}
                    else -> throw UnsupportedOperationException("C: " + counter)
                }
                counter++
            }

            synchronized
            override fun receiveFromPrevious(): Token {
                when (counter) {
                    0 -> {
                        counter++
                        return Token(0, 0)
                    }
                    2 -> {
                        counter++
                        return Token(2, 0)
                    }
                    else -> throw UnsupportedOperationException("C: " + counter)
                }
            }

            var counter = 0

        }
        val terminator = Terminator.createNew(messenger)
        terminator.setDone()
        terminator.waitForTermination()
    }

    Test(timeout = 1000)
    fun wrongUse() {

        val comm = object : TokenMessenger {

            override fun sendToNextAsync(token: Token) {
                if (token.flag != 2) {
                    throw UnsupportedOperationException("Token was forwarded but no token was received")
                }
            }

            override fun receiveFromPrevious(): Token = Token(2, 0)

            override fun isMaster(): Boolean = false
        }

        val terminator = Terminator.createNew(comm)
        terminator.setDone()

        failsWith(javaClass<IllegalStateException>()) {
            terminator.messageSent()
        }

        failsWith(javaClass<IllegalStateException>()) {
            terminator.setDone()
        }

        terminator.waitForTermination()

        failsWith(javaClass<IllegalStateException>()) {
            terminator.messageSent()
        }
        failsWith(javaClass<IllegalStateException>()) {
            terminator.messageReceived()
        }
        failsWith(javaClass<IllegalStateException>()) {
            terminator.setDone()
        }
        failsWith(javaClass<IllegalStateException>()) {
            terminator.waitForTermination()
        }
    }

}