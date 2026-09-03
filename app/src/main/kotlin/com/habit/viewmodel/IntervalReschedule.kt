package com.habit.viewmodel

// where the chime schedule lands after switching intervals mid-cycle
data class IntervalReschedule(val nextAtMs: Long, val chimeNow: Boolean) {
    companion object {
        // the position within the current cycle is kept, so 5s into an 8s cycle becomes 5s into
        // a 9s cycle. once the new interval has already elapsed, the cycle restarts from now and
        // the caller should chime immediately
        fun of(nextAtMs: Long, oldMs: Long, newMs: Long, elapsedMs: Long): IntervalReschedule {
            val kept = nextAtMs - oldMs + newMs
            return if (kept > elapsedMs) IntervalReschedule(kept, chimeNow = false)
            else IntervalReschedule(elapsedMs + newMs, chimeNow = true)
        }
    }
}
