package com.santiifm.milou.domain.eventbus

import com.santiifm.milou.domain.event.MilouEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StandardEventBusTest {

    private data class TestEvent(override val timestamp: Long = System.currentTimeMillis()) : MilouEvent

    @Test
    fun `publish should broadcast to all subscribers`() = runTest {
        val eventBus = StandardEventBus()
        val eventsA = mutableListOf<MilouEvent>()
        val eventsB = mutableListOf<MilouEvent>()

        val jobA = launch { eventBus.events.collect { eventsA.add(it) } }
        val jobB = launch { eventBus.events.collect { eventsB.add(it) } }

        val event = TestEvent()
        eventBus.publish(event)

        // Give it a moment to propagate
        kotlinx.coroutines.delay(100)

        assertEquals(1, eventsA.size)
        assertEquals(1, eventsB.size)
        assertEquals(event, eventsA[0])
        assertEquals(event, eventsB[0])

        jobA.cancel()
        jobB.cancel()
    }

    @Test
    fun `subscriber isolation - failing subscriber should not affect others`() = runTest {
        val eventBus = StandardEventBus()
        val eventsB = mutableListOf<MilouEvent>()

        val jobA = launch {
            try {
                eventBus.events.collect {
                    throw RuntimeException("Boom")
                }
            } catch (_: Exception) {}
        }
        val jobB = launch { eventBus.events.collect { eventsB.add(it) } }

        val event = TestEvent()
        eventBus.publish(event)

        kotlinx.coroutines.delay(100)

        assertEquals(1, eventsB.size)
        assertEquals(event, eventsB[0])

        jobA.cancel()
        jobB.cancel()
    }
}
