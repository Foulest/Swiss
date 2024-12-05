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
    private double avgPlayerRating; // Average player rating

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
