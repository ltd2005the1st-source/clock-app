package com.maxalarm.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChallengesTest {
    @Test fun exactlyTwentyDistinctActions() {
        assertEquals(20, SNOOZE_STEPS)
        val plan = ChallengePlan.create(Random(1))
        assertEquals(20, plan.size)
        assertEquals(20, plan.map { it.type }.toSet().size)
        assertEquals((0 until 20).toList(), plan.map { it.index })
    }

    @Test fun orderChangesBetweenRings() {
        val a = ChallengePlan.create(Random(1)).map { it.type }
        val b = ChallengePlan.create(Random(2)).map { it.type }
        assertNotEquals(a, b)
    }

    @Test fun mathAnswersAreRight() {
        repeat(500) { seed ->
            val p = ChallengeData.math(seed.toLong())
            val parts = p.text.split(" ")
            val x = parts[0].toInt(); val y = parts[2].toInt()
            val expected = when (parts[1]) {
                "+" -> x + y
                "−" -> x - y
                "×" -> x * y
                else -> error(parts[1])
            }
            assertEquals(p.text, expected, p.answer)
            assertTrue(p.answer > 0)
        }
    }

    @Test fun puzzleDataIsWellFormed() {
        repeat(200) { s ->
            val seed = s.toLong()
            assertEquals(4, ChallengeData.code(seed).length)
            assertEquals(9, ChallengeData.numberGrid(seed).toSet().size)
            assertEquals((1..6).toSet(), ChallengeData.tileOrder(seed).toSet())
            val (order, target) = ChallengeData.colorPick(seed, 6)
            assertEquals((0 until 6).toSet(), order.toSet())
            assertTrue(target in 0 until 6)
            assertTrue(ChallengeData.counterTarget(seed) in 7..15)
            assertTrue(ChallengeData.word(seed) in ChallengeData.WORDS)
        }
    }

    @Test fun sameSeedSamePuzzle() {
        assertEquals(ChallengeData.math(42), ChallengeData.math(42))
        assertEquals(ChallengeData.code(42), ChallengeData.code(42))
    }
}
