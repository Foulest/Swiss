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
public class ChampionsBracket implements Bracket {

    private List<Team> teams;
    private long startingTime;

    public ChampionsBracket(@NotNull List<Team> teams) {
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

        // Initialize all the records to 0-0 (just for tracking wins/losses)
        for (Team team : teams) {
            records.put(team, new int[]{0, 0}); // [wins, losses]
        }

        // Sort teams by their champion seeding in ascending order
        List<Team> seededTeams = new ArrayList<>(teams);
        seededTeams.sort(Comparator.comparingInt(Team::getChampionSeed));

        List<Team> activeTeams = seededTeams.stream()
                .map(Team::clone) // Clone or copy each team
                .collect(Collectors.toList());

        // Simulate each round, eliminating teams that lose
        while (activeTeams.size() > 1) {
            List<Team> nextRoundTeams = new ArrayList<>();

            // Process matchups by seeding order
            for (int i = 0; i < activeTeams.size() / 2; i++) {
                Team team1 = activeTeams.get(i); // Lower seed
                Team team2 = activeTeams.get(activeTeams.size() - 1 - i); // Higher seed

                // Create a match between the two teams
                Match match = new Match(team1, team2, 1); // Best of 1 match

                // Simulate the match
                Team winner = match.simulate(false);
                Team loser = (winner == team1) ? team2 : team1;

//                // Update team ratings
//                updateTeamKDR(team1, team2, winner, false);

                // Update records
                Bracket.updateRecords(records, winner, loser);

//                // Print the match result
//                printMatchResult(winner, records, loser);

                // Only add the winner to the next round
                nextRoundTeams.add(winner);
            }

            // If there is an odd number of teams, the last team automatically advances
            if (activeTeams.size() % 2 == 1) {
                Team byeTeam = activeTeams.get(activeTeams.size() / 2);
                nextRoundTeams.add(byeTeam);
            }

            // Update active teams for the next round
            activeTeams = nextRoundTeams;
        }

        // Record final results
        for (Map.Entry<Team, int[]> entry : records.entrySet()) {
            Team team = entry.getKey();
            int[] record = entry.getValue();
            String result = record[0] + "-" + record[1]; // Format: "XW-XL"

            // Record every team's final record
            results.get(team).merge(result, 1, Integer::sum);
        }
    }

    /**
     * Print the results of the simulations.
     *
     * @param results         The results of the simulations.
     * @param numSimulations  The number of simulations.
     */
    private static void printResults(@NotNull Map<Team, Map<String, Integer>> results,
                                     int numSimulations, long startingTime) {
        // Print the header
        Bracket.printHeader(numSimulations, startingTime);

        for (Map.Entry<Team, Map<String, Integer>> entry : results.entrySet()) {
            Team team = entry.getKey();
            Map<String, Integer> recordCounts = entry.getValue();

            // Build the result string
            String teamName = team.getName();
            StringBuilder resultString = new StringBuilder(String.format("Team: %s | ", teamName));

            // Add individual probabilities
            Bracket.appendProbability(resultString, "3-0", recordCounts, numSimulations);
            Bracket.appendProbability(resultString, "2-1", recordCounts, numSimulations);
            Bracket.appendProbability(resultString, "1-1", recordCounts, numSimulations);
            Bracket.appendProbability(resultString, "0-1", recordCounts, numSimulations);

            // Print the team's result
            String result = resultString.toString();
            String trimmed = result.trim();
            System.out.println(trimmed);
        }
    }
}
