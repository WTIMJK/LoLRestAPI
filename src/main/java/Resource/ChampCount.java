package Resource;

import lombok.Data;

import java.util.Map;
@Data
public class ChampCount {
    Map<String, Integer> ChampCount;
    public ChampCount(Map<String, Integer> champs) {
        this.setChampCount(champs);
    }
}
