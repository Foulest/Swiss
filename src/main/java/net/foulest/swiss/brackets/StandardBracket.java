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

import lombok.Cleanup;
import lombok.Data;
import net.foulest.swiss.match.Match;
import net.foulest.swiss.team.Team;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Represents a bracket for the Challengers & Legends stages of the CS2 Major.
 * <p>
 * In this bracket format, 16 teams play against each other in a Swiss format.
 * Every match is a BO1 until a team has a chance to be eliminated, at which point it becomes a BO3.
 * The top 8 teams advance to the Champions stage, while the bottom 8 teams are eliminated.
 *
 * @author Foulest
 */
@Data
public class StandardBracket implements Bracket {

    private List<Team> teams;
    private long startingTime;

    // For special conditions, like a team reaching a certain round
    int specialReached;
    int specialSuccess;

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

        // Map to track head-to-head counts
        Map<Team, Map<Team, Integer>> headToHead = new ConcurrentHashMap<>();
        teams.forEach(team -> {
            Map<Team, Integer> counts = new ConcurrentHashMap<>();
            for (Team opp : teams) {
                if (!team.equals(opp)) {
                    counts.put(opp, 0);
                }
            }
            headToHead.put(team, counts);
        });

        // ExecutorService to manage threads
        @Cleanup ExecutorService executor = Executors.newWorkStealingPool();

