package RoleML;

import com.merakianalytics.orianna.types.common.EventType;
import com.merakianalytics.orianna.types.common.Region;
import com.merakianalytics.orianna.types.core.match.*;
import com.merakianalytics.orianna.types.core.staticdata.Item;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.awt.geom.Point2D;
import java.util.*;

import static RoleML.RiftCoordinates.*;

@Document(collection = "ScoringData")
@TypeAlias("ScoringData")
public class ScoringMatchData extends TrainingMatchData {
    public ScoringMatchData(Timeline timeline, Match match) {
        this.gameId = match.getId();
        this.region = match.getRegion();
        this.participantData = new HashMap<>();
        for (int i = 0; i < match.getParticipants().size(); i++) {
            TrainingParticipantData tpd = new TrainingParticipantData();
            this.participantData.put(i+1, tpd);
        }
        getParticipantData(timeline, match);
    }

}

