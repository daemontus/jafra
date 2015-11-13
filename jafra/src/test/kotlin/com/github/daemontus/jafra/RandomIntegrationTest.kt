package com.github.daemontus.jafra

import org.junit.Test
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.assertFalse

class RandomTest {

    @Test(timeout = 10000)
    fun t1() = test(2, 600, false)

    @Test(timeout = 10000)
    fun t2() = test(2, 200, true)

    @Test(timeout = 10000)
    fun t3() = test(10, 200, true)

    @Test(timeout = 10000)
    fun t4() = test(10, 1000, false)

    @Test(timeout = 10000)
    fun t5() = test(2, 10, true)

    @Test(timeout = 10000)
    fun t6() = test(2, 10, false)

    @Test(timeout = 10000)
    fun t7() = test(10, 20, true)

    @Test(timeout = 10000)
    fun t8() = test(10, 20, false)


    private fun test(agents: Int, messages: Int, sleep: Boolean) {

        val workerRange = 0..(agents-1)

        val messageQueues = workerRange.map { LinkedBlockingQueue<Int>() }

        createSharedMemoryTerminators(agents)
            .mapIndexed { i, factory -> Worker(i, agents, factory, messageQueues, messages, sleep) }
            .map { it.writer.start(); it.reader.start(); it }
            .map { it.writer.join(); it }
            .map { it.reader.join()}

        assertFalse(messageQueues.any { it.isNotEmpty() })

    }

}


class Worker(
        val myId: Int,
        val processCount: Int,
        val terminators: Terminator.Factory,
        val messageQueues: List<BlockingQueue<Int>>,
        val initialMessages: Int,
        val sleep: Boolean = true
) {

    val terminator = terminators.createNew()

    @Volatile var mainTaskDone = false

    val reader = Thread() {
        //receives messages until poison pill comes
        //message can trigger another message if it's value is high enough
        var got = messageQueues[myId].take()
        while (got >= 0) {
            terminator.messageReceived()
            if (Math.random() < 0.5 * (got/200.0)) {
                terminator.messageSent()
                messageQueues[(Math.random() * processCount).toInt()].add((got/1.5).toInt())
            }
            if (sleep) Thread.sleep((Math.random() * got/2).toLong())
            synchronized(messageQueues[myId]) {
                if (mainTaskDone && terminator.working) terminator.setDone()
            }
            got = messageQueues[myId].take()
        }
        println("$myId reader out")
    }

    val writer = Thread() {
        //sends max 200 messages, after each message waits a little (simulate main workload)
        var m = Math.random() * initialMessages
        while (m > 0) {
            terminator.messageSent()
            messageQueues[(Math.random() * processCount).toInt()].add(m.toInt())
            m -= 1
            if (sleep) Thread.sleep((Math.random() * m).toLong())
        }
        println("$myId is done sending")
        synchronized(messageQueues[myId]) {
            if (messageQueues[myId].isEmpty() && terminator.working) {
                terminator.setDone()
            }
            mainTaskDone = true
        }
        terminator.waitForTermination()
        messageQueues[myId].add(-1)
    }

}