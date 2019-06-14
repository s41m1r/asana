package at.ac.wu.asana.util;

public abstract class GeneralUtils {
	
	public static String[] toStrObjArray(Object[] row) {
		String[] res = new String[row.length];
		for(int i=0; i<row.length; i++)
			res[i] = row[i].toString();
		return res;
	}
}
