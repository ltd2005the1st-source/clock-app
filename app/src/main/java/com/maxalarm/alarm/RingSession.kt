package com.maxalarm.alarm

import com.maxalarm.core.Alarm
import com.maxalarm.core.ChallengePlan
import com.maxalarm.core.ChallengeStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory progress through the 20 snooze steps for whatever is ringing right now. */
object RingSession {

    data class State(
        val alarms: List<Alarm>,
        val steps: List<ChallengeStep>,
        val completed: Int,
    ) {
        val isComplete: Boolean get() = completed >= steps.size
        val current: ChallengeStep? get() = steps.getOrNull(completed)
        /** When several alarms ring together, the shortest snooze wins. */
        val snoozeMinutes: Int get() = alarms.minOf { it.snoozeMinutes }
        val isCountdown: Boolean get() = alarms.all { it.id == com.maxalarm.core.Alarm.COUNTDOWN_ID }
    }

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    fun ensure(alarms: List<Alarm>) {
        // Several alarms ringing together use the largest task count among them.
        _state.update {
            it?.copy(alarms = alarms) ?: State(alarms, ChallengePlan.create(alarms.maxOf { a -> a.taskCount }), 0)
        }
    }

    /** Advances only if [index] is the step currently showing, so double taps can't skip steps. */
    fun completeStep(index: Int) {
        _state.update { s -> if (s != null && s.completed == index) s.copy(completed = index + 1) else s }
    }

    fun clear() {
        _state.value = null
    }
}
