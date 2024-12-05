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
package net.foulest.swiss.brackets;

import lombok.Data;
import net.foulest.swiss.match.Match;
import net.foulest.swiss.team.Team;
import net.foulest.swiss.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents a bracket for the Challengers & Legends stages of the CS2 Major.
 * <p>
 * In this bracket format, 16 teams play against each other in a Swiss format.
 * Every match is a BO1 until a team has a chance to be eliminated, at which point it becomes a BO3.
 * The top 8 teams advance to the Champions stage, while the bottom 8 teams are eliminated.
 */
@Data
public class StandardBracket {

    private List<Team> teams;
    private long startingTime;

    // Pairwise results for 3-0 and 0-3 teams
    private static Map<Pair<Team, Team>, Integer> pairwise3_0 = new HashMap<>();
    private static Map<Pair<Team, Team>, Integer> pairwise0_3 = new HashMap<>();

    public StandardBracket(@NotNull List<Team> teams) {
        this.teams = teams;
        startingTime = System.currentTimeMillis();
    }

    /**
     * Simulates multiple brackets.
     *
     * @param numSimulations The number of simulations to run.
     */
    @SuppressWarnings("NestedMethodCall")
    public void simulateMultipleBrackets(int numSimulations) {
        // Map to track results for each team
        Map<Team, Map<String, Integer>> results = new ConcurrentHashMap<>();

        // Initialize results map for each team
        teams.forEach(team -> results.put(team, new ConcurrentHashMap<>()));

        // ExecutorService to manage threads
        ExecutorService executor = Executors.newWorkStealingPool();

        // Create tasks for simulations
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numSimulations; i++) {
            tasks.add(() -> {
                simulateBracket(results); // Simulate one bracket
                return null;
            });
        }

        try {
            // Execute all tasks and wait for completion
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Simulation interrupted: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        // Analyze and print the results after all simulations are complete
        printResults(results, numSimulations, startingTime);
    }

