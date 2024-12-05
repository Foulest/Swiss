package net.foulest.swiss;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

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
        for (Team team : Main.teams) {
            if (team.name.equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }
}
