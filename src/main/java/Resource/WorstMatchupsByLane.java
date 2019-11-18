package Resource;

import lombok.Data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class WorstMatchupsByLane {
    private Map<String, List<Champ>> laneMatchups;
    public WorstMatchupsByLane() {
        this.laneMatchups = new HashMap<>();
    }
}
@Data
class Champ {
    private String name;
    private List<Matchup> opponents;
    public Champ() {
        opponents = new LinkedList<>();
    }
}
@Data
class Matchup {
    private String name;
    private double laneWinRate;
    private double gameWinRate;
}
