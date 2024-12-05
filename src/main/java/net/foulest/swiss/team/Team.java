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
package net.foulest.swiss.team;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.foulest.swiss.Swiss;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a team in the tournament.
 */
@Data
@AllArgsConstructor
public class Team {

    private String name;
    private int seeding; // Higher number = better seed
    private int worldRanking; // Lower number = better rank

    // The average player rating is calculated by adding each player's
    // HLTV 2.1 rating from the past 3 months together and dividing by five
    private double avgPlayerRating;

    // Method to get the Team object based on the team name
    public static @Nullable Team getTeamByName(String name) {
        for (Team team : Swiss.teams) {
            if (team.name.equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }
}