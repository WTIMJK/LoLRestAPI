package OriannaMongo.RestAPI;

import Orianna.SummonerData;
import Orianna.MatchData;
import Repository.ScoringMatchRepository;
import Resource.MatchResource;
import RoleML.ScoringMatchData;
import RoleML.TrainingMatchData;
import Repository.SummonerRepository;
import Repository.MatchRepository;
import Repository.TrainingMatchRepository;
import Utility.CSVParser;
import com.merakianalytics.orianna.Orianna;
import com.merakianalytics.orianna.types.common.Region;
import com.merakianalytics.orianna.types.common.Season;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.MatchHistory;
import com.merakianalytics.orianna.types.core.match.Timeline;

import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.*;

@Configuration
@EnableMongoRepositories(basePackageClasses = {MatchResource.class, SummonerRepository.class, MatchRepository.class})
@ComponentScan(basePackageClasses = {SummonerData.class, MatchData.class})
public class AppConfig {
    @Bean
    public MatchResource matchResource() {
        return new MatchResource();
    }
//    @Bean
//    CommandLineRunner commandLineRunner(MatchResource matchResource,TrainingMatchData trainingMatchRepository, ScoringMatchRepository smr){//SummonerRepository summonerRepository, MatchRepository matchRepository) {
//        return new CommandLineRunner() {
//            @Override
//            public void run(String... strings) throws Exception {
//                long startTime = System.nanoTime();
//
//                //addSampleData(summonerRepository, matchRepository);
//                //testTrainingData(trainingMatchRepository);
////                scoreData(smr);
////                long endTime = System.nanoTime();
////                double duration = (endTime - startTime)/(1000000 * 1000 * 60.0);
////                System.out.println(duration + " " + "Execution Minutes");
//            }
//        };
//    }
    private void scoreData(ScoringMatchRepository smr) {
        Summoner s = Orianna.summonerNamed("WTHavoK").withRegion(Region.NORTH_AMERICA).get();
        MatchHistory history = Orianna.matchHistoryForSummoner(s).withStartTime(new DateTime(2019, 10, 17, 0, 0, 0))
                .withSeasons(Season.SEASON_9)
                .withQueues(com.merakianalytics.orianna.types.common.Queue.RANKED_SOLO)
                .get();
        Match m = history.get(history.size()-1);
        Timeline t = Orianna.timelineWithId(m.getId()).get();
        ScoringMatchData smd = new ScoringMatchData(t, m);
        System.out.println("SAVING " + m.getId());
        smr.save(smd);
    }
    private void testTrainingData(TrainingMatchRepository tmr) {
        int i = 1;
        Region region = Region.EUROPE_WEST;
        String path = "C:\\Users\\v-jakrah\\Desktop\\LoLTrainSet.csv";
        CSVParser parser = new CSVParser(path, 11);
        List<Map<String, String>> records = parser.getRecords();
        for (Map<String, String> record : records) {
            long gameId = Long.parseLong( record.get("gameId"));
            Timeline timeline = Orianna.timelineWithId(gameId).withRegion(region).get();
            Match game = Orianna.matchWithId(gameId).withRegion(region).get();
            if (game.isRemake() || game.getDuration().getStandardMinutes() < 12) {
                System.out.println("Remake or too short, SKIP. " + game.getDuration().getStandardMinutes());
                continue;
            }
            TrainingMatchData t = new TrainingMatchData(timeline, game, record);
            System.out.println(i + ". Writing " + game.getId());
            i++;
            tmr.save(t);
        }
    }
}
