package com.github.daemontus.jafra

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class QueueTokenMessenger(
        myId: Int,
        val queues: List<BlockingQueue<Token>>
    ) : IdTokenMessenger(myId, queues.size) {

    override fun sendTokenAsync(destination: Int, token: Token) {
        queues[destination].add(token)
    }

    override fun waitForToken(source: Int): Token = queues[myId].take()

}

public fun createSharedMemoryTerminators(count: Int): List<Terminator.Factory> {
    val queues = (1..count).map { LinkedBlockingQueue<Token>() }
    return (0..(count-1)).map { QueueTokenMessenger(it, queues) }.map { Terminator.Factory(it) }
}