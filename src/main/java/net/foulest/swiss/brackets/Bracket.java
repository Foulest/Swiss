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
     * Print the match result with a win probability.
     *
     * @param winner         The winning team.
     * @param records        The records of each team.
     * @param loser          The losing team.
     * @param winProbability The win probability of the match for Team 1.
     * @param team1          The first team.
     */
    default void printMatchResult(@NotNull Team winner,
                                  @NotNull Map<Team, int[]> records,
                                  @NotNull Team loser,
                                  double winProbability,
                                  @NotNull Team team1) {
        String winnerName = winner.getName();
        String loserName = loser.getName();

        int[] winnerRecords = records.get(winner);
        int[] loserRecords = records.get(loser);

        // Flip the win probability if team 1 isn't the winner
        // This is needed because the win probability is always calculated for team 1
        if (!winner.equals(team1)) {
            winProbability = Math.abs(1 - winProbability);
        }

        System.out.println(winnerName + " (" + winnerRecords[0] + "-" + winnerRecords[1] + ")"
                + " beat " + loserName + " (" + loserRecords[0] + "-" + loserRecords[1] + ")"
                + " with a " + winProbability + "% win probability");
    }

    /**
     * Append a probability to the result string if it exists.
     *
     * @param resultString   The result string to append to.
     * @param record         The record to check for.
     * @param recordCounts   The record counts for the team.
     * @param numSimulations The number of simulations.
     */
    static void appendProbability(StringBuilder resultString, String record,
                                  @NotNull Map<String, Integer> recordCounts,
                                  int numSimulations) {
        if (recordCounts.containsKey(record)) {
            double probability = recordCounts.get(record) * 100.0 / numSimulations;
            resultString.append(String.format("\t[%s] %.2f%%", record, probability));
        }
    }

    /**
     * Print the header for the results.
     *
     * @param numSimulations The number of simulations.
     * @param startingTime   The starting time of the simulations.
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
     * @param winner  The winning team.
     * @param loser   The losing team.
     */
    static void updateRecords(@NotNull Map<Team, int[]> records, Team winner, Team loser) {
        records.get(winner)[0] += 1;
        records.get(loser)[1] += 1;
    }
}
