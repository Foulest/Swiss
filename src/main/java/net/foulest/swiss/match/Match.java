/*
 * Swiss - a Monte Carlo bracket simulator for Counter-Strike 2 tournaments.
 * Copyright (C) 2024 Foulest (https://github.com/Foulest)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.foulest.swiss.match;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.foulest.swiss.team.Team;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Represents a match between two teams.
 */
@Data
@AllArgsConstructor
public class Match {

    private Team team1;
    private Team team2;
    private int maxRounds;

    /**
     * Simulates a match between two teams.
     *
     * @param mostLikelyOnly Whether to return the most likely winner only.
     * @return The winning team.
     */
    public Team simulate(boolean mostLikelyOnly) {
        int team1Wins = 0;
        int team2Wins = 0;
        double winProbability = calculateWinProbability(team1, team2);

        // Return the most likely winner only
        if (mostLikelyOnly) {
            return winProbability >= 0.5 ? team1 : team2;
        }

        // Generate random numbers in bulk for up to 5 rounds
        double[] randomNumbers = new Random().doubles(5).toArray();
        int round = 0;

        // Simulates the match
        while (team1Wins < maxRounds && team2Wins < maxRounds) {
            if (randomNumbers[round] < winProbability) {
                team1Wins++;
            } else {
                team2Wins++;
            }

            round++;
        }
        return team1Wins == maxRounds ? team1 : team2;
    }

    /**
     * Calculates the win probability of a team against another team.
     * - 20% of the win probability is based on the team's world ranking.
     * - 80% of the win probability is based on the team's average player rating.
     *
     * @param t1 The first team.
     * @param t2 The second team.
     * @return The win probability of the first team.
     */
    private static double calculateWinProbability(@NotNull Team t1, @NotNull Team t2) {
        final double worldRankingWeight = 0.20 / 100.0; // Precompute constant
        final double playerRatingWeight = 0.80;
        final double scale = 5.0;

        // Compute scores
        double t1Score = worldRankingWeight * (100.0 - t1.getWorldRanking()) + playerRatingWeight * t1.getAvgPlayerRating();
        double t2Score = worldRankingWeight * (100.0 - t2.getWorldRanking()) + playerRatingWeight * t2.getAvgPlayerRating();

        // Calculate win probability
        return 1.0 / (1.0 + Math.exp(-scale * (t1Score - t2Score)));
    }

    /**
     * Displays the winner of a match based on the win probability of the teams.
     *
     * @param t1 The first team.
     * @param t2 The second team.
     */
    public static void displayWinnerFromProbability(@NotNull Team t1, @NotNull Team t2) {
        double winProbability = calculateWinProbability(t1, t2);
        String t1Name = t1.getName();
        String t2Name = t2.getName();

        if (winProbability >= 0.5) {
            System.out.println(t1Name + " has a " + (winProbability * 100) + "% chance of winning against " + t2Name);
        } else {
            System.out.println(t2Name + " has a " + ((1 - winProbability) * 100) + "% chance of winning against " + t1Name);
        }
    }
}