    /**
     * Simulate a single bracket.
     *
     * @param results The results of the simulations.
     */
    @SuppressWarnings("NestedMethodCall")
    private void simulateBracket(Map<Team, Map<String, Integer>> results) {
        Map<Team, int[]> records = new HashMap<>();
        Map<Team, Integer> buchholzScores = new HashMap<>();
        Map<Team, List<Team>> pastOpponents = new HashMap<>();

        // Initialize all the records to 0-0
        for (Team team : teams) {
            records.put(team, new int[]{0, 0});
            buchholzScores.put(team, 0);
            pastOpponents.put(team, new ArrayList<>());
        }

        // Sort teams by their seeding in ascending order
        List<Team> seededTeams = new ArrayList<>(teams);
        seededTeams.sort(Comparator.comparingInt(Team::getSeeding));

        List<Team> activeTeams = new ArrayList<>();

        // Generate first-round matchups based on seeding
        for (int i = 0; i < seededTeams.size() / 2; i++) {
            Team team1 = seededTeams.get(i); // Top seed
            Team team2 = seededTeams.get(seededTeams.size() / 2 + i); // Bottom seed
            Match match = new Match(team1, team2, 1);

            Team winner = match.simulate(false);
            Team loser = (winner == team1) ? team2 : team1;

            // Update records
            updateRecords(records, winner, loser);

//            // Print the match result
//            System.out.println(winner.getName() + " (" + records.get(winner)[0] + "-" + records.get(winner)[1] + ")" +
//                    " beat " + loser.getName() + " (" + records.get(loser)[0] + "-" + records.get(loser)[1] + ")");

            // Update past opponents
            updatePastOpponents(pastOpponents, winner, loser);

            // Add only teams that haven't been eliminated
            if (records.get(winner)[1] < 3) {
                activeTeams.add(winner);
            }
            if (records.get(loser)[1] < 3) {
                activeTeams.add(loser);
            }
        }

        // Calculate Buchholz scores at the end of the group stage
        calculateBuchholz(activeTeams, pastOpponents, records, buchholzScores);

        // Proceed to simulate remaining rounds
        while (!allTeamsDecided(activeTeams, records)) {
            Map<String, List<Team>> groups = new HashMap<>();

            // Group teams by their current record (e.g., "2-0", "1-1")
            for (Team team : activeTeams) {
                int[] record = records.get(team);
                String key = record[0] + "-" + record[1];
                groups.putIfAbsent(key, new ArrayList<>());
                groups.get(key).add(team);
            }

            List<Team> nextRoundTeams = new ArrayList<>();
            Map<Team, Integer> currentStandings = new HashMap<>();

            // Find the current round by taking the first team in the activeTeams and adding its record
            int currentRound = records.get(activeTeams.get(0))[0] + records.get(activeTeams.get(0))[1] + 1;

            // Calculate the current standings for the current round
            calculateCurrentStandings(currentRound, records, currentStandings, buchholzScores);

            // Sort the current standings by their standing
            currentStandings = new LinkedHashMap<>(currentStandings);
            currentStandings = currentStandings.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);

//            // Print the current standings (sorted)
//            System.out.println();
//            System.out.println("Round: " + currentRound);
//            System.out.println("Current Standings:");
//            for (Map.Entry<Team, Integer> entry : currentStandings.entrySet()) {
//                Team team = entry.getKey();
//                int standing = entry.getValue();
//
//                System.out.println(standing + ". " + team.getName() + " (" + records.get(team)[0] + "-" + records.get(team)[1] + ")");
//            }

            // Process each group separately
            for (Map.Entry<String, List<Team>> entry : groups.entrySet()) {
                List<Team> group = entry.getValue();

                // Sort teams by their standing
                group.sort(Comparator.comparingInt(currentStandings::get));

                List<Match> matches = new ArrayList<>();
                List<Team> availableTeams = new ArrayList<>(group);

                // Create the matchups for the group
                for (Team team : group) {
                    // If the team has already played all of its games, skip it
                    if (records.get(team)[0] == 3 || records.get(team)[1] == 3) {
                        availableTeams.remove(team);
                        continue;
                    }

                    // Continue if the team isn't in the list of available teams
                    if (!availableTeams.contains(team)) {
                        continue;
                    }

//                    System.out.println();
//                    System.out.println("List of opponents for " + team.getName()
//                            + " (" + records.get(team)[0] + "-" + records.get(team)[1] + ")"
//                            + " (Seed: " + team.getSeeding() + ")"
//                            + " (Standing: " + currentStandings.get(team) + ")"
//                            + " (Buchholz: " + buchholzScores.get(team) + "):"
//                    );

                    // Remove the team from the list of available teams
                    availableTeams.remove(team);

                    List<Team> uniqueOpponents = new ArrayList<>();

                    for (Team opponent : availableTeams) {
                        if (!pastOpponents.get(team).contains(opponent)) {
                            uniqueOpponents.add(opponent);
                        }
                    }

                    List<Team> differentBuchholz = new ArrayList<>();
                    List<Team> sameBuchholz = new ArrayList<>();

                    for (Team unique : uniqueOpponents) {
                        if (buchholzScores.get(unique).equals(buchholzScores.get(team))) {
                            sameBuchholz.add(unique);
                        } else {
                            differentBuchholz.add(unique);
                        }
                    }

                    Team idealMatchup = null;
                    Team highestStandingDifferentBuchholz = null;
                    Team highestStandingSameBuchholz = null;

                    for (Team other : differentBuchholz) {
                        if (highestStandingDifferentBuchholz != null) {
                            int currentDifference = Math.abs(buchholzScores.get(other) - buchholzScores.get(team));
                            int highestDifference = Math.abs(buchholzScores.get(highestStandingDifferentBuchholz) - buchholzScores.get(team));

                            if (currentDifference > highestDifference) {
                                highestStandingDifferentBuchholz = other;
                            } else if (currentDifference == highestDifference) {
                                // If the differences are the same, fall back to comparing standings
                                if (currentStandings.get(other) > currentStandings.get(highestStandingDifferentBuchholz)) {
                                    highestStandingDifferentBuchholz = other;
                                }
                            }
                        } else {
                            highestStandingDifferentBuchholz = other;
                        }

//                        System.out.println(" + " + other.getName() + " (" + records.get(other)[0] + "-" + records.get(other)[1] + ")"
//                                + " (Seed: " + other.getSeeding() + ")"
//                                + " (Standing: " + currentStandings.get(other) + ")"
//                                + " (Buchholz: " + buchholzScores.get(other) + ")");
                    }

                    for (Team same : sameBuchholz) {
                        if (highestStandingSameBuchholz == null || currentStandings.get(same) > currentStandings.get(highestStandingSameBuchholz)) {
                            highestStandingSameBuchholz = same;
                        }

//                        System.out.println(" - " + same.getName() + " (" + records.get(same)[0] + "-" + records.get(same)[1] + ")"
//                                + " (Seed: " + same.getSeeding() + ")"
//                                + " (Standing: " + currentStandings.get(same) + ")"
//                                + " (Buchholz: " + buchholzScores.get(same) + ")");
                    }

                    // Choose the highest standing different Buchholz team
                    // If that's null, choose the highest standing same Buchholz team
                    // If that's null, output debug information
                    if (highestStandingDifferentBuchholz != null) {
//                        System.out.println("Ideal matchup for " + team.getName() + " is " + highestStandingDifferentBuchholz.getName());
                        idealMatchup = highestStandingDifferentBuchholz;
                    } else if (highestStandingSameBuchholz != null) {
//                        System.out.println("Ideal matchup for " + team.getName() + " is " + highestStandingSameBuchholz.getName());
                        idealMatchup = highestStandingSameBuchholz;
                    } else {
                        for (Team available : availableTeams) {
                            if (availableTeams.size() == 1) {
//                                System.out.println("Ideal matchup for " + team.getName() + " is " + available.getName());
                                idealMatchup = available;
                                break;
                            } else {
                                if (idealMatchup == null || currentStandings.get(available) > currentStandings.get(idealMatchup)) {
                                    idealMatchup = available;
                                }
                            }
                        }
                    }

                    // Determine if the matchup should be best-of-three
                    boolean bestOfThree = records.get(team)[0] == 2 || records.get(team)[1] == 2;

                    // Create the match if an ideal matchup exists
                    if (idealMatchup != null) {
                        Match match = new Match(team, idealMatchup, bestOfThree ? 3 : 1);
                        matches.add(match);
                        availableTeams.remove(idealMatchup);
                    } else {
                        System.err.println("No ideal matchup found for: " + team.getName());
                    }
                }

                // Print all matches information
                for (Match match : matches) {
                    Team team1 = match.getTeam1();
                    Team team2 = match.getTeam2();

//                    System.out.println("Match: " + team1.getName() + " (" + records.get(team1)[0] + "-" + records.get(team1)[1] + ")"
//                            + " (Buchholz: " + buchholzScores.get(team1) + ")"
//                            + " (Seed: " + team1.getSeeding() + ")"
//                            + " (Standing: " + currentStandings.get(team1) + ")"
//                            + " vs " + team2.getName() + " (" + records.get(team2)[0] + "-" + records.get(team2)[1] + ")"
//                            + " (Buchholz: " + buchholzScores.get(team2) + ")"
//                            + " (Standing: " + currentStandings.get(team2) + ")"
//                            + " (Seed: " + team2.getSeeding() + ")"
//                    );

                    // Simulate the match
                    Team winner = match.simulate(false);
                    Team loser = (winner == team1) ? team2 : team1;

                    // Update records
                    updateRecords(records, winner, loser);

//                    // Print the match result
//                    printMatchResult(winner, records, loser);

                    // Update past opponents
                    updatePastOpponents(pastOpponents, winner, loser);

                    // Add only teams that haven't been eliminated
                    if (records.get(winner)[1] < 3) {
                        nextRoundTeams.add(winner);
                    }
                    if (records.get(loser)[1] < 3) {
                        nextRoundTeams.add(loser);
                    }
                }

                // If there's an odd number of teams, the middle-ranked team gets a bye
                if (group.size() % 2 == 1) {
                    Team byeTeam = group.get(group.size() / 2);
                    nextRoundTeams.add(byeTeam);
                }
            }

            // Update the list of active teams for the next round
            activeTeams = nextRoundTeams;

            // Calculate buchholz scores for the next round
            calculateBuchholz(activeTeams, pastOpponents, records, buchholzScores);
        }

