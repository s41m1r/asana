package at.ac.wu.asana.db.io;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.SessionFactory;

import at.ac.wu.asana.db.utils.DatabaseConnector;
import at.ac.wu.asana.model.StructuralDataChange;
import at.ac.wu.asana.util.GeneralUtils;

public abstract class ReadFromDB {

	public static List<StructuralDataChange> readFromDB(String dbname, String sql){
		
		// read the data
		SessionFactory sf = DatabaseConnector.getSessionFactory(dbname);
		org.hibernate.Session session = sf.openSession();
		
		List<StructuralDataChange> allEvents = new ArrayList<StructuralDataChange>();
			
		Query queryEvents = session.createSQLQuery((sql!=null)? sql: ""
				+ "SELECT * FROM `Springest`"
				+ "ORDER BY taskId, `timestamp`, `parentTaskId`");
		
		List<Object> results = queryEvents.list();
		List<StructuralDataChange> changeEvents = new ArrayList<StructuralDataChange>();
		
		for (Object e : results) {
			Object[] row = (Object[]) e;
			String[] str = GeneralUtils.toStrObjArray(row);
			StructuralDataChange sdc = StructuralDataChange.fromString(str);
			allEvents.add(sdc);
		}
		
		session.flush();
		session.close();
		sf.close();
		
		return allEvents;
	}
	
}
