package com.github.daemontus.jafra

import org.junit.Test
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertFalse

class Messenger(id: Int, count: Int, val tokenQueues: List<BlockingQueue<Token>>) : IdTokenMessenger(id, count) {
    override fun sendTokenAsync(destination: Int, token: Token) {
        tokenQueues[destination].add(token)
    }
    override fun waitForToken(source: Int): Token {
        return tokenQueues[myId].take()
    }
}

class RandomTest {

    Test fun t1() = test(2, 600, false)
    Test fun t2() = test(2, 200, true)
    Test fun t3() = test(10, 200, true)
    Test fun t4() = test(10, 1000, false)
    Test fun t5() = test(2, 10, true)
    Test fun t6() = test(2, 10, false)
    Test fun t7() = test(10, 20, true)
    Test fun t8() = test(10, 20, false)


    private fun test(agents: Int, messages: Int, sleep: Boolean) {

        val workerRange = 0..(agents-1)

        val tokenQueues = workerRange.map { LinkedBlockingQueue<Token>() }
        val messageQueues = workerRange.map { LinkedBlockingQueue<Int>() }
        val tokenMessengers = workerRange.map { Messenger(it, agents, tokenQueues) }

        tokenMessengers
                .map { Worker(it, messageQueues, messages, sleep) }
                .map { it.writer.start(); it.reader.start(); it }
                .map { it.writer.join(); it }
                .map { it.reader.join()}

        assertFalse(messageQueues.any { it.isNotEmpty() })
        assertFalse(tokenQueues.any { it.isNotEmpty() })

    }

}


class Worker(
        val messenger: IdTokenMessenger,
        val messageQueues: List<BlockingQueue<Int>>,
        val initialMessages: Int,
        val sleep: Boolean = true
) {

    val terminator = Terminator.createNew(messenger)

    volatile var mainTaskDone = false

    val reader = Thread() {
        //receives messages until poison pill comes
        //message can trigger another message if it's value is high enough
        var got = messageQueues[messenger.myId].take()
        while (got >= 0) {
            terminator.messageReceived()
            if (Math.random() < 0.5 * (got/200.0)) {
                terminator.messageSent()
                messageQueues[(Math.random() * messenger.processCount).toInt()].add((got/1.5).toInt())
            }
            if (sleep) Thread.sleep((Math.random() * got/2).toLong())
            synchronized(messageQueues[messenger.myId]) {
                if (mainTaskDone && terminator.working) terminator.setDone()
            }
            got = messageQueues[messenger.myId].take()
        }
        println("${messenger.myId} reader out")
    }

    val writer = Thread() {
        //sends max 200 messages, after each message waits a little (simulate main workload)
        var m = Math.random() * initialMessages
        while (m > 0) {
            terminator.messageSent()
            messageQueues[(Math.random() * messenger.processCount).toInt()].add(m.toInt())
            m -= 1
            if (sleep) Thread.sleep((Math.random() * m).toLong())
        }
        println("${messenger.myId} is done sending")
        synchronized(messageQueues[messenger.myId]) {
            if (messageQueues[messenger.myId].isEmpty() && terminator.working) {
                terminator.setDone()
            }
            mainTaskDone = true
        }
        terminator.waitForTermination()
        messageQueues[messenger.myId].add(-1)
    }

}