        List<Team> threeZeroTeams = new ArrayList<>();
        List<Team> zeroThreeTeams = new ArrayList<>();

        // Record final results
        for (Map.Entry<Team, int[]> entry : records.entrySet()) {
            Team team = entry.getKey();
            int[] record = entry.getValue();
            String result = record[0] + "-" + record[1]; // Format: "XW-XL"

            // Record the number of 3-0 and 0-3 teams
            if (record[0] == 3 && record[1] == 0) {
                threeZeroTeams.add(team);
            }
            if (record[1] == 3 && record[0] == 0) {
                zeroThreeTeams.add(team);
            }

            // Record the individual team's result (if needed)
            if (record[0] <= 3) {
                results.get(team).put(result, results.get(team).getOrDefault(result, 0) + 1);
            }
        }

        // Record pairwise results for 3-0
        pairwise3_0.put(new Pair<>(threeZeroTeams.get(0), threeZeroTeams.get(1)),
                pairwise3_0.getOrDefault(new Pair<>(threeZeroTeams.get(0), threeZeroTeams.get(1)), 0) + 1);

        // Record pairwise results for 0-3
        pairwise0_3.put(new Pair<>(zeroThreeTeams.get(0), zeroThreeTeams.get(1)),
                pairwise0_3.getOrDefault(new Pair<>(zeroThreeTeams.get(0), zeroThreeTeams.get(1)), 0) + 1);
    }

    /**
     * Calculate current standings based on the seeding of the teams in the following group order:
     * (If no teams are in the group, skip to the next group)
     * <p>
     * First Round:
     * - 0-0
     * <p>
     * Second Round:
     * - 1-0
     * - 0-1
     * <p>
     * Third Round:
     * - 2-0
     * - 1-1
     * - 0-2
     * <p>
     * Fourth Round:
     * - 3-0
     * - 2-1
     * - 1-2
     * - 0-3
     * <p>
     * Fifth Round:
     * - 3-0
     * - 3-1
     * - 2-2
     * - 1-3
     * - 0-3
     * <p>
     * Sixth Round:
     * - 3-0
     * - 3-1
     * - 3-2
     * - 2-3
     * - 1-3
     * - 0-3
     *
     * @param currentRound The current round of the group stage
     * @param records The records of each team
     * @param currentStandings The current standings of each team
     */
    private static void calculateCurrentStandings(int currentRound, Map<Team, int[]> records,
                                                  Map<Team, Integer> currentStandings,
                                                  Map<Team, Integer> buchholzScores) {
        List<List<Team>> groups = new ArrayList<>();

        // Define group conditions for each round
        switch (currentRound) {
            case 1:
                groups.add(getTeamsByRecord(records, 0, 0));
                break;

            case 2:
                groups.add(getTeamsByRecord(records, 1, 0));
                groups.add(getTeamsByRecord(records, 0, 1));
                break;

            case 3:
                groups.add(getTeamsByRecord(records, 2, 0));
                groups.add(getTeamsByRecord(records, 1, 1));
                groups.add(getTeamsByRecord(records, 0, 2));
                break;

            case 4:
                groups.add(getTeamsByRecord(records, 3, 0));
                groups.add(getTeamsByRecord(records, 2, 1));
                groups.add(getTeamsByRecord(records, 1, 2));
                groups.add(getTeamsByRecord(records, 0, 3));
                break;

            case 5:
                groups.add(getTeamsByRecord(records, 3, 0));
                groups.add(getTeamsByRecord(records, 3, 1));
                groups.add(getTeamsByRecord(records, 2, 2));
                groups.add(getTeamsByRecord(records, 1, 3));
                groups.add(getTeamsByRecord(records, 0, 3));
                break;

            case 6:
                groups.add(getTeamsByRecord(records, 3, 0));
                groups.add(getTeamsByRecord(records, 3, 1));
                groups.add(getTeamsByRecord(records, 3, 2));
                groups.add(getTeamsByRecord(records, 2, 3));
                groups.add(getTeamsByRecord(records, 1, 3));
                groups.add(getTeamsByRecord(records, 0, 3));
                break;

            default:
                throw new IllegalArgumentException("Unsupported round: " + currentRound);
        }

        int standing = 0;

        // Process each group
        for (List<Team> group : groups) {
            sortTeams(group, buchholzScores);

            for (Team team : group) {
                standing++;
                currentStandings.put(team, standing);
            }
        }
    }

    /**
     * Get the teams with a specific record.
     *
     * @param records The records of each team.
     * @param wins The number of wins.
     * @param losses The number of losses.
     * @return The list of teams with the specified record.
     */
    private static @NotNull List<Team> getTeamsByRecord(@NotNull Map<Team, int[]> records,
                                                        int wins, int losses) {
        List<Team> group = new ArrayList<>();

        records.forEach((team, record) -> {
            if (record[0] == wins && record[1] == losses) {
                group.add(team);
            }
        });
        return group;
    }

    /**
     * Sort the teams in the group by seeding first, then by Buchholz score, then by seeding.
     *
     * @param group The group of teams.
     * @param buchholzScores The Buchholz score for each team.
     */
    private static void sortTeams(@NotNull List<Team> group, Map<Team, Integer> buchholzScores) {
        // Sort the teams by seeding first
        group.sort(Comparator.comparingInt(Team::getSeeding));

        // Sort the teams by Buchholz score, then by seeding
        group.sort((team1, team2) -> {
            int buchholzComparison = Integer.compare(buchholzScores.get(team2), buchholzScores.get(team1));

            if (buchholzComparison != 0) {
                return buchholzComparison;
            }
            return Integer.compare(team1.getSeeding(), team2.getSeeding());
        });
    }

    /**
     * Print the match result.
     *
     * @param winner  The winning team.
     * @param records The records of each team.
     * @param loser   The losing team.
     */
    private static void printMatchResult(@NotNull Team winner,
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
     * Update the records for each team.
     *
     * @param records The records of each team.
     * @param winner The winning team.
     * @param loser The losing team.
     */
    private static void updateRecords(@NotNull Map<Team, int[]> records, Team winner, Team loser) {
        records.get(winner)[0] += 1;
        records.get(loser)[1] += 1;
    }

    /**
     * Update the past opponents for each team.
     *
     * @param pastOpponents The past opponents for each team.
     * @param winner The winning team.
     * @param loser The losing team.
     */
    private static void updatePastOpponents(@NotNull Map<Team, List<Team>> pastOpponents, Team winner, Team loser) {
        pastOpponents.get(winner).add(loser);
        pastOpponents.get(loser).add(winner);
    }

    /**
     * Calculate the Buchholz score for each team.
     *
     * @param activeTeams The list of active teams.
     * @param pastOpponents The past opponents for each team.
     * @param records The records of each team.
     * @param buchholzScores The Buchholz score for each team.
     */
    private static void calculateBuchholz(@NotNull List<Team> activeTeams,
                                          Map<Team, List<Team>> pastOpponents,
                                          Map<Team, int[]> records,
                                          Map<Team, Integer> buchholzScores) {
        // Calculate buchholz scores for the first round
        for (Team team : activeTeams) {
            int buchholz = 0;

            // Combine the buchholz score of the team's past opponents
            for (Team opponent : pastOpponents.get(team)) {
                buchholz += records.get(opponent)[0] - records.get(opponent)[1];
            }

            // Set the buchholz score for the team
            buchholzScores.put(team, buchholz);
        }
    }

    /**
     * Check if all teams have been decided.
     *
     * @param activeTeams The list of active teams.
     * @param records The records of each team.
     * @return True if all teams have either 3 wins or 3 losses, false otherwise.
     */
    private static boolean allTeamsDecided(@NotNull List<Team> activeTeams, Map<Team, int[]> records) {
        for (Team team : activeTeams) {
            int[] record = records.get(team);

            if (record[0] < 3 && record[1] < 3) {
                return false; // At least one team still has games to play
            }
        }
        return true; // All teams have either 3 wins or 3 losses
    }

    /**
     * Print the results of the simulations.
     *
     * @param results         The results of the simulations.
     * @param numSimulations  The number of simulations.
     */
    private static void printResults(@NotNull Map<Team, Map<String, Integer>> results,
                                     int numSimulations, long startingTime) {
        long duration = System.currentTimeMillis() - startingTime;
        double seconds = duration / 1000.0;

        System.out.println();
        System.out.println("Results after " + numSimulations + " simulations (took " + seconds + " seconds):");
        System.out.println();
        System.out.println("Individual Team Results:");

        for (Map.Entry<Team, Map<String, Integer>> entry : results.entrySet()) {
            Team team = entry.getKey();
            Map<String, Integer> recordCounts = entry.getValue();

            // Calculate combined probabilities
            double probability3X = (recordCounts.getOrDefault("3-1", 0)
                    + recordCounts.getOrDefault("3-2", 0)) * 100.0 / numSimulations;
            double probabilityX3 = (recordCounts.getOrDefault("1-3", 0)
                    + recordCounts.getOrDefault("2-3", 0)) * 100.0 / numSimulations;

            // Build the result string
            String teamName = team.getName();
            StringBuilder resultString = new StringBuilder(String.format("Team: %s | 3-X (%.2f%%) X-3 (%.2f%%) ",
                    teamName, probability3X, probabilityX3));

            // Add individual probabilities
            appendProbability(resultString, "3-0", recordCounts, numSimulations);
            appendProbability(resultString, "0-3", recordCounts, numSimulations);

            // Print the team's result
            System.out.println(resultString.toString().trim());
        }

        System.out.println();

        // Print most common pairs for 3-0 and 0-3
        printMostCommonPair("3-0", pairwise3_0, numSimulations);
        printMostCommonPair("0-3", pairwise0_3, numSimulations);
    }

    /**
     * Append a probability to the result string if it exists.
     *
     * @param resultString The result string to append to.
     * @param record The record to check for.
     * @param recordCounts The record counts for the team.
     * @param numSimulations The number of simulations.
     */
    private static void appendProbability(StringBuilder resultString, String record,
                                          @NotNull Map<String, Integer> recordCounts,
                                          int numSimulations) {
        if (recordCounts.containsKey(record)) {
            double probability = recordCounts.get(record) * 100.0 / numSimulations;
            resultString.append(String.format("%s (%.2f%%) ", record, probability));
        }
    }

    /**
     * Find and print the most common pair for a given record type.
     *
     * @param label The label for the record type. (e.g., "3-0" or "0-3")
     * @param pairResults The results for the pair.
     * @param numSimulations The number of simulations.
     */
    private static void printMostCommonPair(String label,
                                            @NotNull Map<Pair<Team, Team>, Integer> pairResults,
                                            int numSimulations) {
        Pair<Team, Team> mostCommonPair = null;
        int maxCount = 0;

        for (Map.Entry<Pair<Team, Team>, Integer> entry : pairResults.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommonPair = entry.getKey();
            }
        }

        if (mostCommonPair != null) {
            System.out.printf("Most Common %s Pair: %s and %s (%d times / %d simulations)%n",
                    label,
                    mostCommonPair.getFirst().getName(),
                    mostCommonPair.getLast().getName(),
                    maxCount,
                    numSimulations);
        }
    }
}
