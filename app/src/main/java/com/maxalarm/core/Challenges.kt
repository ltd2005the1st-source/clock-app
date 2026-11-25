package com.maxalarm.core

import kotlin.random.Random

/**
 * The 20 different actions that make up a snooze. A ringing alarm shows all of them,
 * one after another, in a fresh random order every time it rings.
 */
enum class ChallengeType(val title: String, val instruction: String) {
    TAP("Tap", "Tap the button."),
    DOUBLE_TAP("Double-tap", "Double-tap the panel."),
    LONG_PRESS("Press and hold", "Hold the button down until the ring fills."),
    SWIPE_LEFT("Swipe left", "Swipe across the panel to the left."),
    SWIPE_RIGHT("Swipe right", "Swipe across the panel to the right."),
    SWIPE_UP("Swipe up", "Swipe up on the panel."),
    SWIPE_DOWN("Swipe down", "Swipe down on the panel."),
    MOVING_TARGET("Catch the dot", "Tap the dot. It won't sit still."),
    TYPE_WORD("Type the word", "Type the word exactly as shown."),
    MATH("Solve it", "Work out the answer."),
    SLIDER("Slide to the end", "Drag the slider all the way right. Let go early and it resets."),
    SWITCH("Flip the switch", "Turn the switch on."),
    TWO_FINGER_HOLD("Two fingers", "Hold two fingers on the panel until the ring fills."),
    COLOR_PICK("Pick the color", "Tap the circle with the named color."),
    CHECKBOXES("Check everything", "Tick every box."),
    CODE_ENTRY("Enter the code", "Key in the code shown."),
    SHAKE("Shake the phone", "Shake your phone hard until the bar fills."),
    LARGEST_NUMBER("Find the biggest", "Tap the largest number."),
    NUMBER_ORDER("In order", "Tap the tiles from 1 to 6. A wrong tile starts you over."),
    COUNTER("Set the counter", "Use + and − to reach the target number."),
}

/** Number of actions needed to snooze. Always equal to the number of challenge types. */
val SNOOZE_STEPS: Int = ChallengeType.entries.size

data class ChallengeStep(val index: Int, val type: ChallengeType, val seed: Long)

object ChallengePlan {
    /** All 20 challenge types, each used exactly once, shuffled. */
    fun create(random: Random = Random.Default): List<ChallengeStep> =
        ChallengeType.entries.shuffled(random).mapIndexed { i, type ->
            ChallengeStep(index = i, type = type, seed = random.nextLong())
        }
}

/** Deterministic puzzle content for a step, derived from its seed. */
object ChallengeData {
    val WORDS = listOf(
        "AWAKE", "SUNRISE", "COFFEE", "MORNING", "STRETCH",
        "BREAKFAST", "SHOWER", "OUTSIDE", "DAYLIGHT", "UPRIGHT",
    )

    fun word(seed: Long): String = WORDS[Random(seed).nextInt(WORDS.size)]

    data class MathProblem(val text: String, val answer: Int)

    fun math(seed: Long): MathProblem {
        val r = Random(seed)
        return when (r.nextInt(3)) {
            0 -> {
                val a = r.nextInt(12, 60)
                val b = r.nextInt(12, 60)
                MathProblem("$a + $b", a + b)
            }
            1 -> {
                val a = r.nextInt(50, 100)
                val b = r.nextInt(11, a - 9)
                MathProblem("$a − $b", a - b)
            }
            else -> {
                val a = r.nextInt(3, 10)
                val b = r.nextInt(6, 13)
                MathProblem("$a × $b", a * b)
            }
        }
    }

    fun code(seed: Long): String {
        val r = Random(seed)
        return (1..4).joinToString("") { r.nextInt(10).toString() }
    }

    /** Display order of [colorCount] colors, and which one to tap. */
    fun colorPick(seed: Long, colorCount: Int): Pair<List<Int>, Int> {
        val r = Random(seed)
        val order = (0 until colorCount).shuffled(r)
        return order to r.nextInt(colorCount)
    }

    /** Nine distinct two-digit numbers. */
    fun numberGrid(seed: Long): List<Int> = (10..99).shuffled(Random(seed)).take(9)

    /** Tiles 1..6 in a shuffled layout. */
    fun tileOrder(seed: Long): List<Int> = (1..6).shuffled(Random(seed))

    fun counterTarget(seed: Long): Int = Random(seed).nextInt(7, 16)
}
