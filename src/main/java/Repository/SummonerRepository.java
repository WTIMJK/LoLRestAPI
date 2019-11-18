package Repository;

import Orianna.SummonerData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface SummonerRepository extends MongoRepository<SummonerData, String> {
    @Query("{ '_id' : ?0 }")
    Optional<SummonerData> findById(String id);
    boolean existsById(String id);
}
