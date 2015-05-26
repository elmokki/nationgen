package nationGen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import com.elmokki.Generic;

public class Settings {
	
	public HashMap<String, Double> settings = new HashMap<String, Double>();
	public HashMap<String, String> descs = new HashMap<String, String>();

	
	public Settings()
	{
		// Militia
		settings.put("militiaResMulti", -0.1333);
		settings.put("militiaLowResMulti", -0.125);
		settings.put("militiaResIntercept", 1.333);
		settings.put("militiaLowResIntercept", 2.5);
		
		settings.put("militiaGoldMulti", -0.1);
		settings.put("militiaLowGoldMulti", -1.666);
		settings.put("militiaGoldIntercept", 1.0);
		settings.put("militiaLowGoldIntercept", 16.66);
		
		// Items
		settings.put("weaponGenerationChance", 0.3);
		
		// POWER (ranges from 0 to 2, integers only)
		settings.put("sacredpower", 1.0);
		
		
		settings.put("era", 2.0);
		settings.put("hidevanillanations", 1.0);

        Scanner file;
		
		try {
			file = new Scanner(new FileInputStream(System.getProperty("user.dir") + "/settings.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		while(file.hasNextLine())
		{
			List<String> args = Generic.parseArgs(file.nextLine());
			
	
			if(args.size() < 2 || args.get(0).startsWith("-"))
				continue;
			
			
			settings.put(args.get(0), Double.parseDouble(args.get(1)));
		}
		
		file.close();
	}
	
	
	public void put(String name, double value)
	{
		settings.put(name, value);
	}
	
	public Double get(String name)
	{
		Double d = 0.0;
		if(settings.get(name) != null)
			d = settings.get(name);
		else
			System.out.println("SETTING " + name + " WAS NOT DEFINED!");
		
		return d;
	}
}
