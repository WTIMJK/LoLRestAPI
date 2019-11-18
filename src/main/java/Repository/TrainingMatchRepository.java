package Repository;

import RoleML.TrainingMatchData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrainingMatchRepository extends MongoRepository<TrainingMatchData,Long> {

}