        // Create tasks for simulations
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numSimulations; i++) {
            tasks.add(() -> {
                simulateBracket(results, headToHead); // Simulate one bracket
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
        printResults(results, numSimulations, startingTime, specialReached, specialSuccess);
    }

    /**
     * Simulate a single bracket.
     *
     * @param results The results of the simulations.
     */
    @SuppressWarnings("NestedMethodCall")
    private void simulateBracket(Map<Team, Map<String, Integer>> results, Map<Team, Map<Team, Integer>> headToHead) {
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
        seededTeams.sort(Comparator.comparingInt(Team::getSeed));

        List<Team> activeTeams = seededTeams.stream()
                .map(Team::clone) // Clone or copy each team
                .collect(Collectors.toList());

        // Generate first-round matchups based on seeding
        for (int i = 0; i < activeTeams.size() / 2; i++) {
            Team team1 = activeTeams.get(i); // Top seed
            Team team2 = activeTeams.get(activeTeams.size() / 2 + i); // Bottom seed

            // Create a match between the two teams
            Match match = new Match(team1, team2, 1);

            // Simulate the match
            Team winner = match.simulate(false);
            Team loser = (winner == team1) ? team2 : team1;

            // Update records
            Bracket.updateRecords(records, winner, loser);

            // Update past opponents
            updatePastOpponents(pastOpponents, winner, loser);
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
            int currentRound = records.get(activeTeams.getFirst())[0] + records.get(activeTeams.getFirst())[1] + 1;

            // Calculate the current standings for the current round
            calculateCurrentStandings(currentRound, records, currentStandings, buchholzScores);

            // Sort the current standings by their standing
            currentStandings = new LinkedHashMap<>(currentStandings);
            currentStandings = currentStandings.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);

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
                    }

                    for (Team same : sameBuchholz) {
                        if (highestStandingSameBuchholz == null || currentStandings.get(same) > currentStandings.get(highestStandingSameBuchholz)) {
                            highestStandingSameBuchholz = same;
                        }
                    }

                    // Choose the highest standing different Buchholz team
                    // If that's null, choose the highest standing same Buchholz team
                    // If that's null, output debug information
                    if (highestStandingDifferentBuchholz != null) {
                        idealMatchup = highestStandingDifferentBuchholz;
                    } else if (highestStandingSameBuchholz != null) {
                        idealMatchup = highestStandingSameBuchholz;
                    } else {
                        for (Team available : availableTeams) {
                            if (availableTeams.size() == 1) {
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

                // Simulate the matches in the group
                for (Match match : matches) {
                    Team team1 = match.getTeam1();
                    Team team2 = match.getTeam2();

                    // Simulate the match
                    Team winner = match.simulate(false);
                    Team loser = (winner == team1) ? team2 : team1;

                    // Update records
                    Bracket.updateRecords(records, winner, loser);

                    // Update past opponents
                    updatePastOpponents(pastOpponents, winner, loser);

                    // Update head-to-head counts
                    headToHead.get(team1).computeIfPresent(team2, (k, v) -> v + 1);
                    headToHead.get(team2).computeIfPresent(team1, (k, v) -> v + 1);

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

        // Record final results
        for (Map.Entry<Team, int[]> entry : records.entrySet()) {
            Team team = entry.getKey();
            int[] record = entry.getValue();
            String result = record[0] + "-" + record[1]; // Format: "XW-XL"

            // Record the individual team's result (if needed)
            if (record[0] <= 3) {
                results.get(team).put(result, results.get(team).getOrDefault(result, 0) + 1);
            }
        }
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
     * @param currentRound     The current round of the group stage
     * @param records          The records of each team
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
     * @param wins    The number of wins.
     * @param losses  The number of losses.
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
     * Print head-to-head encounter percentages for each team.
     */
    private static void printHeadToHead(@NotNull Map<Team, Map<Team, Integer>> headToHead) {
        System.out.println();
        headToHead.forEach((team, opponents) -> {
            int totalMatches = opponents.values().stream().mapToInt(Integer::intValue).sum();
            System.out.println(team.getName() + "'s most faced opponents:");
            opponents.entrySet().stream()
                    .sorted(Map.Entry.<Team, Integer>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> {
                        double pct = entry.getValue() * 100.0 / totalMatches;
                        System.out.printf("- %s (%.2f%%)%n", entry.getKey().getName(), pct);
                    });
            System.out.println();
        });
    }

    /**
     * Sort the teams in the group by seeding first, then by Buchholz score, then by seeding.
     *
     * @param group          The group of teams.
     * @param buchholzScores The Buchholz score for each team.
     */
    private static void sortTeams(@NotNull List<Team> group, Map<Team, Integer> buchholzScores) {
        // Sort the teams by seeding first
        group.sort(Comparator.comparingInt(Team::getSeed));

        // Sort the teams by Buchholz score, then by seeding
        group.sort((team1, team2) -> {
            Integer t2Score = buchholzScores.get(team2);
            Integer t1Score = buchholzScores.get(team1);
            int buchholzComparison = Integer.compare(t2Score, t1Score);

            if (buchholzComparison != 0) {
                return buchholzComparison;
            }

            int t1Seeding = team1.getSeed();
            int t2Seeding = team2.getSeed();
            return Integer.compare(t1Seeding, t2Seeding);
        });
    }

    /**
     * Update the past opponents for each team.
     *
     * @param pastOpponents The past opponents for each team.
     * @param winner        The winning team.
     * @param loser         The losing team.
     */
    private static void updatePastOpponents(@NotNull Map<Team, List<Team>> pastOpponents, Team winner, Team loser) {
        pastOpponents.get(winner).add(loser);
        pastOpponents.get(loser).add(winner);
    }

    /**
     * Calculate the Buchholz score for each team.
     *
     * @param activeTeams    The list of active teams.
     * @param pastOpponents  The past opponents for each team.
     * @param records        The records of each team.
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
     * @param records     The records of each team.
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
     * @param results        The results of the simulations.
     * @param numSimulations The number of simulations.
     */
    private static void printResults(@NotNull Map<Team, Map<String, Integer>> results,
                                     int numSimulations, long startingTime,
                                     int specialReached, int specialSuccess) {
        System.out.println();
        System.out.println("Note: These predictions are not guaranteed to be accurate.");
        System.out.println("Use these simulations as a guideline, alongside form, betting odds, and results.");

        // Print the header
        Bracket.printHeader(numSimulations, startingTime);

        // Sort results entryset by team seeding
        List<Map.Entry<Team, Map<String, Integer>>> sortedEntries = new ArrayList<>(results.entrySet());
        sortedEntries.sort(Comparator.comparingInt(e -> e.getKey().getSeed()));

        // ---- Alignment prep: compute dynamic team-name column width ----
        int maxNameLen = sortedEntries.stream()
                .mapToInt(e -> e.getKey().getName().length())
                .max().orElse(10);
        int nameCol = Math.max(14, Math.min(28, maxNameLen + 2)); // clamp to a reasonable range

        // Print a fixed header row (added Rank and 3-A)
        System.out.printf(
                "%-8s %-" + nameCol + "s  %6s  %6s  %10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s%n",
                "Seed", "Team", "Rank", "Swing", "[3-X]", "[X-3]", "[3-0]", "[3-1]", "[3-2]", "[2-3]", "[1-3]", "[0-3]", "[3-A]"
        );

        // ---- Rows ----
        for (Map.Entry<Team, Map<String, Integer>> entry : sortedEntries) {
            Team team = entry.getKey();
            Map<String, Integer> recordCounts = entry.getValue();

            // Percentages for printing
            double p3xPct = (recordCounts.getOrDefault("3-1", 0) + recordCounts.getOrDefault("3-2", 0)) * 100.0 / numSimulations;
            double px3Pct = (recordCounts.getOrDefault("1-3", 0) + recordCounts.getOrDefault("2-3", 0)) * 100.0 / numSimulations;
            double p30Pct = recordCounts.getOrDefault("3-0", 0) * 100.0 / numSimulations;
            double p31Pct = recordCounts.getOrDefault("3-1", 0) * 100.0 / numSimulations;
            double p32Pct = recordCounts.getOrDefault("3-2", 0) * 100.0 / numSimulations;
            double p23Pct = recordCounts.getOrDefault("2-3", 0) * 100.0 / numSimulations;
            double p13Pct = recordCounts.getOrDefault("1-3", 0) * 100.0 / numSimulations;
            double p03Pct = recordCounts.getOrDefault("0-3", 0) * 100.0 / numSimulations;
            double p3APct = p3xPct + p30Pct;

            // Fixed-width aligned line; always print all columns on one line
            System.out.printf(
                    "(#%-2d) %-" + nameCol + "s  %6.2f  %6.2f  %10.2f%%  %10.2f%%  %10.2f%%  %10.2f%%  %10.2f%%  %10.2f%%  %10.2f%%  %10.2f%%  %10.2f%%%n",
                    team.getSeed(),
                    team.getName(),
                    team.getRank(),
                    team.getRoundSwing(),
                    p3xPct, px3Pct, p30Pct, p31Pct, p32Pct, p23Pct, p13Pct, p03Pct, p3APct
            );
        }

        // Print special conditions (existing)
        if (specialReached > 0) {
            System.out.println();
            System.out.println("Special Reached: " + specialReached);
            System.out.println("Special Success: " + specialSuccess);
            System.out.println("Special Odds: " + (specialSuccess * 100.0 / specialReached) + "%");
        }
    }
}
