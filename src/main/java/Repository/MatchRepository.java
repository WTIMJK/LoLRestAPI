package Repository;

import Orianna.MatchData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface MatchRepository extends MongoRepository<MatchData, Long> {
    boolean existsById(Long id);
    Optional<MatchData> findById(Long id);
    List<MatchData> findAll();
}
