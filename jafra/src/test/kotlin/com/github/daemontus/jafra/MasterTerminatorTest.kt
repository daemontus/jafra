package com.github.daemontus.jafra

import org.junit.Test
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

open class MockMasterMessenger : TokenMessenger {

    var mc: Int = 0
    val queue: BlockingQueue<Token> = LinkedBlockingQueue<Token>()

    @Synchronized
    override fun sendToNextAsync(token: Token) {
        mc += 1
        queue.add(token)
    }

    override fun receiveFromPrevious(): Token = queue.take()

    override fun isMaster() = true

}

class MasterTerminatorTest {

    @Test(timeout = 2000)
    fun complexTest() {
        var flag = 1
        var count = 0
        val comm = object : MockMasterMessenger() {

            override fun receiveFromPrevious(): Token {
                val t = queue.take()
                if (t.flag == 2) {
                    return t
                } else {
                    //we use dirty flag first to simulate the environment that received the messages
                    return Token(Math.min(t.flag + flag, 1), t.count + count)  //2 because 2 messages are sent
                }
            }

        }
        val terminator = Terminator.createNew(comm)
        terminator.messageSent()
        count++    //message created in system
        terminator.messageReceived()   //message received
        Thread(Runnable {
            try {
                Thread.sleep(100)
                count--    //pair for first message Sent
                terminator.messageSent()
                terminator.setDone()
                Thread.sleep(100)
                count++ //message created in system
                terminator.messageReceived()   //message received
                Thread.sleep(100)
                count--    //pair for second message Sent
                Thread.sleep(100)
                flag = 0
                terminator.setDone()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }).start()
        terminator.waitForTermination()

        assertTrue(comm.queue.isEmpty())
        assertTrue(comm.mc > 2) //this can cycle for quite a long time, so we just know it's going to be a big number
    }

    @Test(timeout = 1000)
    fun receivedMessagesAfterStart() {
        var flag = 1

        val comm = object : MockMasterMessenger() {

            override fun receiveFromPrevious(): Token {
                val t = queue.take()
                if (t.flag == 2) {
                    return t
                } else {
                    //we use dirty flag first to simulate the environment that received the messages
                    return Token(Math.min(t.flag + flag, 1), t.count + 2)  //2 because 2 messages are sent
                }
            }

        }
        val terminator = Terminator.createNew(comm)
        Thread(Runnable {
            try {
                Thread.sleep(200)
                terminator.messageReceived()
                Thread.sleep(100)
                terminator.messageReceived()
                Thread.sleep(100)
                flag = 0
                terminator.setDone()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }).start()
        terminator.waitForTermination()

        assertTrue(comm.queue.isEmpty())
        assertEquals(2, comm.mc)
    }

    @Test(timeout = 1000)
    fun sentMessagesAfterStart() {
        var flag = 1

        val comm = object : MockMasterMessenger() {

            override fun receiveFromPrevious(): Token {
                val t = queue.take()
                if (t.flag == 2) {
                    return t
                } else {
                    //we use dirty flag first to simulate the environment that received the messages
                    return Token(Math.min(t.flag + flag, 1), t.count - 2)  //2 because 2 messages are sent
                }
            }

        }
        val terminator = Terminator.createNew(comm)
        Thread(object : Runnable {
            override fun run() {
                try {
                    Thread.sleep(50)
                    terminator.messageSent()
                    Thread.sleep(50)
                    terminator.messageSent()
                    flag = 0
                    terminator.setDone()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }).start()
        terminator.waitForTermination()

        assertTrue(comm.queue.isEmpty())
        assertEquals(2, comm.mc)

    }

    @Test(timeout = 1000)
    fun receiveMessagesBeforeDone() {
        //we don't need to simulate flags here, since message send does not modify environment flags
        val comm = object : MockMasterMessenger() {

            override fun receiveFromPrevious(): Token {
                val token = queue.take()
                if (token.flag == 2) return token
                return Token(token.flag, token.count + 2)   // +2 for two sent messages
            }

        }

        //receive and finish work before start
        val terminator = Terminator.createNew(comm)
        terminator.messageReceived()
        Thread.sleep(100)
        terminator.messageReceived()
        terminator.setDone()
        terminator.waitForTermination()

        assertEquals(2, comm.mc)
        assertTrue(comm.queue.isEmpty())

        //receive before start and finish after start
        val terminator2 = Terminator.createNew(comm)
        terminator2.messageReceived()
        Thread(object : Runnable {
            override fun run() {
                Thread.sleep(400)
                terminator2.setDone()
            }
        }).start()
        Thread.sleep(100)
        terminator2.messageReceived()
        terminator2.waitForTermination()

        //only one message is needed to conclude termination, since
        //environment is clean and probe is sent only at the first setDone
        assertEquals(4, comm.mc)
        assertTrue(comm.queue.isEmpty())
    }

    @Test(timeout = 1000)
    fun sendMessagesBeforeDone() {

        val comm = object : MockMasterMessenger() {

            var flag: Int = 1

            override fun receiveFromPrevious(): Token {
                val t = queue.take()
                if (t.flag == 2) {
                    return t
                } else {
                    //we use dirty flag first to simulate the environment that received the messages
                    val r = Token(Math.min(t.flag + flag, 1), t.count - 2)  //2 because 2 messages are sent
                    flag = 0
                    return r
                }
            }

        }

        val terminator = Terminator.createNew(comm)
        terminator.messageSent()
        Thread.sleep(100)
        terminator.messageSent()
        terminator.setDone()
        terminator.waitForTermination()

        assertTrue(comm.queue.isEmpty())
        assertEquals(0, comm.flag)
        //one with dirty flag, second to verify, last as poison pill
        assertEquals(3, comm.mc)

    }

    @Test(timeout = 1000)
    fun wrongUse() {

        val comm = MockMasterMessenger()

        val terminator = Terminator.createNew(comm)
        terminator.setDone()

         //terminator not working

        assertFailsWith(IllegalStateException::class) {
            terminator.setDone()
        }

        assertFailsWith(IllegalStateException::class) {
            terminator.messageSent()
        }

        terminator.waitForTermination()

        //terminator finalized

        assertFailsWith(IllegalStateException::class) {
            terminator.setDone()
        }

        assertFailsWith(IllegalStateException::class) {
            terminator.messageSent()
        }

        assertFailsWith(IllegalStateException::class) {
            terminator.messageReceived()
        }

        assertFailsWith(IllegalStateException::class) {
            terminator.waitForTermination()
        }

    }

    @Test(timeout = 1000)
    fun noMessages() {

        //In case there are no other messages, terminator should finish after first onDone call
        //It should send exactly two tokens - one for termination check, second as poison pill

        val comm = MockMasterMessenger()

        val terminator = Terminator.createNew(comm)

        terminator.setDone()
        terminator.waitForTermination()

        assertEquals(2, comm.mc)
        assertTrue(comm.queue.isEmpty())

    }

}