package at.ac.wu.asana.util;

import at.ac.wu.asana.csv.ReadInfoFromCSV;
import at.ac.wu.asana.csv.WriteUtils;
import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import com.vdurmont.emoji.EmojiManager;


import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CountEmojis {

    public static void main(String[] args) {
        List<String[]> rows = null;
        String filename = "/Users/saimir/ownCloud/Holacracy/Springest/Data/Data Extracted from DB/extracted20220220/tasksPerProject.csv";
        try {
            CSVReaderBuilder r = new CSVReaderBuilder(new FileReader(filename)).withCSVParser(new CSVParserBuilder()
                            .withSeparator(',')
                            .withIgnoreQuotations(true)
                            .build());
            CSVReader reader = r.build();
            rows = reader.readAll();
            reader.close();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }

        List<String[]> withEmoji = keepEmoji(rows);

        String outFile = "/Users/saimir/ownCloud/Holacracy/Springest/Data/Data Extracted from DB/extracted20220220/tasksPerProjects-Emoji.csv";
        WriteUtils.writeList(withEmoji,outFile);
    }

    private static List<String[]> keepEmoji(List<String[]> rows) {
        List<String[]> res = new ArrayList<String[]>();
        for (String[] row:
             rows) {
            if (EmojiManager.containsEmoji(row[1]))
                res.add(new String[]{row[0], row[1]});
        }
        return res;
    }

}
