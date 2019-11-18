package OriannaMongo.RestAPI;

import Orianna.MatchData;
import Orianna.SummonerData;
import Repository.MatchRepository;
import Repository.SummonerRepository;
import Resource.MatchResource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Set;


@SpringBootApplication
public class RestApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestApiApplication.class, args);
	}



}
