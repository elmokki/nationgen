package nationGen.entities;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;

public class Entity {

	
	public NationGen nationGen;
	public String name = null;
	public double basechance = 1;
	public List<String> tags = new ArrayList<String>();
	public List<String> themes = new ArrayList<String>();

	public Entity(NationGen nationGen)
	{
		this.nationGen = nationGen;
	}

	
	public static <E extends Entity> List<E> readFile(NationGen nationGen, String file, Class<E> c) throws IOException
	{
		List<E> list = new ArrayList<E>();
		
		BufferedReader br = null;
		DataInputStream in = null;
		try
		{
			FileInputStream fstream = new FileInputStream(file);
			in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in));
		}
		catch(IOException e)
		{
			System.out.println("ERROR LOADING DATA FROM FILE " + file +".");
			return list;
		}
		

		
		String strLine;
		

		
		E instance = null;
		while ((strLine = br.readLine()) != null)   
		{
			
			if(strLine.trim().toLowerCase().startsWith("#new"))
				try
				{
					instance = c.getConstructor(NationGen.class).newInstance(nationGen);
				}
				catch(Exception e)
				{
					e.printStackTrace();
					System.out.println("Error initializing a new instance of " + c.getCanonicalName());
				}
				
			else if(strLine.trim().toLowerCase().startsWith("#end"))
			{
				if(instance != null)
				{
					instance.finish();
					list.add(instance);
				}
				instance = null;
				
				
			}
			else if(instance != null && strLine.startsWith("#"))
			{
				instance.handleOwnCommand(strLine);
			}

		}
			
		in.close();
		
		return list;
	}
	
	
	public <E extends Entity> void handleOwnCommand(String str)
	{
		List<String> args = Generic.parseArgs(str);
		if(args.get(0).equals("#id") || args.get(0).equals("#name"))
		{
			this.name = args.get(1);
		}
		else if(args.get(0).equals("#basechance") || args.get(0).equals("#chance"))
		{
			args.remove(0);
			this.basechance = Double.parseDouble(Generic.listToString(args));
		}
		else if(args.get(0).equals("#tag"))
		{
			// Legacy thing
			args.get(1).replaceAll("\'", "\"");
			
			args.remove(0);

			// Legacy thing EA20150605
			this.tags.add(Generic.listToString(args).replaceAll("\'", "\""));

		}
		else if(args.get(0).equals("#theme"))
		{
			// Legacy thing
			args.get(1).replaceAll("\'", "\"");
			
			args.remove(0);

			// Legacy thing EA20150605
			this.themes.add(Generic.listToString(args).replaceAll("\'", "\""));

		}
		else if(str.startsWith("#"))
		{
			this.tags.add(str.substring(1));

		}
		
	}


	
	public String toString()
	{
		return this.name;
	}

	public static <E extends Entity> E getRandom(Random r, List<E> set)
	{
		if(set == null || set.size() == 0)
			return null;
			
		List<E> items = new ArrayList<E>();
		items.addAll(set);
		
		if(items.size() == 0)
			return null;
		
		
		double max = 0;
		for(E i : items)
		{
			max += i.basechance;
		}

		
		if(max == 0)
			return null;
		
		double target = r.nextDouble() * max;
		double current = 0;

		E item = null;

		
		for(E i : items)
		{
			current = current + i.basechance;
			if(current >= target)
			{
				item = i;
				break;
			}
		}
		
		if(item == null)
			System.out.println("ITEM PICKING FAILURE. Strange, eh?");

			
		return item;
	}
	
	
	public static <E extends Entity> E getRandom(Random r, LinkedHashMap<E, Double> set)
	{
		if(set == null || set.size() == 0)
			return null;
			

		if(set.size() == 0)
			return null;
		
		
		double max = 0;
		for(E i : set.keySet())
		{
			max += set.get(i);
		}

		double target = r.nextDouble() * max;
		double current = 0;

		E item = null;

		
		for(E i : set.keySet())
		{
			current = current + set.get(i);
			if(current >= target)
			{
				item = i;
				break;
			}
		}
		
		if(item == null)
			System.out.println("ITEM PICKING FAILURE. Strange, eh?");

			
		return item;
	}
	
	
	/**
	 * Called when #end is read in file. Used at least for race to set visiblename to name if no #visiblename was found
	 */
	protected void finish()
	{
		
	}
}

