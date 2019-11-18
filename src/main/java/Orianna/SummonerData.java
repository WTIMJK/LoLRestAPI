package Orianna;

import com.merakianalytics.orianna.Orianna;
import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.common.Season;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.MatchHistory;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;


@Data
@Document(collection = "summoner")
@TypeAlias("summoner")
public class SummonerData {
    @Id
    String id;
    @DBRef
    private Map<Long, MatchData> matchList;
    private long lastUpdated;
    private String name;
    @Transient
    private Summoner summoner;



    @PersistenceConstructor
    public SummonerData(String id, Map<Long, MatchData> matchList, long lastUpdated, String name) {
        this.id = id;
        this.matchList = matchList;
        this.lastUpdated = lastUpdated;
        this.name = name;
    }

    public SummonerData(Summoner summoner) {
        this.id = summoner.getId();
        this.summoner = summoner;
        this.name = summoner.getName();
        MatchHistory history = Orianna.matchHistoryForSummoner(summoner)//.withStartTime(new DateTime(1571385869017L))
                .withSeasons(Season.SEASON_9)
                .withQueues(Queue.RANKED_SOLO)
                .get();
        this.matchList = new HashMap<>();
        this.addMatches(history);
        Date d = new Date();
        lastUpdated = d.getTime();
    }
    private void addMatches(MatchHistory history) {
        int count = 1;
        for (Match m : history) {
            System.out.println(count);
            count++;
            if(!m.isRemake() &&  !(m.getDuration().toStandardMinutes().getMinutes() < 10)) {
                this.matchList.put(m.getId(), new MatchData(m));
            }
            else {
                System.out.println("Remove match " + m.getId() + " " + m.isRemake() + " " + m.getDuration().toStandardMinutes().getMinutes());
            }
        }
    }
}
