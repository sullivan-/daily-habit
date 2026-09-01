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
