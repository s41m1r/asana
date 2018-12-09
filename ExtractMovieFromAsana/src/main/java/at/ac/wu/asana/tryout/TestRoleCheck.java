package at.ac.wu.asana.tryout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TestRoleCheck {

	public static void main(String[] args) {

		String input = "☺ Demand Roles";
		String input2 = "⌘ Marketplace Governance";

		System.out.println(input.startsWith("☺"));
		System.out.println(input2.startsWith("☺"));

		try {
			BufferedReader br = new BufferedReader(new FileReader("/home/saimir/ownCloud/PhD/Collaborations/Waldemar/API/projectNames.txt"));
			String line;
			System.out.println("input"+"\t"+"isCircle(input)");
			
			while((line=br.readLine())!=null)
				System.out.println(line+"\t"+isCircle(line));
			
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private static boolean isCircle(String input) {
		return startsWithSmiley(input) && endsWithRoles(input);
	}

	private static boolean startsWithSmiley(String input) {
		return input.startsWith("☺");
	}

	private static boolean endsWithRoles(String input) {
		return input.toLowerCase().endsWith("roles");
	}
}
