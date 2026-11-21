package com.music.bitchord

import com.music.bitchord.data.sources.GlazeCompanion.Job
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class CompanionJobTest {
    @Test
    fun alreadyInTheLibraryCountsAsDone() {
        assertTrue(Job("1", "in_library").finished)
        assertTrue(Job("1", "skipped").finished)
        assertTrue(Job("1", "rescanned").finished)
        assertFalse(Job("1", "in_library").active)
    }

    @Test
    fun aPausedRequestIsNeitherFailedNorRunning() {
        val paused = Job("1", "needs_review", "library_review", "You have a different cut")
        assertTrue(paused.review)
        assertFalse(paused.failed)
        assertFalse(paused.active)
    }

    @Test
    fun otherReviewsAndFailuresAreFailures() {
        assertTrue(Job("1", "needs_review", "metadata_review").failed)
        assertTrue(Job("1", "failed", "no_suitable_slskd_match").failed)
        assertTrue(Job("1", "downloading").active)
    }
}
