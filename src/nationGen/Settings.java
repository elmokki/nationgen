package nationGen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;





import com.elmokki.Generic;




public class Settings {
	
	private class SettingEntry {
		public String key;
		public Double value;
		
		public SettingEntry(String k, Double v)
		{
			key = k;
			value = v;
		}

	}

	
	public HashMap<String, Double> settings = new HashMap<String, Double>();
	public HashMap<String, String> descs = new HashMap<String, String>();
	
	public LinkedList<SettingEntry> exportvalues = new LinkedList<SettingEntry>();
	public LinkedList<SettingEntry> defaultvalues = new LinkedList<SettingEntry>();
	
	
	/**
	 * Fills exportvalues
	 */
	private void setExportValues()
	{
		
		// Settings
		
		// 1: Early era
		exportvalues.add(new SettingEntry("era", 1.0));
		// 2: Late era
		exportvalues.add(new SettingEntry("era", 3.0));
		// 4: Powerful sacreds
		exportvalues.add(new SettingEntry("sacredpower", 2.0));
		// 8: Batshit insane sacreds
		exportvalues.add(new SettingEntry("sacredpower", 3.0));

		
		// Defaults
		defaultvalues.add(new SettingEntry("era", 2.0));
		defaultvalues.add(new SettingEntry("sacredpower", 1.0));

	}
	
	
	/**
	 * Returns certain settings as an integer
	 *
	 * Here's the table of what each byte value is:
	 * 1: Early era
	 * 2: Late era
	 * 4: High sacred power
	 * 8: Batshit insane sacred power
	 * 
	 * @return
	 */
	public int getSettingInteger()
	{
		int integer = 0;
		
		Iterator<SettingEntry> expit = exportvalues.iterator();
		
		int i = 0;
		while(expit.hasNext())
		{
			SettingEntry e = expit.next();
			if(get(e.key).equals(e.value))
			{
				integer += Math.pow(2, i);
			}
			i++;
		}
		
		return integer;
	}
	
	/**
	 * Sets certain settings based on an integer
	 * @param integer
	 */
	public void setSettingInteger(int integer)
	{
		for(SettingEntry se : defaultvalues)
		{
			put(se.key, se.value);
		}
		
		Iterator<SettingEntry> expit = exportvalues.iterator();
		int i = 0;
		while(expit.hasNext())
		{
			SettingEntry e = expit.next();
			if(Generic.containsBitmask(integer, (int)Math.pow(2,i)))
			{
				put(e.key, e.value);
			}
			i++;
		}
	}
	
	
	
	public Settings()
	{
		setExportValues();

		// Militia
		settings.put("militiaResMulti", -0.1333);
		settings.put("militiaLowResMulti", -0.125);
		settings.put("militiaResIntercept", 1.333);
		settings.put("militiaLowResIntercept", 2.5);
		
		settings.put("militiaGoldMulti", -0.1);
		settings.put("militiaLowGoldMulti", -1.666);
		settings.put("militiaGoldIntercept", 1.0);
		settings.put("militiaLowGoldIntercept", 16.66);
		
		// Debug
		
		settings.put("debug", 0.0);

		// Items
		settings.put("weaponGenerationChance", 0.3);
		
		// POWER (ranges from 1 to 3, integers only)
		settings.put("sacredpower", 1.0);
		
		settings.put("drawPreview", 0.0);

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
