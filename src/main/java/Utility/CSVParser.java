package Utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CSVParser {
    private List<Map<String, String>> records;
    public CSVParser (String path, int columns) {
        String line = "";
        String cvsSplitBy = ",";
        int lineNum = 0;
        String[] keySet = new String[columns];
        List<Map<String, String>> records = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] splitLine = line.split(cvsSplitBy);
                if (lineNum==0) {
                    keySet = splitLine;
                }
                else {
                    Map<String, String> record = new HashMap<>();
                    for (int i = 0; i < splitLine.length; i++) {
                        record.put(keySet[i], splitLine[i]);
                    }
                    records.add(record);
                }
                lineNum++;
            }
            this.records = records;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public List<Map<String, String>> getRecords() {
        return records;
    }
}
