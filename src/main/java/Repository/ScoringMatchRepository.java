package Repository;

import RoleML.ScoringMatchData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ScoringMatchRepository extends MongoRepository<ScoringMatchData, Long> {
}
