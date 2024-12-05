/*
 * Swiss - a Monte Carlo bracket simulator for the CS2 Major.
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
package net.foulest.swiss;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

@Data
@AllArgsConstructor
class Match {

    private Team team1;
    private Team team2;
    private boolean bestOfThree;

    Team simulate() {
        int team1Wins = 0;
        int team2Wins = 0;
        double winProbability = calculateWinProbability(team1, team2);

        // Generate random numbers in bulk for up to 5 rounds
        double[] randomNumbers = new Random().doubles(5).toArray();
        int round = 0;
        int maxRounds = bestOfThree ? 3 : 1;

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

    private static double calculateWinProbability(@NotNull Team t1, @NotNull Team t2) {
        final double W1_DIV_MAX_RANK = 0.20 / 100.0; // Precompute constant
        final double W2 = 0.80;
        final double SCALE = 5.0;

        // Compute scores
        double t1Score = W1_DIV_MAX_RANK * (100.0 - t1.getWorldRanking()) + W2 * t1.getAvgPlayerRating();
        double t2Score = W1_DIV_MAX_RANK * (100.0 - t2.getWorldRanking()) + W2 * t2.getAvgPlayerRating();

        // Calculate win probability
        return 1.0 / (1.0 + Math.exp(-SCALE * (t1Score - t2Score)));
    }

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
