package nationGen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;





import com.elmokki.Generic;

public class Settings {
	public enum SettingsType {
	    advancedDescs,
	    basicDescs,
	    separateSeeds,
	    seedsFromFile,
	    maxrestrictedperspell,
	    era,
	    sacredpower,
	    militiaMultiplier,
	    resUpperTreshold,
	    resUpperTresholdChange,
	    goldUpperTreshold,
	    goldUpperTresholdChange,
	    resLowerTreshold,
	    resLowerTresholdChange,
	    goldLowerTreshold,
	    goldLowerTresholdChange,
	    resMultiTreshold,
	    resMulti,
	    desiredBaseSize,
	    desiredRandom,
	    cavalryPower,
	    chariotPower,
	    rangedPower,
	    infantryPower,
	    newArmorChance,
	    debug,
	    drawPreview,
	    hidevanillanations;
	 }
    
	private class SettingEntry {
		public SettingsType key;
		public Double value;
		
		public SettingEntry(SettingsType k, Double v)
		{
            key = k;
            value = v;  
		}

	}

	
	public HashMap<SettingsType, Double> settings = new HashMap<SettingsType, Double>();
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
		exportvalues.add(new SettingEntry(SettingsType.era, 1.0));
		// 2: Late era
		exportvalues.add(new SettingEntry(SettingsType.era, 3.0));
		// 4: Powerful sacreds
		exportvalues.add(new SettingEntry(SettingsType.sacredpower, 2.0));
		// 8: Batshit insane sacreds
		exportvalues.add(new SettingEntry(SettingsType.sacredpower, 3.0));

		
		// Defaults
		defaultvalues.add(new SettingEntry(SettingsType.era, 2.0));
		defaultvalues.add(new SettingEntry(SettingsType.sacredpower, 1.0));

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
		
		// spells
		settings.put(SettingsType.maxrestrictedperspell, 8.0);
		
		// Debug	
		settings.put(SettingsType.debug, 0.0);
		
		// POWER (ranges from 1 to 3, integers only)
		settings.put(SettingsType.sacredpower, 1.0);
		
		settings.put(SettingsType.drawPreview, 0.0);

		settings.put(SettingsType.era, 2.0);
		settings.put(SettingsType.hidevanillanations, 1.0);

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
			
			
			settings.put(SettingsType.valueOf(args.get(0)), Double.parseDouble(args.get(1)));
		}
		
		file.close();
	}
	
	/**
	 * There is probably *way* more efficient ways to handle settings. But, here's a WORKING write.
	 * @throws IOException
	 */
	public void write() throws IOException
	{   
        int lineNum = 0;
        Path path = Paths.get(System.getProperty("user.dir") + "/settings.txt");
        List<String> lines = Files.readAllLines(path);
        boolean changes = false;
        for (String line : lines)
        {
            List<String> args = Generic.parseArgs(line);         
            
            if(args.size() < 2 || args.get(0).startsWith("-"))
            {
                lineNum++;
                continue;
            }
            
            SettingsType key = SettingsType.valueOf(args.get(0));
            String val = args.get(1);
            
            if (settings.containsKey(key) && Double.parseDouble(val) != settings.get(key))
            {
                lines.set(lineNum, key + " " + settings.get(key).toString());
                changes = true;
            }
            
            lineNum++;
        }
        
        if (changes)
        {
            Files.write(path, lines);
        }
	}
	
	
	public void put(SettingsType key, double value)
	{
		settings.put(key, value);
        try
        {
            write();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
	}
	
	public Double get(SettingsType key)
	{
		Double d = 0.0;
		if(settings.get(key) != null)
			d = settings.get(key);
		else
			System.out.println("SETTING " + key + " WAS NOT DEFINED!");
		
		return d;
	}
}
