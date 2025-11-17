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

import java.util.Objects;

/**
 * Represents a team in the tournament.
 *
 * @author Foulest
 */
@Data
@AllArgsConstructor
public class Team implements Cloneable {

    private String name;
    private int worldRanking; // Lower number = better rank
    private int seeding; // Higher number = better seed
    private int championSeed; // Where the team is seeded in the championship bracket

    // The KDR from the past three months vs Top 50 teams; 0.95 if no data available
    private double kdr;

    // Method to get the Team object based on the team name
    public static @Nullable Team getTeamByName(String name) {
        for (Team team : Swiss.teams) {
            if (team.name.equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Team team = (Team) o;
        return Objects.equals(name, team.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public Team clone() {
        return new Team(name, worldRanking, seeding, championSeed, kdr);
    }
}
