package com.glb.campulsebackend.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchScoreCalculatorTest {

	@Test
	void fullOverlapScores100() {
		assertEquals(100, MatchScoreCalculator.score(Set.of("ai", "design"), Set.of("ai", "design")));
	}

	@Test
	void noOverlapScoresBaseline40() {
		assertEquals(40, MatchScoreCalculator.score(Set.of("music"), Set.of("ai", "design")));
	}

	@Test
	void partialOverlapIsProportional() {
		// 2 of 3 event tags matched -> 40 + 60 * 2/3 = 80
		assertEquals(80, MatchScoreCalculator.score(
				Set.of("ai", "design", "entrepreneurship"), Set.of("ai", "coding", "entrepreneurship")));
		// 1 of 2 -> 70
		assertEquals(70, MatchScoreCalculator.score(Set.of("design"), Set.of("design", "ux")));
	}

	@Test
	void returnsNullWhenThereIsNothingToCompare() {
		assertNull(MatchScoreCalculator.score(Set.of(), Set.of("ai")));
		assertNull(MatchScoreCalculator.score(Set.of("ai"), Set.of()));
	}
}
