package net.foulest.swiss.brackets;

import net.foulest.swiss.match.Match;
import net.foulest.swiss.team.Team;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface Bracket {

    List<Team> getTeams();

    long getStartingTime();

    /**
     * Update the ratings for both teams based on the match outcome.
     *
     * @param team1 The first team.
     * @param team2 The second team.
     * @param winner The winning team.
     */
    default void updateTeamRatings(Team team1, Team team2, Team winner, boolean bestOfThree) {
        // Calculate win probability for dynamic rating adjustment
        double winProbability = Match.calculateWinProbability(team1, team2, bestOfThree);
        double actualOutcome = (winner == team1) ? 1 : 0; // 1 if team1 wins, 0 if team2 wins

        // Randomly generate the K-factor between 0.003 and 0.007
        double K = 0.003 + Math.random() * 0.004;

        // Adjust ratings for both teams
        double team1Expected = winProbability;  // Expected score for team1
        double team2Expected = 1 - winProbability; // Expected score for team2

        // Update ratings using Elo-like formula
        team1.setAvgPlayerRating(team1.getAvgPlayerRating() + K * (actualOutcome - team1Expected));
        team2.setAvgPlayerRating(team2.getAvgPlayerRating() + K * ((1 - actualOutcome) - team2Expected));
    }

    /**
     * Print the match result.
     *
     * @param winner  The winning team.
     * @param records The records of each team.
     * @param loser   The losing team.
     */
    default void printMatchResult(@NotNull Team winner,
                                  @NotNull Map<Team, int[]> records,
                                  @NotNull Team loser) {
        String winnerName = winner.getName();
        String loserName = loser.getName();

        int[] winnerRecords = records.get(winner);
        int[] loserRecords = records.get(loser);

        System.out.println(winnerName + " (" + winnerRecords[0] + "-" + winnerRecords[1] + ")"
                + " beat " + loserName + " (" + loserRecords[0] + "-" + loserRecords[1] + ")");
    }

    /**
     * Append a probability to the result string if it exists.
     *
     * @param resultString The result string to append to.
     * @param record The record to check for.
     * @param recordCounts The record counts for the team.
     * @param numSimulations The number of simulations.
     */
    static void appendProbability(StringBuilder resultString, String record,
                                  @NotNull Map<String, Integer> recordCounts,
                                  int numSimulations) {
        if (recordCounts.containsKey(record)) {
            double probability = recordCounts.get(record) * 100.0 / numSimulations;
            resultString.append(String.format("%s (%.2f%%) ", record, probability));
        }
    }

    /**
     * Print the header for the results.
     *
     * @param numSimulations The number of simulations.
     * @param startingTime The starting time of the simulations.
     */
    static void printHeader(int numSimulations, long startingTime) {
        long duration = System.currentTimeMillis() - startingTime;
        double seconds = duration / 1000.0;

        System.out.println();
        System.out.println("Results after " + numSimulations + " simulations (took " + seconds + " seconds):");
        System.out.println();
        System.out.println("Individual Team Results:");
    }

    /**
     * Update the records for each team.
     *
     * @param records The records of each team.
     * @param winner The winning team.
     * @param loser The losing team.
     */
    static void updateRecords(@NotNull Map<Team, int[]> records, Team winner, Team loser) {
        records.get(winner)[0] += 1;
        records.get(loser)[1] += 1;
    }
}
