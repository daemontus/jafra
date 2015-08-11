package com.github.daemontus.jafra

import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

public class SharedMemoryMessengers (
        processCount: Int,
        private val queueFactory: () -> BlockingQueue<Token> = { LinkedBlockingQueue<Token>() }
) {

    val messengers : List<QueueTokenMessenger>

    init {
        val processRange = 0..(processCount-1)

        val queues = processRange.map { queueFactory() }
        messengers = processRange.map { QueueTokenMessenger(it, queues) }
    }

}

class QueueTokenMessenger(
        myId: Int,
        val queues: List<BlockingQueue<Token>>
    ) : IdTokenMessenger(myId, queues.size()) {

    override fun sendTokenAsync(destination: Int, token: Token) {
        queues[destination].add(token)
    }

    override fun waitForToken(source: Int): Token = queues[myId].take()

}