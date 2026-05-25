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
package net.foulest.swiss;

import lombok.Data;
import net.foulest.swiss.brackets.ChampionsBracket;
import net.foulest.swiss.brackets.StandardBracket;
import net.foulest.swiss.match.Match;
import net.foulest.swiss.team.Team;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Swiss class for the program.
 *
 * @author Foulest
 */
@Data
public final class Swiss {

    private static final List<Team> teams = new ArrayList<>();

    /**
     * The main method of the program.
     *
     * @param args The program's arguments.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8).useLocale(Locale.ROOT);

        Team team1 = new Team("GamerLegion", 1, 1, 39, 19, 157,
                List.of(0.12, 0.74, 0.47, 0.62, 0.88), // trading
                List.of(0.38, 0.79, 0.14, 0.67, 0.29), // opening
                List.of(0.08, 0.74, 0.41, 0.67, 0.38), // firepower
                List.of(-2.58, 1.18, 0.28, -0.26, 1.01), // round swing
                List.of(0.80, 1.14, 1.02, 1.07, 1.05), // HLTV rating
                11, 36.4, 50.0, 38.0, 50.0, 0.0,
                9, 55.6, 49.5, 50.0, 46.7, 10.0,
                6, 66.7, 58.2, 55.2, 13.3, 0.0,
                7, 71.4, 52.2, 58.8, 7.7, 10.0,
                3, 33.3, 35.3, 58.3, 9.1, 26.1,
                3, 0.0, 35.3, 48.7, 0.0, 21.7,
                0, 0.0, 0.0, 0.0, 0.0, 100.0
        );

        Team team2 = new Team("B8", 2, 2, 63, 29, 172,
                List.of(0.51, 0.75, 0.55, 0.83, 0.25), // trading
                List.of(0.21, 0.70, 0.61, 0.24, 0.70), // opening
                List.of(0.17, 0.83, 0.51, 0.30, 0.34), // firepower
                List.of(-0.63, 0.92, -0.71, -0.37, -0.39), // round swing
                List.of(0.95, 1.11, 0.98, 0.98, 0.96), // HLTV rating
                15, 66.7, 59.5, 48.4, 63.6, 3.2,
                6, 16.7, 36.1, 34.3, 0.0, 32.4,
                7, 28.6, 64.6, 27.8, 0.0, 25.6,
                15, 60.0, 57.6, 45.5, 58.8, 0.0,
                7, 42.9, 47.7, 45.8, 0.0, 26.5,
                2, 0.0, 35.0, 18.8, 0.0, 76.0,
                11, 36.4, 50.4, 39.8, 5.0, 5.1
        );

        Team team3 = new Team("HEROIC", 3, 3, 56, 29, 97,
                List.of(0.51, 0.92, 0.43, 0.16, 0.54), // trading
                List.of(0.66, 0.36, 0.35, 0.64, 0.37), // opening
                List.of(0.59, 0.73, 0.32, 0.30, 0.51), // firepower
                List.of(0.05, 0.65, -0.07, -1.01, 0.46), // round swing
                List.of(1.05, 1.09, 1.02, 0.95, 1.04), // HLTV rating
                9, 44.4, 51.1, 40.9, 20.0, 11.8,
                9, 33.3, 43.2, 46.5, 15.4, 0.0,
                14, 64.3, 68.9, 37.0, 66.7, 0.0,
                4, 75.0, 54.2, 62.2, 0.0, 27.3,
                6, 50.0, 65.3, 40.8, 0.0, 25.6,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                14, 50.0, 63.1, 40.0, 35.3, 0.0
        );

        Team team4 = new Team("BetBoom", 4, 4, 55, 32, 103,
                List.of(0.28, 0.53, 0.67, 0.46, 0.36), // trading
                List.of(0.38, 0.53, 0.23, 0.62, 0.86), // opening
                List.of(0.30, 0.58, 0.29, 0.79, 0.90), // firepower
                List.of(-1.06, 0.36, -0.03, 0.10, 1.53), // round swing
                List.of(0.96, 1.03, 1.02, 1.09, 1.19), // HLTV rating
                11, 45.5, 51.2, 45.9, 26.7, 13.8,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                12, 58.3, 62.7, 36.9, 50.0, 8.7,
                10, 40.0, 57.9, 38.8, 17.6, 10.3,
                8, 50.0, 53.6, 44.6, 0.0, 19.4,
                7, 85.7, 49.4, 64.9, 36.4, 10.0,
                7, 85.7, 62.3, 61.1, 10.5, 22.2
        );

        Team team5 = new Team("BIG", 5, 5, 47, 25, 155,
                List.of(0.15, 0.07, 0.55, 0.65, 0.52), // trading
                List.of(0.41, 0.88, 0.25, 0.48, 0.50), // opening
                List.of(0.08, 0.79, 0.27, 0.81, 0.63), // firepower
                List.of(-1.58, 0.96, -0.77, 1.64, 0.31), // round swing
                List.of(0.87, 1.13, 0.95, 1.17, 1.05), // HLTV rating
                7, 71.4, 61.5, 44.6, 18.2, 24.0,
                5, 80.0, 60.7, 59.1, 25.0, 26.7,
                3, 33.3, 66.7, 19.4, 0.0, 71.4,
                10, 50.0, 54.5, 38.1, 28.6, 8.7,
                14, 57.1, 58.9, 47.7, 60.0, 0.0,
                4, 25.0, 28.1, 54.8, 0.0, 27.3,
                4, 25.0, 54.0, 25.6, 0.0, 26.9
        );

        Team team6 = new Team("M80", 6, 6, 42, 21, 85,
                List.of(0.29, 0.85, 0.72, 0.84, 0.38), // trading
                List.of(0.62, 0.31, 0.25, 0.21, 0.80), // opening
                List.of(0.26, 0.57, 0.10, 0.19, 0.42), // firepower
                List.of(1.48, 0.63, -0.88, -0.86, -0.25), // round swing
                List.of(1.04, 1.09, 0.93, 0.95, 0.99), // HLTV rating
                8, 37.5, 42.4, 46.5, 7.1, 12.5,
                11, 54.5, 43.4, 54.4, 66.7, 5.9,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                6, 16.7, 54.2, 27.4, 8.3, 16.7,
                5, 60.0, 52.3, 40.7, 22.2, 9.5,
                5, 60.0, 54.2, 48.8, 27.3, 15.8,
                7, 71.4, 56.0, 61.5, 0.0, 24.0
        );

        Team team7 = new Team("MIBR", 7, 7, 36, 18, 147,
                List.of(0.65, 0.75, 0.61, 0.48, 0.94), // trading
                List.of(0.30, 0.37, 0.87, 0.52, 0.27), // opening
                List.of(0.26, 0.20, 0.91, 0.33, 0.79), // firepower
                List.of(-0.17, -1.11, 1.39, -0.72, 0.66), // round swing
                List.of(1.00, 0.94, 1.22, 0.96, 1.12), // HLTV rating
                7, 42.9, 64.6, 36.5, 28.6, 0.0,
                6, 66.7, 55.1, 54.3, 50.0, 7.1,
                5, 20.0, 56.2, 37.1, 0.0, 19.0,
                7, 71.4, 54.4, 54.9, 40.0, 5.9,
                5, 0.0, 38.6, 34.8, 0.0, 33.3,
                4, 75.0, 52.9, 56.5, 28.6, 0.0,
                0, 0.0, 0.0, 0.0, 0.0, 100.0
        );

        Team team8 = new Team("SINNERS", 8, 8, 41, 18, 85,
                List.of(0.13, 0.22, 0.47, 0.14, 0.83), // trading
                List.of(0.64, 0.38, 0.49, 0.63, 0.48), // opening
                List.of(0.43, 0.37, 0.45, 0.54, 0.64), // firepower
                List.of(-0.26, 0.78, 0.23, -0.20, 0.10), // round swing
                List.of(1.02, 1.05, 1.00, 1.02, 1.06), // HLTV rating
                11, 54.5, 52.8, 49.0, 75.0, 6.2,
                3, 0.0, 50.0, 35.1, 0.0, 21.1,
                7, 42.9, 59.1, 42.0, 9.1, 9.1,
                5, 60.0, 55.1, 52.8, 0.0, 22.7,
                7, 85.7, 56.4, 59.7, 23.1, 16.7,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                4, 0.0, 41.9, 28.9, 11.1, 16.7
        );

        Team team9 = new Team("NRG", 9, 9, 33, 8, 58,
                List.of(0.52, 0.56, 0.65, 0.46, 0.86), // trading
                List.of(0.06, 0.69, 0.30, 0.67, 0.45), // opening
                List.of(0.06, 0.50, 0.10, 0.59, 0.46), // firepower
                List.of(-1.89, -0.58, -1.49, -0.76, -0.22), // round swing
                List.of(0.84, 1.00, 0.87, 1.00, 1.00), // HLTV rating
                6, 50.0, 67.3, 32.3, 30.8, 8.3,
                5, 40.0, 46.2, 55.0, 50.0, 6.2,
                8, 0.0, 38.8, 35.6, 0.0, 21.0,
                7, 42.9, 49.2, 40.0, 41.7, 11.1,
                2, 0.0, 25.0, 46.7, 0.0, 38.1,
                2, 0.0, 44.8, 44.8, 11.1, 15.0,
                3, 0.0, 40.6, 28.6, 0.0, 57.9
        );

        Team team10 = new Team("TYLOO", 10, 10, 19, 9, 104,
                List.of(0.31, 0.53, 0.69, 0.56, 0.32), // trading
                List.of(0.75, 0.26, 0.72, 0.47, 0.39), // opening
                List.of(0.76, 0.14, 0.40, 0.57, 0.29), // firepower
                List.of(1.83, -1.34, -0.79, 0.49, -1.00), // round swing
                List.of(1.19, 0.93, 1.00, 1.10, 0.97), // HLTV rating
                6, 50.0, 42.6, 54.0, 71.4, 10.0,
                2, 100.0, 43.5, 70.4, 16.7, 0.0,
                2, 100.0, 57.1, 66.7, 0.0, 30.8,
                4, 25.0, 39.0, 47.4, 0.0, 22.2,
                4, 25.0, 41.7, 48.1, 20.0, 0.0,
                1, 0.0, 66.7, 16.7, 0.0, 18.2,
                0, 0.0, 0.0, 0.0, 0.0, 100.0
        );

        Team team11 = new Team("Sharks", 11, 11, 30, 15, 50,
                List.of(0.67, 0.26, 0.33, 0.83, 0.52), // trading
                List.of(0.22, 0.40, 0.31, 0.68, 0.83), // opening
                List.of(0.27, 0.72, 0.41, 0.76, 0.67), // firepower
                List.of(-0.94, 0.53, 1.24, -0.05, 0.61), // round swing
                List.of(0.95, 1.07, 1.06, 1.09, 1.09), // HLTV rating
                4, 75.0, 69.8, 55.6, 10.0, 11.8,
                2, 50.0, 64.3, 25.0, 10.0, 27.8,
                9, 66.7, 59.0, 55.8, 50.0, 0.0,
                6, 33.3, 48.4, 47.2, 40.0, 5.9,
                7, 28.6, 54.1, 40.8, 0.0, 18.8,
                2, 50.0, 33.3, 64.7, 20.0, 8.3,
                0, 0.0, 0.0, 0.0, 0.0, 100.0
        );

        Team team12 = new Team("Gaimin Gladiators", 12, 12, 25, 10, 77,
                List.of(0.87, 0.34, 0.70, 0.67, 0.82), // trading
                List.of(0.69, 0.47, 0.26, 0.21, 0.54), // opening
                List.of(0.69, 0.44, 0.09, 0.69, 0.78), // firepower
                List.of(0.75, -0.13, -1.93, -0.28, -0.17), // round swing
                List.of(1.05, 0.99, 0.89, 1.06, 1.07), // HLTV rating
                1, 0.0, 8.3, 66.7, 0.0, 81.8,
                3, 66.7, 54.8, 45.5, 12.5, 23.1,
                6, 33.3, 47.2, 32.7, 0.0, 15.4,
                5, 40.0, 50.0, 42.6, 28.6, 7.7,
                3, 66.7, 62.5, 39.0, 22.2, 6.2,
                1, 0.0, 40.0, 16.7, 0.0, 53.8,
                6, 33.3, 50.9, 38.6, 62.5, 100.0
        );

        Team team13 = new Team("Liquid", 13, 13, 48, 16, 109,
                List.of(0.38, 0.56, 0.30, 0.27, 0.57), // trading
                List.of(0.30, 0.71, 0.83, 0.35, 0.38), // opening
                List.of(0.41, 0.58, 0.59, 0.12, 0.38), // firepower
                List.of(-0.21, -0.72, -0.39, -1.63, -0.19), // round swing
                List.of(1.02, 1.01, 1.03, 0.87, 0.98), // HLTV rating
                7, 57.1, 60.7, 38.9, 35.7, 4.2,
                8, 50.0, 44.2, 56.0, 8.3, 25.0,
                9, 11.1, 53.8, 27.2, 18.2, 12.0,
                13, 30.8, 58.3, 33.6, 27.8, 13.8,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                2, 50.0, 33.3, 75.0, 7.1, 20.7,
                9, 22.2, 49.5, 38.6, 29.4, 14.3
        );

        Team team14 = new Team("Lynn Vision", 14, 14, 6, 2, 111,
                List.of(0.23, 0.13, 0.59, 0.66, 0.49), // trading
                List.of(0.37, 0.84, 0.56, 0.52, 0.10), // opening
                List.of(0.11, 0.54, 0.58, 0.68, 0.51), // firepower
                List.of(-2.54, -1.69, 0.79, 0.62, 0.37), // round swing
                List.of(0.82, 0.97, 1.11, 1.05, 1.10), // HLTV rating
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                1, 0.0, 58.3, 20.0, 0.0, 50.0,
                3, 33.3, 30.0, 55.9, 0.0, 0.0,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                2, 50.0, 54.2, 50.0, 100.0, 33.3
        );

        Team team15 = new Team("THUNDERdOWNUNDER", 15, 15, 3, 1, 53,
                List.of(0.72, 0.27, 0.38, 0.88, 0.58), // trading
                List.of(0.32, 0.26, 0.70, 0.03, 0.83), // opening
                List.of(0.01, 0.53, 0.31, 0.34, 0.51), // firepower
                List.of(-4.66, 1.69, -0.58, -1.78, 0.95), // round swing
                List.of(0.67, 1.12, 0.89, 0.91, 1.17), // HLTV rating
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                1, 0.0, 66.7, 8.3, 0.0, 0.0,
                1, 0.0, 72.7, 16.7, 100.0, 0.0,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                1, 100.0, 41.7, 80.0, 0.0, 0.0
        );

        Team team16 = new Team("FlyQuest", 16, 16, 7, 1, 55,
                List.of(0.18, 0.23, 0.79, 0.97, 0.19), // trading
                List.of(0.46, 0.29, 0.49, 0.22, 0.76), // opening
                List.of(0.30, 0.03, 0.24, 0.29, 0.50), // firepower
                List.of(-0.07, -2.00, -1.62, -1.25, 0.19), // round swing
                List.of(1.01, 0.83, 0.92, 0.94, 1.00), // HLTV rating
                1, 0.0, 16.7, 0.0, 0.0, 33.3,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                3, 0.0, 43.6, 44.4, 0.0, 0.0,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                0, 0.0, 0.0, 0.0, 0.0, 100.0,
                2, 50.0, 62.5, 29.4, 100.0, 0.0,
                1, 0.0, 20.0, 58.3, 50.0, 0.0
        );

        teams.add(team1);
        teams.add(team2);
        teams.add(team3);
        teams.add(team4);
        teams.add(team5);
        teams.add(team6);
        teams.add(team7);
        teams.add(team8);

        teams.add(team9);
        teams.add(team10);
        teams.add(team11);
        teams.add(team12);
        teams.add(team13);
        teams.add(team14);
        teams.add(team15);
        teams.add(team16);

        System.out.println("Swiss - CS2 Major Monte Carlo Simulation");
        System.out.println("by Foulest | github.com/Foulest");
        System.out.println();
        System.out.println("Choose the bracket to simulate:");
        System.out.println("0. Manual Matches");
        System.out.println("1. Standard Bracket");
        System.out.println("2. Champions Bracket");
        System.out.println();
        System.out.print("Enter the bracket number: ");

        // Get whether to simulate Standard or Champions bracket
        int bracketNumber = scanner.nextInt();

        // Validate the input
        if (bracketNumber != 0 && bracketNumber != 1 && bracketNumber != 2) {
            System.out.println("Invalid input. Please enter 0, 1 or 2.");
            return;
        }

        // You can also display the winner of a match based on win probability
        // instead of simulating the entire bracket (these are just examples).
        if (bracketNumber == 0) {
            Match.displayWinnerFromProbability(team1, team9);
            Match.displayWinnerFromProbability(team2, team10);
            Match.displayWinnerFromProbability(team3, team11);
            Match.displayWinnerFromProbability(team4, team12);
            Match.displayWinnerFromProbability(team5, team13);
            Match.displayWinnerFromProbability(team6, team14);
            Match.displayWinnerFromProbability(team7, team15);
            Match.displayWinnerFromProbability(team8, team16);
            return;
        }

        boolean standardBracket = bracketNumber == 1;

        System.out.println();
        System.out.println("Note: At 25,000,000 simulations, the data is as accurate as it can be.");
        System.out.println("Anything beyond that would be computationally expensive and unnecessary.");
        System.out.println("On average, for Standard brackets, every 1,000,000 simulations takes 7.5 seconds.");
        System.out.println("On average, for Champions brackets, every 1,000,000 simulations takes 1.5 seconds.");
        System.out.println("You can do the math to figure out how long it would take to simulate your desired amount of brackets.");

        System.out.println();
        System.out.print("Enter the amount of brackets to simulate: ");

        // Get the amount of brackets to simulate based on user input
        int bracketsToSimulate = scanner.nextInt();
        int teamsSize = teams.size();

        // Validates team size for Standard brackets
        if (standardBracket && teamsSize != 16) {
            System.out.println();
            System.out.println("Invalid team count. Please make sure there are 16 teams in the list.");
            return;
        }

        // Validates team size for Champions brackets
        if (!standardBracket && teamsSize != 8) {
            System.out.println();

            if (teamsSize >= 8) {
                System.out.println("Invalid team count; trimming the list to the first 8 teams.");
                teams.subList(8, teamsSize).clear();
            } else {
                System.out.println("Invalid team count. Please make sure there are 8 teams in the list.");
                return;
            }
        }

        // Validate the input
        if (bracketsToSimulate <= 0 || bracketsToSimulate > 50000000) {
            System.out.println("Invalid input. Please enter a number between 1 and 50,000,000.");
            return;
        }

        System.out.println("Simulating " + bracketsToSimulate + " brackets...");

        // Simulate the brackets
        if (standardBracket) {
            StandardBracket bracket = new StandardBracket(teams);
            bracket.simulateMultipleBrackets(bracketsToSimulate);
        } else {
            ChampionsBracket bracket = new ChampionsBracket(teams);
            bracket.simulateMultipleBrackets(bracketsToSimulate);
        }
    }
}
