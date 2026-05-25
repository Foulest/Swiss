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
import org.jetbrains.annotations.Contract;

import java.util.List;
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

    // The name of the team
    private final String name;

    // The team's seed in the standard Swiss bracket
    // Lower number = better seed (i.e., 1 is highest seed)
    private int seed;

    // The team's seed in the championship bracket (if applicable)
    private int cSeed;

    // The number of maps played by the team (last 3 months vs top-50 teams)
    private int mapsPlayed;

    // The number of maps won by the team (last 3 months vs top-50 teams)
    private int mapsWon;

    // The Opponent Network VRS stat for the team (last 3 months)
    private int opponentNetwork;

    // The trading percentages of the team (last 3 months vs top-50 teams)
    private List<Double> trading;

    // The opening percentages of the team (last 3 months vs top-50 teams)
    private List<Double> opening;

    // The firepower percentages of the team (last 3 months vs top-50 teams)
    private List<Double> firepower;

    // The round swing percentages of the team (last 3 months vs top-50 teams)
    private List<Double> roundSwing;

    // The HLTV ratings of the team (last 3 months vs top-50 teams)
    private List<Double> hltvRating;

    // Map statistics for Mirage (last 3 months vs top-50 teams)
    private int mirageMapsPlayed;
    private double mirageWinRate;
    private double mirageWinRateCT;
    private double mirageWinRateT;
    private double miragePickPercent;
    private double mirageBanPercent;

    // Map statistics for Inferno (last 3 months vs top-50 teams)
    private int infernoMapsPlayed;
    private double infernoWinRate;
    private double infernoWinRateCT;
    private double infernoWinRateT;
    private double infernoPickPercent;
    private double infernoBanPercent;

    // Map statistics for Nuke (last 3 months vs top-50 teams)
    private int nukeMapsPlayed;
    private double nukeWinRate;
    private double nukeWinRateCT;
    private double nukeWinRateT;
    private double nukePickPercent;
    private double nukeBanPercent;

    // Map statistics for Ancient (last 3 months vs top-50 teams)
    private int ancientMapsPlayed;
    private double ancientWinRate;
    private double ancientWinRateCT;
    private double ancientWinRateT;
    private double ancientPickPercent;
    private double ancientBanPercent;

    // Map statistics for Overpass (last 3 months vs top-50 teams)
    private int overpassMapsPlayed;
    private double overpassWinRate;
    private double overpassWinRateCT;
    private double overpassWinRateT;
    private double overpassPickPercent;
    private double overpassBanPercent;

    // Map statistics for Anubis (last 3 months vs top-50 teams)
    private int anubisMapsPlayed;
    private double anubisWinRate;
    private double anubisWinRateCT;
    private double anubisWinRateT;
    private double anubisPickPercent;
    private double anubisBanPercent;

    // Map statistics for Dust II (last 3 months vs top-50 teams)
    private int dustIIMapsPlayed;
    private double dustIIWinRate;
    private double dustIIWinRateCT;
    private double dustIIWinRateT;
    private double dustIIPickPercent;
    private double dustIIBanPercent;

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Team team = (Team) obj;
        return Objects.equals(name, team.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public Team clone() {
        return new Team(name, seed, cSeed, mapsPlayed, mapsWon, opponentNetwork, trading, opening, firepower, roundSwing, hltvRating,
                mirageMapsPlayed, mirageWinRate, mirageWinRateCT, mirageWinRateT, miragePickPercent, mirageBanPercent,
                infernoMapsPlayed, infernoWinRate, infernoWinRateCT, infernoWinRateT, infernoPickPercent, infernoBanPercent,
                nukeMapsPlayed, nukeWinRate, nukeWinRateCT, nukeWinRateT, nukePickPercent, nukeBanPercent,
                ancientMapsPlayed, ancientWinRate, ancientWinRateCT, ancientWinRateT, ancientPickPercent, ancientBanPercent,
                overpassMapsPlayed, overpassWinRate, overpassWinRateCT, overpassWinRateT, overpassPickPercent, overpassBanPercent,
                anubisMapsPlayed, anubisWinRate, anubisWinRateCT, anubisWinRateT, anubisPickPercent, anubisBanPercent,
                dustIIMapsPlayed, dustIIWinRate, dustIIWinRateCT, dustIIWinRateT, dustIIPickPercent, dustIIBanPercent);
    }
}
