package com.glb.campulsebackend.service;

import java.util.Set;

/**
 * Interest-overlap match: 40 + 60 * (matching tags / event tags), so 40..100.
 * Returns null when either side has nothing to compare, rather than inventing a number.
 */
public final class MatchScoreCalculator {

	private static final int BASE = 40;
	private static final int RANGE = 60;

	private MatchScoreCalculator() {
	}

	public static Integer score(Set<String> userInterests, Set<String> eventTags) {
		if (userInterests.isEmpty() || eventTags.isEmpty()) {
			return null;
		}
		long matched = eventTags.stream().filter(userInterests::contains).count();
		return BASE + (int) Math.round(RANGE * (double) matched / eventTags.size());
	}
}
