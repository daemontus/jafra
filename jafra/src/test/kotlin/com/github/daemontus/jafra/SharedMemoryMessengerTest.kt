package com.github.daemontus.jafra

import org.junit.Test
import kotlin.test.assertEquals

class MessengerTest {


    @Test fun simple() {

        val messengers = SharedMemoryMessengers(3).messengers

        messengers[0].sendToNextAsync(Token(1,1))
        assertEquals(Token(1,1), messengers[1].receiveFromPrevious())

        messengers[1].sendToNextAsync(Token(1,1))
        assertEquals(Token(1,1), messengers[2].receiveFromPrevious())

        messengers[2].sendToNextAsync(Token(1,1))
        assertEquals(Token(1,1), messengers[0].receiveFromPrevious())

        messengers[0].sendToNextAsync(Token(1,0))
        messengers[0].sendToNextAsync(Token(1,1))
        messengers[0].sendToNextAsync(Token(1,2))
        assertEquals(Token(1,0), messengers[1].receiveFromPrevious())
        assertEquals(Token(1,1), messengers[1].receiveFromPrevious())
        assertEquals(Token(1,2), messengers[1].receiveFromPrevious())

    }

}