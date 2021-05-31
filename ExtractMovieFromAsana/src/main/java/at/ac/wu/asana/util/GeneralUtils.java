package at.ac.wu.asana.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class GeneralUtils {
	
	public static String[] toStrObjArray(Object[] row) {
		String[] res = new String[row.length];
		for(int i=0; i<row.length; i++)
			res[i] = row[i].toString();
		return res;
	}
	
	public static <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }
	
    public static <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();

        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }
    
    public static <T> int countEntriesMap(Map<String, List<T>> weeklyChanges) {
    	int entries = 0;
    	Set<String> keys = weeklyChanges.keySet();
    	for (String key : keys) {
			List<T> list = weeklyChanges.get(key);
			entries+=list.size();
		} 	
		return entries;
    }
    
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

	public static List<String> readFromTextFile(String filename){
		List<String> list = new ArrayList<String>();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
        	
        	list = br.lines().collect(Collectors.toList());
        	
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return list;
	}
}
