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
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Represents a team in the tournament.
 *
 * @author Foulest
 */
@Getter
@Setter
@AllArgsConstructor
public class Team implements Cloneable {

    private String name;
    private double rank; // Lower number = better rank (i.e., 1 is highest rank)
    private int seed; // Lower number = better seed (i.e., 1 is highest seed)
    private int cSeed; // Where the team is seeded in the championship bracket (if applicable)

    // The combined round swing percentage of the team
    private double roundSwing;

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
        return new Team(name, rank, seed, cSeed, roundSwing);
    }
}
