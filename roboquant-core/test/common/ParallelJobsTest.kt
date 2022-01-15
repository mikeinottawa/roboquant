package org.roboquant.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

internal class ParallelJobsTest {

    private suspend fun test() {
        delay(1)
    }

    @Test
    fun joinJobs() = runBlocking {
        val jobs = ParallelJobs()
        for (i in 1..3) jobs.add { test() }
        assertEquals(3, jobs.size)
        jobs.joinAll()
        assertEquals(0, jobs.size)
    }

    @Test
    fun seqJobs() = runBlocking {
        val jobs = ParallelJobs(simulateSequential = true)
        for (i in 1..3) jobs.add { test() }
        assertEquals(3, jobs.size)
        jobs.joinAll()
        assertEquals(0, jobs.size)
    }

    @Test
    fun cancelJobs() = runBlocking {
        val jobs = ParallelJobs()
        for (i in 1..3) jobs.add { test() }
        assertEquals(3, jobs.size)
        jobs.cancelAll()
        assertEquals(0, jobs.size)
    }

}