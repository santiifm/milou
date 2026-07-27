package com.santiifm.milou.domain.job

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobStateMachineTest {

    @Test
    fun `valid transitions from QUEUED`() {
        assertTrue(JobStateMachine.canTransition(JobStatus.QUEUED, JobStatus.RUNNING))
        assertTrue(JobStateMachine.canTransition(JobStatus.QUEUED, JobStatus.CANCELLED))
        assertFalse(JobStateMachine.canTransition(JobStatus.QUEUED, JobStatus.COMPLETED))
    }

    @Test
    fun `valid transitions from RUNNING`() {
        assertTrue(JobStateMachine.canTransition(JobStatus.RUNNING, JobStatus.COMPLETED))
        assertTrue(JobStateMachine.canTransition(JobStatus.RUNNING, JobStatus.FAILED))
        assertTrue(JobStateMachine.canTransition(JobStatus.RUNNING, JobStatus.RECOVERABLE))
        assertTrue(JobStateMachine.canTransition(JobStatus.RUNNING, JobStatus.PAUSED))
    }

    @Test
    fun `invalid transitions from COMPLETED`() {
        assertFalse(JobStateMachine.canTransition(JobStatus.COMPLETED, JobStatus.RUNNING))
        assertFalse(JobStateMachine.canTransition(JobStatus.COMPLETED, JobStatus.QUEUED))
    }
}
