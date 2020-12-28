package at.ac.wu.asana.tryout;

import java.util.Date;

import at.ac.wu.asana.csv.ExtractStructuralDataChanges;

public class ExtractProjectsAfterDate {

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		System.out.println(new Date() + " Extraction started.");
		ExtractAllProjects.extractProjects(args,"2020-05-05T00:00:00Z");
		System.out.println("All done in "+ ExtractStructuralDataChanges.getElapsedTime(System.currentTimeMillis(), start));
	}

}
