/*
 * JavaTemplate - a fully featured Java template with Gradle.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Main class for the program.
 *
 * @author Foulest
 */
@Data
public final class Main {

    public static final List<Team> teams = new ArrayList<>();

    /**
     * The main method of the program.
     *
     * @param args The program's arguments.
     */
    public static void main(String[] args) {
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

        // At 1,000,000 simulations, the data is as accurate as it can be.
        // On average, the default simulation of 1,000,000 brackets takes 60 seconds.
        Bracket bracket = new Bracket(teams);
        bracket.simulateMultipleBrackets(1000000);
    }
}
