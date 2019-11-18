package RoleML;

import lombok.Data;

import java.util.Set;

@Data
class TrainingParticipantData {
    private String champion;
    private Set<String> startingItems;
    private int participantId;
    private String frequentPosition;
    private int experienceAt12;
    private int creepScoreAt12;
    private int neutralKillsAt12;
    private String[] summonerSpells;
    private String validatedRole;
}

