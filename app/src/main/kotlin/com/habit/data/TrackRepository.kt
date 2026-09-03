package com.habit.data

import kotlinx.coroutines.flow.Flow

class TrackRepository(
    private val trackDao: TrackDao,
    private val milestoneDao: MilestoneDao
) {
    fun tracksForHabit(habitId: String): Flow<List<Track>> =
        trackDao.tracksForHabit(habitId)

    suspend fun activeTracksForHabit(habitId: String): List<Track> =
        trackDao.activeTracksForHabit(habitId)

    suspend fun getById(id: String): Track? = trackDao.getById(id)

    suspend fun insert(track: Track) = trackDao.insert(track)

    suspend fun update(track: Track) = trackDao.update(track)

    suspend fun canDelete(id: String): Boolean = trackDao.activityCount(id) == 0

    suspend fun deleteById(id: String) = trackDao.deleteById(id)

    suspend fun milestonesForTrack(trackId: String): List<Milestone> =
        milestoneDao.milestonesForTrack(trackId)

    suspend fun defaultMilestone(trackId: String): Milestone? =
        milestoneDao.defaultMilestone(trackId)

    suspend fun incompleteMilestones(trackId: String): List<Milestone> =
        milestoneDao.incompleteMilestones(trackId)

    suspend fun getMilestoneById(id: Long): Milestone? = milestoneDao.getById(id)

    suspend fun insertMilestone(milestone: Milestone): Long =
        milestoneDao.insert(milestone)

    suspend fun updateMilestone(milestone: Milestone) =
        milestoneDao.update(milestone)

    // milestones checked off in this activity; pending while it is in progress
    suspend fun claimedMilestones(activityId: Long): List<Milestone> =
        milestoneDao.claimedBy(activityId)

    suspend fun claimedMilestonesByAny(activityIds: List<Long>): List<Milestone> =
        if (activityIds.isEmpty()) emptyList() else milestoneDao.claimedByAny(activityIds)

    // a check on a finished activity completes the milestone right away; on an in-progress
    // one it stays pending until the activity finishes
    suspend fun checkMilestone(milestoneId: Long, activityId: Long, completed: Boolean) =
        milestoneDao.claim(milestoneId, activityId, completed)

    suspend fun uncheckMilestone(milestoneId: Long) = milestoneDao.release(milestoneId)

    suspend fun completeClaimedMilestones(activityId: Long) =
        milestoneDao.completeClaimedBy(activityId)

    suspend fun releaseClaimedMilestones(activityId: Long) =
        milestoneDao.releaseClaimedBy(activityId)

    suspend fun milestoneIdsWithHistory(ids: List<Long>): Set<Long> =
        if (ids.isEmpty()) emptySet() else milestoneDao.milestoneIdsWithHistory(ids).toSet()

    // an unfinished activity may already point at this milestone; drop the reference so the
    // foreign key cascade does not take the agenda entry down with it
    suspend fun deleteMilestone(id: Long) {
        milestoneDao.detachPendingActivities(id)
        milestoneDao.deleteById(id)
    }

    suspend fun maxMilestoneSortOrder(trackId: String): Int =
        milestoneDao.maxSortOrder(trackId) ?: 0

    suspend fun loadFromConfig(
        tracks: List<Track>,
        milestones: Map<String, List<Milestone>>
    ) {
        trackDao.insertAll(tracks)
        milestones.forEach { (_, ms) -> milestoneDao.insertAll(ms) }
    }
}
