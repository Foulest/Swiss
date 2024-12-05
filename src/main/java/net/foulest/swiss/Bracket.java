package net.foulest.swiss;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Data
class Bracket {

    private List<Team> teams;
    // Pairwise 3-0
    private Map<Team, Map<Team, Integer>> pairwise3_0 = new HashMap<>();

    Bracket(@NotNull List<Team> teams) {
        this.teams = teams;
    }

    @SuppressWarnings("NestedMethodCall")
    void simulateMultipleBrackets(int numSimulations) {
        // Map to track results for each team
        Map<Team, Map<String, Integer>> results = new HashMap<>();

        // Initialize results map for each team
        teams.forEach(team -> results.put(team, new HashMap<>()));

        // Run simulations sequentially
        for (int i = 0; i < numSimulations; i++) {
            simulateBracket(results); // Simulate one bracket
        }

        // Analyze and print the results after all simulations are complete
        printResults(results, numSimulations);
    }

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
            Team higherSeed = seededTeams.get(i); // Top seed
            Team lowerSeed = seededTeams.get(seededTeams.size() / 2 + i); // Bottom seed
            Match match = new Match(higherSeed, lowerSeed, false);

            Team winner = match.simulate();
            Team loser = (winner == higherSeed) ? lowerSeed : higherSeed;

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

                    if (idealMatchup != null) {
                        Match match = new Match(team, idealMatchup, true);
                        matches.add(match);
                        availableTeams.remove(idealMatchup);
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
                    Team winner = match.simulate();
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

    private static void updateRecords(@NotNull Map<Team, int[]> records, Team winner, Team loser) {
        records.get(winner)[0] += 1;
        records.get(loser)[1] += 1;
    }

    private static void updatePastOpponents(@NotNull Map<Team, List<Team>> pastOpponents, Team winner, Team loser) {
        pastOpponents.get(winner).add(loser);
        pastOpponents.get(loser).add(winner);
    }

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

    private static boolean allTeamsDecided(@NotNull List<Team> activeTeams, Map<Team, int[]> records) {
        for (Team team : activeTeams) {
            int[] record = records.get(team);

            if (record[0] < 3 && record[1] < 3) {
                return false; // At least one team still has games to play
            }
        }
        return true; // All teams have either 3 wins or 3 losses
    }

    @SuppressWarnings("NestedMethodCall")
    private static void printResults(@NotNull Map<Team, Map<String, Integer>> results, int numSimulations) {
        System.out.println();
        System.out.println("Individual Team Results:");

        for (Map.Entry<Team, Map<String, Integer>> entry : results.entrySet()) {
            Team team = entry.getKey();
            Map<String, Integer> recordCounts = entry.getValue();

            // Create a map to combine probabilities for 3-X and X-3
            double probability3X = 0.0;
            double probabilityX3 = 0.0;

            // Combine the 3-1 and 3-2 probabilities into 3-X
            probability3X += (recordCounts.getOrDefault("3-1", 0)
                    + recordCounts.getOrDefault("3-2", 0)) * 100.0 / numSimulations;

            // Combine the 1-3 and 2-3 probabilities into X-3
            probabilityX3 += (recordCounts.getOrDefault("1-3", 0)
                    + recordCounts.getOrDefault("2-3", 0)) * 100.0 / numSimulations;

            // Prepare the result string
            String teamName = team.getName();
            StringBuilder resultString = new StringBuilder("Team: " + teamName + " | ");

            // Add the combined 3-X and X-3 probabilities to the result string
            resultString.append("3-X (").append(String.format("%.2f", probability3X)).append("%) ");
            resultString.append("X-3 (").append(String.format("%.2f", probabilityX3)).append("%) ");

            // Add the individual probabilities for 3-0, 0-3, and 3-1/3-2, 1-3/2-3
            if (recordCounts.containsKey("3-0")) {
                double probability3_0 = (recordCounts.getOrDefault("3-0", 0) * 100.0) / numSimulations;
                resultString.append("3-0 (").append(String.format("%.2f", probability3_0)).append("%) ");
            }
            if (recordCounts.containsKey("0-3")) {
                double probability0_3 = (recordCounts.getOrDefault("0-3", 0) * 100.0) / numSimulations;
                resultString.append("0-3 (").append(String.format("%.2f", probability0_3)).append("%) ");
            }

            // Print the team's result
            System.out.println(resultString.toString().trim());
        }
    }
}
