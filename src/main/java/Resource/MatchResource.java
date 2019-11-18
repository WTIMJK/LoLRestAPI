package Resource;

import Orianna.MatchData;
import Orianna.SummonerData;
import Orianna.ParticipantData;
import Repository.MatchRepository;
import Repository.SummonerRepository;
import com.merakianalytics.orianna.Orianna;
import com.merakianalytics.orianna.types.common.Platform;
import com.merakianalytics.orianna.types.common.Region;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.MatchHistory;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


//@RequestMapping("/rest/")
@RestController
public class MatchResource {
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private SummonerRepository summonerRepository;
    public MatchResource() {
        String apikey = "RGAPI-b20e23d2-869b-4de8-a2df-852e46769075";
        Orianna.setRiotAPIKey(apikey);
        Orianna.setDefaultRegion(Region.NORTH_AMERICA);
        Orianna.setDefaultPlatform(Platform.NORTH_AMERICA);
    }
    public MatchResource(MatchRepository matchRepository, SummonerRepository summonerRepository) {
        this.matchRepository = matchRepository;
        this.summonerRepository = summonerRepository;
    }
//    @GetMapping("/stats")
//    public void worstMatchupsByLane(@RequestParam(value = "summoner")String summonerName,
//                                    @RequestParam(value = "forceUpdate", defaultValue = "False") String forceUpdate) throws IOException, InterruptedException {
//        Summoner summoner = Orianna.summonerNamed(summonerName).get();
//        SummonerData summonerData = getSummonerData(summoner, Boolean.getBoolean(forceUpdate));
//    }
    @GetMapping("/")
    public Integer showInt(@RequestParam(value="num",defaultValue = "1") String num) {
        return Integer.parseInt(num);
    }
    @GetMapping("/test")
    public ChampCount showGames(@RequestParam(value="summoner", defaultValue="WTHavoK") String summonerName,
                          @RequestParam(value = "forceUpdate", defaultValue = "false") String forceUpdate) throws IOException, InterruptedException {
        Summoner summoner = Orianna.summonerNamed(summonerName).get();
        SummonerData summonerData = getSummonerData(summoner, Boolean.getBoolean(forceUpdate));
        Map<String, Integer> champCount = new HashMap<>();
        for (MatchData m : summonerData.getMatchList().values()) {
            String champion = "";
            for (ParticipantData p : m.getBlueTeam().values()) {
                if (p.getSummonerId().equals(summoner.getId())) {
                    champion = p.getChampion();
                    if (champCount.containsKey(champion)) {
                        champCount.put(champion, champCount.get(champion)+1);
                    }
                    else {
                        champCount.put(champion, 1);
                    }
                    break;
                }
            }
            if (champion.length() > 0) {
                for (ParticipantData p : m.getRedTeam().values()) {
                    if (p.getSummonerId().equals(summoner.getId())) {
                        champion = p.getChampion();
                        if (champCount.containsKey(champion)) {
                            champCount.put(champion, champCount.get(champion)+1);
                        }
                        else {
                            champCount.put(champion, 1);
                        }
                        break;
                    }
                }
            }
        }
        return new ChampCount(champCount);
    }

    private SummonerData getSummonerData(Summoner summoner, boolean doUpdate) throws IOException, InterruptedException {
        SummonerData summonerData;
        Optional<SummonerData> optionalSummonerData = summonerRepository.findById(summoner.getId());
        if (optionalSummonerData.isPresent()) {
            System.out.println(summoner.getName() + " Exists in db");
            summonerData = optionalSummonerData.get();
            if (doUpdate) {//update summoner
                updateUser(summonerData);
                this.summonerRepository.save(summonerData);
            }
        }
        else {
            System.out.println("Adding new user Data");
            summonerData = new SummonerData(summoner);
            addAndScoreNewMatches(summonerData.getMatchList().values());
            //need to update validated Role
            for (MatchData m : summonerData.getMatchList().values()) {
                Optional<MatchData> temp = this.matchRepository.findById(m.getId());
                if (temp.isPresent()) {
                    MatchData tempData = temp.get();
                    for (Map.Entry<Integer, ParticipantData> e : m.getParticipantData().entrySet()) {
                        e.getValue().setValidatedRole(
                                tempData.getParticipantData().get(
                                        e.getKey())
                                        .getValidatedRole());
                    }
                    m.setTeams();
                    matchRepository.save(m);
                }

            }
            this.summonerRepository.save(summonerData);
        }

        return summonerData;
    }
    private void updateUser(SummonerData summonerData) throws IOException, InterruptedException {
        summonerData.setLastUpdated(new Date().getTime());
        MatchHistory history = Orianna.matchHistoryForSummoner(
                summonerData.getSummoner())
                .withStartTime(new DateTime(summonerData.getLastUpdated()+1))
                .get();
        List<Match> matchList = new LinkedList<>();
        //TODO: add matches, EXCEPT BAD MATCHES
        for (Match m : history) {
            if (!m.isRemake() && m.getDuration().getStandardMinutes() > 10) {
                matchList.add(m);
            }
        }
        System.out.println("Found " + matchList.size() + " new matches");
        addAndScoreNewMatches(matchList);
        for (Match match : matchList) {
            Optional<MatchData> optionalMatchData = this.matchRepository.findById(match.getId());
            //if match is in database, add to summoner match list
            if (optionalMatchData.isPresent()) {
                MatchData m = optionalMatchData.get();
                m.setTeams();
                summonerData.getMatchList().put(m.getId(), m);
            }
        }
    }
    private void addAndScoreNewMatches(List<Match> history) throws IOException, InterruptedException {
        String toScorePath = "toScore.txt";
        FileWriter fileWriter = new FileWriter(toScorePath);
        PrintWriter printWriter = new PrintWriter(fileWriter);
//        for (Match m : history) {
//            if (this.matchRepository.findById(m.getId()).isEmpty()) { //not in repo
//                this.matchRepository.save(new MatchData(m));
//                printWriter.println(m.getId());
//            }
//        }
        printWriter.close();
        //execute python
        System.out.println("Start scoring " + history.size() + " games");
        boolean exitVal = runPython(toScorePath);
        System.out.println("End Scoring: " + exitVal);
        //python is done
    }
    private void addAndScoreNewMatches(Collection<MatchData> history) throws IOException, InterruptedException {
        String toScorePath = "toScore.txt";
        FileWriter fileWriter = new FileWriter(toScorePath);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        for (MatchData m : history) {
            if (!m.isRemake() &&  m.getDuration() > 10*60) {
                this.matchRepository.save(m);
                printWriter.println(m.getId());
            }
        }
        printWriter.close();
        //execute python
        System.out.println("Start scoring " + history.size() + " games");
        boolean exitVal = runPython(toScorePath);
        System.out.println("End Scoring: " + exitVal);
        //python is done
    }
    private boolean runPython(String toScorePath) throws IOException, InterruptedException {
        String MLPath = "Python\\scoring.py";
        List<String> args = new LinkedList<>();
        args.add("python");
        args.add(MLPath);
        args.add(toScorePath);
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectErrorStream(true);
        builder.redirectOutput(new File("this.txt"));
        Process process = builder.start();
        return process.waitFor(5, TimeUnit.MINUTES);
    }
}
