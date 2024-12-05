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
        Team g2 = new Team("G2", 1, 2, 1.046);
        Team natusVincere = new Team("Natus Vincere", 2, 1, 1.098);
        Team vitality = new Team("Vitality", 3, 3, 1.110);
        Team spirit = new Team("Spirit", 4, 4, 1.074);
        Team mouz = new Team("MOUZ", 5, 5, 1.034);
        Team faze = new Team("FaZe", 6, 6, 1.068);
        Team heroic = new Team("Heroic", 7, 7, 1.098);
        Team _3DMAX = new Team("3DMAX", 8, 16, 1.056);
        Team theMongolZ = new Team("The MongolZ", 9, 9, 1.128);
        Team liquid = new Team("Liquid", 10, 13, 1.028);
        Team gamerLegion = new Team("GamerLegion", 11, 20, 1.032);
        Team furia = new Team("FURIA", 12, 8, 1.044);
        Team paiN = new Team("paiN", 13, 15, 1.108);
        Team wildcard = new Team("Wildcard", 14, 22, 1.056);
        Team big = new Team("BIG", 15, 19, 1.060);
        Team mibr = new Team("MIBR", 16, 17, 1.064);

        // Add teams to the list
        teams.add(g2);
        teams.add(natusVincere);
        teams.add(vitality);
        teams.add(spirit);
        teams.add(mouz);
        teams.add(faze);
        teams.add(heroic);
        teams.add(_3DMAX);
        teams.add(theMongolZ);
        teams.add(liquid);
        teams.add(gamerLegion);
        teams.add(furia);
        teams.add(paiN);
        teams.add(wildcard);
        teams.add(big);
        teams.add(mibr);

        System.out.println("Swiss - CS2 Major Monte Carlo Simulation");
        System.out.println("by Foulest | github.com/Foulest");
        System.out.println();
        System.out.println("Choose the bracket to simulate:");
        System.out.println("1. Standard Bracket");
        System.out.println("2. Champions Bracket");
        System.out.println();
        System.out.print("Enter the bracket number: ");

        // Get whether to simulate Standard or Champions bracket
        int bracketNumber = scanner.nextInt();

        if (bracketNumber != 1 && bracketNumber != 2) {
            System.out.println("Invalid input. Please enter 1 or 2.");
            return;
        }

        boolean standardBracket = bracketNumber == 1;

        System.out.println();
        System.out.println("Note: At 25,000,000 simulations, the data is as accurate as it can be.");
        System.out.println("Anything beyond that would be computationally expensive and unnecessary.");
        System.out.println("On average, for Standard brackets, every 1,000,000 simulations takes 7.5 seconds.");
        System.out.println("On average, for Champions brackets, every 1,000,000 simulations takes 1.5 seconds.");
        System.out.println("You can do the math to figure out how long it would take to simulate your desired amount of brackets.");
        System.out.println("You can also enter 0 to just print the win probability of the matches below.");
        System.out.println();
        System.out.print("Enter the amount of brackets to simulate: ");

        // Get the amount of brackets to simulate based on user input
        int bracketsToSimulate = scanner.nextInt();

        // Validates team size for Standard brackets
        if (standardBracket && teams.size() != 16) {
            System.out.println();
            System.out.println("Invalid team count. Please make sure there are 16 teams in the list.");
            return;
        }

        // Validates team size for Champions brackets
        if (!standardBracket && teams.size() != 8) {
            if (teams.size() >= 8) {
                System.out.println();
                System.out.println("Invalid team count; trimming the list to the first 8 teams.");
                teams.subList(8, teams.size()).clear();
            } else {
                System.out.println();
                System.out.println("Invalid team count. Please make sure there are 8 teams in the list.");
                return;
            }
        }

        // Validate the input
        if (bracketsToSimulate < 0 || bracketsToSimulate > 50000000) {
            System.out.println("Invalid input. Please enter a number between 1 and 50,000,000.");
            return;
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

        // You can also display the winner of a match based on win probability
        // instead of simulating the entire bracket.
        if (bracketsToSimulate == 0) {
            Match.displayWinnerFromProbability(natusVincere, mibr);
            Match.displayWinnerFromProbability(vitality, furia);
            Match.displayWinnerFromProbability(mouz, theMongolZ);
            Match.displayWinnerFromProbability(faze, heroic);
            Match.displayWinnerFromProbability(g2, big);
            Match.displayWinnerFromProbability(spirit, wildcard);
            Match.displayWinnerFromProbability(_3DMAX, paiN);
            Match.displayWinnerFromProbability(liquid, gamerLegion);
        }
    }
}