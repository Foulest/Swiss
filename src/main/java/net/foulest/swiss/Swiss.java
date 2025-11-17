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

    public static final List<Team> teams = new ArrayList<>();

    /**
     * The main method of the program.
     *
     * @param args The program's arguments.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, "UTF-8").useLocale(Locale.ROOT);

        // Create teams with data from HLTV
        Team team1 = new Team("Legacy", 8, 1, 1, 1.01);
        Team team2 = new Team("FaZe", 15, 2, 2, 0.99);
        Team team3 = new Team("B8", 18, 3, 3, 0.98);
        Team team4 = new Team("GamerLegion", 19, 4, 4, 0.96);
        Team team5 = new Team("fnatic", 21, 5, 5, 1.01);
        Team team6 = new Team("PARIVISION", 22, 6, 6, 1.08);
        Team team7 = new Team("NIP", 34, 7, 7, 0.96);
        Team team8 = new Team("Imperial", 31, 8, 8, 0.99);
        Team team9 = new Team("FlyQuest", 37, 9, 9, 0.90);
        Team team10 = new Team("Lynn Vision", 33, 10, 10, 0.97);
        Team team11 = new Team("M80", 30, 11, 11, 0.97);
        Team team12 = new Team("Fluxo", 39, 12, 12, 0.96);
        Team team13 = new Team("Red Canids", 61, 13, 13, 0.90);
        Team team14 = new Team("The Huns", 55, 14, 14, 0.90);
        Team team15 = new Team("NRG", 32, 15, 15, 0.99);
        Team team16 = new Team("Rare Atom", 63, 16, 16, 0.92);

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
            Match.displayWinnerFromProbability(team1, team2, false);
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
        } else {
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
}
