package com.elmokki;


import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.Theme;
import nationGen.items.Item;
import nationGen.misc.Tags;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class Generic {
	
	
	public static final Set<String> LEADERSHIP_LEVELS = Set.of(
		"no",
		"poor",
		"ok",
		"good",
		"expert",
		"superior"
	);
	
	public static <E extends Entity> E getRandom(List<E> list, Random r)
	{
		double max = 0;
		double rand = 0;
		for(E e : list)
			max = max + e.basechance;
		
		rand = r.nextDouble() * max;
		
		double current = 0;
		
		for(E e : list)
		{
			current += e.basechance;
			if(current >= rand)
				return e;
		}
		
		
		return null;
		
		
	}
	
	public static List<String> parseArgs(String str)
	{
		return parseArgs(str, "\"");
	}
	
	
	
	public static Tags getAllUnitTags(Unit u)
	{
		Tags tags = new Tags();
		tags.addAll(u.race.tags);
		tags.addAll(u.pose.tags);
		for(Filter f : u.appliedFilters)
			tags.addAll(f.tags);
		for(Theme t : u.race.themefilters)
			tags.addAll(t.tags);
		for(Item i : u.slotmap.values())
			if(i != null)
				tags.addAll(i.tags);
		
		return tags;
	}
	
	public static Tags getAllNationTags(Nation n)
	{
		Tags tags = new Tags();

		if(n.races.get(0) != null)
		{
			tags.addAll(n.races.get(0).tags);
			for(Theme t : n.races.get(0).themefilters)
				tags.addAll(t.tags);
		}
		for(Filter f : n.nationthemes)
			tags.addAll(f.tags);
		
		return tags;
	}
	
	public static boolean isNumeric(String str)
	{
		try
		{
			@SuppressWarnings("unused")
			double d = Double.parseDouble(str);
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
	public static String listToString(List<String> list, String separator, String multiwordid)
	{
		return list.stream()
				.map(str -> (str.contains(" ") && list.size() > 1)
						? (multiwordid + str + multiwordid)
						: str)
				.collect(Collectors.joining(separator + " "));
	}
	
	public static String listToString(List<String> list, String separator)
	{
		return listToString(list, separator, "");
	}
	
	
	public static String listToString(List<String> list)
	{
		return listToString(list, "");
	}
	
   public static boolean containsLongBitmask(long bitmask, long wanted)
   {
	   return((bitmask & wanted) != 0);
   }
	   
   public static boolean containsBitmask(int bitmask, int wanted)
   {
	   return((bitmask & wanted) != 0);
   }
		
   
   public static int AmountOfVariables(int bitmask)
   {
	   return Integer.bitCount(bitmask);
   }
   
   
   
   public static String plural(String line)
   {
  
   	String end = "";
   	if(line.indexOf(" of ") != -1)
   	{
   		end = line.substring(line.indexOf(" of "), line.length());
   		line = line.substring(0, line.indexOf(" of "));
   	}
   	
   	if(line.toLowerCase().endsWith("s") || line.toLowerCase().endsWith("x") || line.toLowerCase().endsWith("sh"))
   		line = line + "es";
   	else if(line.toLowerCase().endsWith("y"))
   		line = line.substring(0, line.length() - 1) + "ies";
   	else if(line.toLowerCase().endsWith("man") && !line.toLowerCase().endsWith("human"))
   		line = line.substring(0, line.length() - 3) + "men";
   	else if(line.toLowerCase().endsWith("samurai") || line.toLowerCase().endsWith("vanir") || line.toLowerCase().endsWith(" sheep") || line.toLowerCase().endsWith(" fish"))
   	{
   		/*do nothing*/
   	}
   	else if(line.toLowerCase().endsWith("van"))
   	{
   		line = line + "ir";
   	}
   	else if(line.toLowerCase().endsWith("vaetti"))
   		line = line + "r";
   	else if(line.toLowerCase().endsWith("avvite"))
   		line = line.substring(0, line.length() - 3) + "im";
   	else if(line.toLowerCase().endsWith("siddhe") || line.toLowerCase().endsWith("sidhe"))
   	{
   		// do nothing
   	}
   	else
   		line = line + "s";
   	

   	
   	return line + end;
   }
		
	public static String capitalizeFirst(String s)
	{
		if(s.length() < 2)
			return s.toUpperCase();
		
		String string = "";
		string = (s.substring(0, 1).toUpperCase() + s.substring(1));
		return string;
	}
	
	private static final Set<String> DO_NOT_CAPITALIZE = Set.of("of", "the");
	
	public static String capitalize(String s)
	{
		
		if(s.length() <= 1)
			return s.toUpperCase();
		
		return Arrays.stream(s.split("\\s+"))
			.map(str -> DO_NOT_CAPITALIZE.contains(str)
				? str
				: str.length() <= 1
					? str.toUpperCase()
					: (str.substring(0, 1).toUpperCase() + str.substring(1)))
			.collect(Collectors.joining(" "));
	}
	
	public static List<String> parseArgs(String str, String separator)
	{
		return parseArgs(str, separator, false);
	}
	/**
	 * Parses args from string. Separator is space by default, but separator arg
	 * Overrides, ie string DERP "HERP DURP" would result in args 
	 * DERP and HERP DURP.
	 * @param str
	 * @param separator
	 * @return
	 */
	public static List<String> parseArgs(String str, String separator, boolean leaveseparator)
	{
		List<String> newlist = new ArrayList<>();
		
		int offset = 0;
		if(leaveseparator)
			offset = 1;
		
		str = str.trim();
		
		while(str.length() > 0)
		{
			boolean done = false;
			if(str.startsWith(separator))
			{
				for(int i = 1; i < str.length(); i++)
				{
					//System.out.println(i + ": " + str.charAt(i));
					if(("" + str.charAt(i)).equals(separator))
					{
						// Found next separator
						newlist.add(str.substring(1 - offset, i + offset));
						str = str.substring(i + 1);

						done = true;
						break;
					}	
				}
			}
			
			if(!done)
			{
				int index = str.indexOf(" ");
				if(index == -1)
					index = str.length();
				
				newlist.add(str.substring(0, index).trim());
				str = str.substring(index);
				
			}
			
			str = str.trim();
		}
		
		return newlist;
				
	}
	
	public static boolean containsSpace(String str) {
		for (char c : str.toCharArray()) {
			if (Character.isWhitespace(c)) {
				return true;
			}
		}
		return false;
	}
	
	public static String quote(String arg, char quote) {
		return quote + arg.replaceAll("\\\\", "\\\\").replaceAll("" + quote, "\\" + quote) + quote;
	}
}
