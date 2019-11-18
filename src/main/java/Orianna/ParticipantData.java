package Orianna;

import com.merakianalytics.orianna.types.core.match.*;
import lombok.Data;
import org.springframework.data.annotation.PersistenceConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
public
class ParticipantData {
    private String champion;
    private String summonerId;
    private String team;
    private Set<String> startingItems;
    private String[] summonerSpells;
    private int participantId;
    private String frequentPosition;
    private int experienceAt12;
    private int creepScoreAt12;
    private int neutralKillsAt12;
    private Map<String, Integer> stats;
    private Map<String, Map<String, Integer>> timeline;
    //null until ML Part
    private String validatedRole;


    @PersistenceConstructor
    public ParticipantData(String champion, String summonerId, String team, Set<String> startingItems, String[] summonerSpells, int participantId, String frequentPosition, int experienceAt12, int creepScoreAt12, int neutralKillsAt12, Map<String, Integer> stats, Map<String, Map<String, Integer>> timeline, String validatedRole) {
        this.champion = champion;
        this.summonerId = summonerId;
        this.team = team;
        this.startingItems = startingItems;
        this.summonerSpells = summonerSpells;
        this.participantId = participantId;
        this.frequentPosition = frequentPosition;
        this.experienceAt12 = experienceAt12;
        this.creepScoreAt12 = creepScoreAt12;
        this.neutralKillsAt12 = neutralKillsAt12;
        this.stats = stats;
        this.timeline = timeline;
        this.validatedRole = validatedRole;
    }

    ParticipantData(Participant participant) {
        this.champion = participant.getChampion().getName();
        this.participantId = participant.getCoreData().getParticipantId();
        this.team = participant.getTeam().getSide().name();
        this.summonerId = participant.getSummoner().getId();
        this.stats = getStats(participant.getStats());
        this.timeline = getTimeline(participant.getTimeline());
    }
    private Map<String, Integer> getStats(ParticipantStats participantStats) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("kills",participantStats.getKills());
        stats.put("deaths", participantStats.getDeaths());
        stats.put("assists", participantStats.getAssists());
        stats.put("combatScore", participantStats.getCombatScore());
        stats.put("visionScore", participantStats.getVisionScore());
        stats.put("creepScore", participantStats.getCreepScore());
        stats.put("wardsKilled", participantStats.getWardsKilled());
        stats.put("wardsPlaced", participantStats.getWardsPlaced());
        stats.put("pinkWards", participantStats.getPinkWardsPurchased());
        return stats;
    }
    private Map<String, Map<String, Integer>> getTimeline(ParticipantTimeline participantTimeline) {
        Map<String, Map<String, Integer>> timeline = new HashMap<>();

        Map<String, Integer> CS = new HashMap<>();
        CS.put("10", (int) participantTimeline.getCreepScore().getAt10());
        CS.put("20", (int) participantTimeline.getCreepScore().getAt20());
        CS.put("30", (int) participantTimeline.getCreepScore().getAt30());
        CS.put("Final", (int) participantTimeline.getCreepScore().getAtGameEnd());
        timeline.put("creepScore", CS);

        Map<String, Integer> gold = new HashMap<>();
        gold.put("10", (int) participantTimeline.getGold().getAt10());
        gold.put("20", (int) participantTimeline.getGold().getAt20());
        gold.put("30", (int) participantTimeline.getGold().getAt30());
        gold.put("Final", (int) participantTimeline.getGold().getAtGameEnd());
        timeline.put("gold", gold);

        return timeline;
    }

}
