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
 *
 * @author Foulest
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
        double winProbability = calculateWinProbability(team1, team2, maxRounds == 3);

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
     * <p>
     * If the match is the best of one:
     * - 30% of the win probability is based on the team's world ranking.
     * - 70% of the win probability is based on the team's KDR.
     * <p>
     * If the match is the best of three:
     * - 40% of the win probability is based on the team's world ranking.
     * - 60% of the win probability is based on the team's KDR.
     *
     * @param t1 The first team.
     * @param t2 The second team.
     * @return The win probability of the first team.
     */
    public static double calculateWinProbability(@NotNull Team t1, @NotNull Team t2, boolean bestOfThree) {
        double worldRankingWeight = bestOfThree ? 0.70 : 0.60;
        double kdrWeight = bestOfThree ? 0.30 : 0.40;
        double scale = 5.0;

        // Compute scores
        double t1Score = (worldRankingWeight / 100.0) * (100.0 - t1.getWorldRanking()) + kdrWeight * t1.getKdr();
        double t2Score = (worldRankingWeight / 100.0) * (100.0 - t2.getWorldRanking()) + kdrWeight * t2.getKdr();

        // Calculate win probability
        return (1.0 / (1.0 + Math.exp(-scale * (t1Score - t2Score))));
    }

    /**
     * Displays the winner of a match based on the win probability of the teams.
     *
     * @param t1 The first team.
     * @param t2 The second team.
     */
    public static void displayWinnerFromProbability(@NotNull Team t1, @NotNull Team t2, boolean bestOfThree) {
        double t1WinProbability = calculateWinProbability(t1, t2, bestOfThree);
        double t2WinProbability = 1 - t1WinProbability;

        String t1Name = t1.getName();
        String t2Name = t2.getName();

        if (t1WinProbability >= 0.5) {
            System.out.println(t1Name + " has a " + (t1WinProbability * 100) + "% chance of winning against " + t2Name);
        } else {
            System.out.println(t2Name + " has a " + ((1 - t1WinProbability) * 100) + "% chance of winning against " + t1Name);
        }

        // Calculate win probability for dynamic rating adjustment
        double actualOutcome = (t1WinProbability >= 0.5) ? 1 : 0; // 1 if team1 wins, 0 if team2 wins

        // Randomly generate the K-factor between 0.003 and 0.007
        double K = 0.003 + Math.random() * 0.004;

        // Get KDRs
        double t1KDR = t1.getKdr();
        double t2KDR = t2.getKdr();

        // Print ratings using Elo-like formula
        System.out.println(t1Name + "'s rating would change from " + t1KDR + " to "
                + (t1KDR + K * (actualOutcome - t1WinProbability)));
        System.out.println(t2Name + "'s rating would change from " + t2KDR + " to "
                + (t2KDR + K * ((1 - actualOutcome) - t2WinProbability)));
    }
}
