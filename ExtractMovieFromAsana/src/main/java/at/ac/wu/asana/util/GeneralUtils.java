package at.ac.wu.asana.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
}
