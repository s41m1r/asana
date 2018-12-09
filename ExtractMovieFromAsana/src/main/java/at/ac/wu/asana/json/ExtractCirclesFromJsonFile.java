package at.ac.wu.asana.json;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class ExtractCirclesFromJsonFile {

	public static void main(String[] args) {

		JsonParser jsonParser = new JsonParser();
		String inputFile = "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/Data/data-with-deleted-roles.json";

		JsonReader reader;
		try {
			reader = new JsonReader(new FileReader(inputFile));
			
			JsonElement jsonTree = jsonParser.parse(reader);
			
			JsonObject jsonObject = null;
			
			if(jsonTree.isJsonPrimitive())
				System.out.println("Root is jsonPrimitive");
			
			if(jsonTree.isJsonArray()){
				System.out.println("Root is array.");
				JsonArray array = jsonObject.getAsJsonArray();
				for (JsonElement jsonElement : array) {
					System.out.println(jsonElement);
				}
			}
				
			if(jsonTree.isJsonObject()) {
				System.out.println("Root is jsonObject.");
				jsonObject = jsonTree.getAsJsonObject();
				
				JsonElement e = jsonObject.get("circle");
				
				if(jsonTree.isJsonArray()){
					System.out.println("it is an array!");
					JsonArray array = e.getAsJsonArray();
					for (JsonElement elem : array) {
						System.out.println(elem);
					}
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
