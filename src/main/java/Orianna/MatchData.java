package Orianna;

import com.merakianalytics.orianna.types.common.EventType;
import com.merakianalytics.orianna.types.common.Region;
import com.merakianalytics.orianna.types.core.match.*;
import com.merakianalytics.orianna.types.core.staticdata.Item;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static RoleML.RiftCoordinates.*;
import static RoleML.RiftCoordinates.Y_MIN;

@Data
@Document(collection = "match")
@TypeAlias("match")
public class MatchData {
    @Id
    private Long id;

    private Long creationTime;
    private Integer duration;
    private Map<String, ParticipantData> blueTeam;
    private Map<String, ParticipantData> redTeam;
    private String winner;
    private Region region;
    private boolean remake;
    private Map<Integer, ParticipantData> participantData;

    @PersistenceConstructor
    public MatchData(Long id, Long creationTime, Integer duration, Map<String, ParticipantData> blueTeam, Map<String, ParticipantData> redTeam, String winner, Region region, boolean remake) {
        this.id = id;
        this.creationTime = creationTime;
        this.duration = duration;
        this.blueTeam = blueTeam;
        this.redTeam = redTeam;
        this.winner = winner;
        this.region = region;
        this.remake = remake;
    }

    public MatchData(Match m) {
        Timeline timeline = m.getTimeline();
        this.blueTeam = new HashMap<>();
        this.redTeam = new HashMap<>();
        this.id = m.getId();
        this.creationTime = m.getCreationTime().getMillis();
        this.duration = m.getDuration().toStandardSeconds().getSeconds();
        this.remake = m.isRemake();
        if (m.getBlueTeam().isWinner()) {
            this.winner = "BLUE";
        }
        else if (m.getRedTeam().isWinner() ) {
            this.winner = "RED";
        }
        this.participantData = new HashMap<>();
        for (Participant participant : m.getParticipants()) {
            ParticipantData pd = new ParticipantData(participant);
            this.participantData.put(participant.getCoreData().getParticipantId(), pd);
        }
        this.getParticipantData(m, timeline);
    }
    public void setTeams() {
        for (ParticipantData participant : participantData.values()) {
            if(participant.getTeam().equals("BLUE")) {
                blueTeam.put(participant.getValidatedRole(), participant);
            }
            else {
                redTeam.put(participant.getValidatedRole(), participant);
            }
        }
    }
    private void getParticipantData(Match m, Timeline timeline) {
        getExperienceAndCS(timeline);
        //getChampionsAndParticipantId(match);
        getStartingItems(timeline);
        getSummonerSpells(m);
        getRoles(m, timeline);

    }
//    private void getChampionsAndParticipantId(Match match) {
//        this.participantData = new HashMap<>();
//        for (Participant p : match.getParticipants()) {
//            this.participantData.get(p.getCoreData().getParticipantId()).setChampion(p.getChampion().getName());
//            this.participantData.get(p.getCoreData().getParticipantId()).setParticipantId(p.getCoreData().getParticipantId());
//        }
//    }
    private void getExperienceAndCS(Timeline timeline) {
        Frame f = timeline.get(12);
        for (Map.Entry<Participant, ParticipantFrame> entry : f.getParticipantFrames().entrySet()) {
            participantData.get(entry.getKey().getCoreData().getParticipantId()).setCreepScoreAt12(entry.getValue().getCreepScore());
            participantData.get(entry.getKey().getCoreData().getParticipantId()).setExperienceAt12(entry.getValue().getExperience());
            participantData.get(entry.getKey().getCoreData().getParticipantId()).setNeutralKillsAt12(entry.getValue().getNeutralMinionsKilled());
        }
    }
    private void getStartingItems(Timeline timeline) {
        Map<Participant, List<Event>> itemEvents = new HashMap<>();
        for (int i = 1; i <= 2; i++) {
            Frame f = timeline.get(i);
            for (Event e : f) {
                if ((e.getType() == EventType.ITEM_PURCHASED) || (e.getType() == EventType.ITEM_UNDO)) {
                    if (!itemEvents.containsKey(e.getParticipant())) {
                        itemEvents.put(e.getParticipant(), new LinkedList<>());
                    }
                    itemEvents.get(e.getParticipant()).add(e);
                }
            }
        }
        for (Map.Entry<Participant, List<Event>> entry : itemEvents.entrySet()) {
            Map<String, Integer> participantItems = new HashMap<>();
            Participant p = entry.getKey();
            for (Event e : entry.getValue()) {
                if (e.getType() == EventType.ITEM_PURCHASED) {
                    if (!participantItems.containsKey(e.getItem().getName())) {
                        participantItems.put(e.getItem().getName(), 1);
                    }
                    else {
                        participantItems.put(e.getItem().getName(),  participantItems.get(e.getItem().getName()) + 1);
                    }
                }
                else if (e.getType() == EventType.ITEM_UNDO) {
                    Item item = e.getBefore();
                    if (item == null) {
                        item = e.getAfter();
                    }
                    if (!participantItems.containsKey(item.getName())) {continue;}
                    participantItems.put(item.getName(), participantItems.get(item.getName())-1);
                    if (participantItems.get(item.getName())==0) {
                        participantItems.remove(item.getName());
                    }
                }
            }
            //System.out.print(p.getChampion().getName() + " ");
            //System.out.println(Arrays.toString(participantItems.keySet().toArray()));
            participantData.get(p.getCoreData().getParticipantId()).setStartingItems(participantItems.keySet());
        }
    }
    private void getSummonerSpells(Match m) {
        for (Participant participant : m.getParticipants()) {
            participantData.get(
                    participant.getCoreData().getParticipantId()).setSummonerSpells(
                    new String[] {participant.getSummonerSpellD().getName(),
                            participant.getSummonerSpellF().getName()});
        }
    }
    private void getRoles(Match m, Timeline timeline) {
        if (15 > m.getDuration().getStandardMinutes())
            {System.out.println("SHORT GAME");}
        HashMap<Integer, Participant> pIdMap = new HashMap<>();
        for (Participant p : m.getParticipants()) {
            //System.out.println(order + ": " + p.getChampion().getName());
            pIdMap.put(p.getCoreData().getParticipantId(), p);
        }
        HashMap<Integer, HashMap<String, Integer>> locationFrequency = new HashMap<>();
        for (int i = 2; i < Math.min(15, m.getDuration().getStandardMinutes()); i++) {
            Frame f = timeline.get(i);
            Map<Participant, ParticipantFrame> pFrames = f.getParticipantFrames();
            for (Map.Entry e : pFrames.entrySet()) {
                Participant participant = (Participant) e.getKey();
                int participantId = participant.getCoreData().getParticipantId();
                //System.out.print(participantId + " " + participant.getChampion().getName() + " ");
                Position p = ((ParticipantFrame) e.getValue()).getPosition();
                String loc = getLocation(p);
                if (locationFrequency.containsKey(participantId)) {
                    locationFrequency.get(participantId).put(loc, locationFrequency.get(participantId).get(loc) + 1);
                } else {
                    HashMap<String, Integer> locCount = new HashMap<>();
                    locCount.put("TOP", 0);
                    locCount.put("MID", 0);
                    locCount.put("BOT", 0);
                    locCount.put("NONE", 0);
                    locCount.put("JUNGLE", 0);
                    locCount.put(loc, 1);
                    locationFrequency.put(participantId, locCount);
                }
            }
        }
        for (int participantId : pIdMap.keySet()) {
            int loc_max_count = -1;
            String loc_max_name = "";
            //System.out.print(champion + " " + participantId + " ");
            HashMap<String, Integer> locations = locationFrequency.get(participantId);
            for (String loc : locations.keySet()) {
                //System.out.print(loc + ": " + locations.get(loc) + " ");
                int count = locations.get(loc);
                if (count > loc_max_count) {
                    loc_max_count = count;
                    loc_max_name = loc;
                }
            }
            participantData.get(participantId).setFrequentPosition(loc_max_name);
        }
    }


    private String getLocation(Position p) {
        String loc;
        double top = Point2D.distance(topPosition[0], topPosition[1], p.getX(), p.getY());
        double mid = Point2D.distance(midPosition[0], midPosition[1], p.getX(), p.getY());
        double bot = Point2D.distance(botPosition[0], botPosition[1], p.getX(), p.getY());
        double choice = Math.min(top, Math.min(mid, bot));
        if (choice==top && choice < 4500) {
            loc = "TOP";
        }
        else if (choice==mid && choice < 2500) {
            loc = "MID";
        }
        else if (choice==bot && choice < 4500) {
            loc = "BOT";
        }
        else {
            if (Point2D.distance(X_MAX, Y_MAX, p.getX(), p.getY()) < 3000 ||
                    Point2D.distance(X_MIN, Y_MIN, p.getX(), p.getY()) < 3000) {
                loc = "NONE";
            }
            else {loc = "JUNGLE";}
        }
        //System.out.println("(" + p.getX() + "," + p.getY() + ") " + loc + " " + choice);
        return loc;
    }
}
