package nationGen.naming;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import com.elmokki.Dom3DB;
import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Race;
import nationGen.nation.Nation;
import nationGen.units.Unit;



public class NameGenerator {

	
	List<String> magenouns = new ArrayList<String>();
	List<String> mageprefix = new ArrayList<String>();
	List<String> magesuffix = new ArrayList<String>();
	List<String> magerank = new ArrayList<String>();
	
	List<String> nationsyllable = new ArrayList<String>();
	List<String> nationsuffix = new ArrayList<String>();
	
	List<NamePart> siteadjectives = new ArrayList<NamePart>();
	List<NamePart> siteplaces = new ArrayList<NamePart>();
	List<NamePart> sitenouns = new ArrayList<NamePart>();
	
	private List<String> siteblacklist = new ArrayList<String>();
	
	
	public static String writeAsList(List<String> list, boolean capitalize, String lastseparator)
	{
		String str = "";
		for(int i = 0; i < list.size(); i++)
		{
			if(capitalize)
				str = str + NameGenerator.capitalize(list.get(i));
			else
				str = str + list.get(i);
			
			if(i < list.size() - 2)
				str = str + ", ";
			if(i == list.size() - 2)
				str = str + " " + lastseparator + " ";
		}
		
		return str;
	}
	
	public static String writeAsList(List<String> list, boolean capitalize)
	{
		return writeAsList(list, capitalize, "and");
	}
	
	public NameGenerator(NationGen nGen)
	{
		fillSiteBlackList(nGen.sites);
		
		
        Scanner file;

				
		// DERP DURP LOAD SITE NAMING STUFF
		
		try {
			file = new Scanner(new FileInputStream(System.getProperty("user.dir") + "/data/names/siteadjectives.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		while(file.hasNextLine())
		{
			NamePart part = NamePart.fromLine(file.nextLine(), nGen);
			if(part != null)
				this.siteadjectives.add(part);
		}
			

		
		file.close();
		
		try {
			file = new Scanner(new FileInputStream(System.getProperty("user.dir") + "/data/names/sitenouns.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		while(file.hasNextLine())
		{
			NamePart part = NamePart.fromLine(file.nextLine(), nGen);
			if(part != null)
				this.sitenouns.add(part);
		}
		
		file.close();

		
		try {
			file = new Scanner(new FileInputStream(System.getProperty("user.dir") + "/data/names/siteplaces.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		while(file.hasNextLine())
		{
			NamePart part = NamePart.fromLine(file.nextLine(), nGen);
			if(part != null)
				this.siteplaces.add(part);
		}
		
		file.close();

	}
	
	public void fillSiteBlackList(Dom3DB sites)
	{
		siteblacklist = sites.getColumn("name");
	}
	
	public String generateNationName(Race race, Nation n)
	{
		Random r = new Random();
		if(n != null)
			r = n.random;

		
		
		List<NationNamePart> longsyllables = new ArrayList<NationNamePart>();
		List<NationNamePart> shortsyllables = new ArrayList<NationNamePart>();
		List<NationNamePart> suffixes = new ArrayList<NationNamePart>();
		
		
		List<String> list = this.loadList(race.longsyllables);
		for(String str : list)
		{
			NationNamePart part = NationNamePart.fromLine(str);
			if(part != null)
				longsyllables.add(part);
		}
		
		list = this.loadList(race.shortsyllables);
		for(String str : list)
		{
			NationNamePart part = NationNamePart.fromLine(str);
			if(part != null)
				shortsyllables.add(part);
		}
		
		list = this.loadList(race.namesuffixes);
		for(String str : list)
		{
			NationNamePart part = NationNamePart.fromLine(str);
			if(part != null)
				suffixes.add(part);
		}

		String str = "";
		do
		{
			str = "";
			List<NationNamePart> name = new ArrayList<NationNamePart>();
			name.add(longsyllables.get(r.nextInt(longsyllables.size())));
			if(r.nextDouble() > 0.75)
			{
				NationNamePart newPart;
				do {
					newPart = shortsyllables.get(r.nextInt(shortsyllables.size()));
				} while(!newPart.canBeAfter(name.get(name.size() - 1)) || !name.get(name.size() - 1).canBeAfter(newPart));
				name.add(newPart);
			}
			NationNamePart newPart;
			do {
				newPart = suffixes.get(r.nextInt(suffixes.size()));
			} while(!newPart.canBeAfter(name.get(name.size() - 1)) || !name.get(name.size() - 1).canBeAfter(newPart));
			name.add(newPart);
			

			for(NationNamePart part : name)
				str = str + part.text;
		} while(!validateNationName(str));
		
		return capitalizeFirst(str);
		
	}
	
	public List<String> usedSites = new ArrayList<String>();
	public String getSiteName(Random r, int prim, int sec)
	{

		String primary = Generic.integerToPath(prim);
		String secondary = Generic.integerToPath(sec);
		
		String name = "";
		do
		{
			boolean primaryfilled = false;
			boolean noun = true;
			
			List<NamePart> parts = filterNonSuitableSiteNameParts(this.siteplaces, primary, secondary, true, true);
			NamePart place = parts.get(r.nextInt(parts.size()));
			
			if(place.elements.contains(primary))
				primaryfilled = true;
			
	
			if(r.nextDouble() >= 0.5)
			{
				parts = filterNonSuitableSiteNameParts(this.sitenouns, primary, secondary, true, true);
			}
			else
			{
				parts = filterNonSuitableSiteNameParts(this.siteadjectives, primary, secondary, true, true);
				noun = false;
			}
			
			
			if(primaryfilled)
				parts = filterOtherSiteNamePartsThan(parts, primary, false);
			else
				parts = filterOtherSiteNamePartsThan(parts, primary, true);
			
			if(parts.size() == 0)
			{
				if(noun)
				{
					noun = false;
					parts = filterNonSuitableSiteNameParts(this.siteadjectives, primary, secondary, true, true);
				}
				else
				{
					noun = true;
					parts = filterNonSuitableSiteNameParts(this.sitenouns, primary, secondary, true, true);
				}
				
				if(primaryfilled)
					parts = filterOtherSiteNamePartsThan(parts, primary, false);
				else
					parts = filterOtherSiteNamePartsThan(parts, primary, true);
			}
			
			if(parts.size() == 0 && noun)
			{
				noun = true;
				parts = filterNonSuitableSiteNameParts(this.sitenouns, primary, secondary, true, true);
			}
			else if(parts.size() == 0 && !noun)
			{
				noun = false;
				parts = filterNonSuitableSiteNameParts(this.siteadjectives, primary, secondary, true, true);
			}
				
			
			NamePart additional = parts.get(r.nextInt(parts.size()));
			
		
			if(noun)
			{
				name = capitalize(place.name) + " of " + capitalize(additional.name);
			}
			else
			{
				name = capitalize(additional.name) + " " + capitalize(place.name);
			}
		
		} while(usedSites.contains(name) || siteblacklist.contains(name));
		
		usedSites.add(name);
		return name;
	}
	
	
	private List<NamePart> filterOtherSiteNamePartsThan(List<NamePart> list, String element, boolean keeptag)
	{
		List<NamePart> newlist = new ArrayList<NamePart>();
		for(NamePart part : list)
		{	
			boolean suitable = !keeptag;
			for(String str : part.elements)
			{
				if(str.equals(element))
					suitable = keeptag;
			}
			if(suitable)
				newlist.add(part);
		}	
		
		return newlist;
	}
	
	private List<NamePart> filterNonSuitableSiteNameParts(List<NamePart> list, String primary, String secondary, boolean allowweak, boolean allowgeneric)
	{
		List<NamePart> newlist = new ArrayList<NamePart>();
		for(NamePart part : list)
		{		
			int matches = 0;
			for(String str : part.elements)
				if(str.equals(primary) || str.equals(secondary))
					matches++;
			
			if(matches >= part.minimumelements || (allowweak && part.weak))
				if(allowgeneric || part.minimumelements > 0)
					newlist.add(part);
			
		}	
		
		return newlist;
	}
	
	private List<String> loadList(String filename)
	{
		List<String> list = new ArrayList<String>();
		// ---------------------
        Scanner file;
		try {
			file = new Scanner(new FileInputStream(System.getProperty("user.dir") + filename));
		} catch (FileNotFoundException e) {
			System.out.println("ERROR WITH " + filename);
			e.printStackTrace();
			return list;
		}
		
		while(file.hasNextLine())
			list.add(file.nextLine());
		
		file.close();
		return list;

	}
	

	public static String addPreposition(String str)
	{
		if(isVowel(str.toLowerCase().charAt(0) + " "))
			return "an " + str;
		else
			return "a " + str;

	}
	
 	public static boolean isVowel(String str)
	{
		String[] vowels = { "a", "e", "i", "o", "u", "y" };
		for(String letter : vowels)
		{
			if(str.equals(letter))
				return true;
		}
		return false;
	}
	
	public boolean validateNationName(String name)
	{
		if(name.length() < 4)
			return false;
		
		char lastLetter = '%';
		for(int i = 3; i < name.length(); i++)
		{
			int consonantsInRow = 0;
			for(int j = i; j > i - 4; j--)
			{
				if(!isVowel(name.charAt(j) + ""))
					consonantsInRow++;
				else
					consonantsInRow = 0;
				
				if(consonantsInRow >= 4)
					return false;
			}
			
			int sameletters = 0;
			for(int j = i; j > i - 4; j--)
			{
			
				if(name.charAt(j) != lastLetter)
				{
					lastLetter = name.charAt(j);
					sameletters = 1;
				}
				else
				{
					sameletters++;
				}
				if(sameletters >= 3)
					return false;
			}
		}
		
		return true;
		
		
	}
	
	
	public static NamePart getTemplateName(Nation n, String role)
	{
		List<String> possiblenames = new ArrayList<String>();
		

		if(role.equals("infantry"))
		{
			possiblenames.add("Infantry");
			possiblenames.add("Soldier");
			possiblenames.add("Warrior");
		}
		else if(role.equals("mounted") || role.equals("cavalry"))
		{
			possiblenames.add("Cavalry");
			possiblenames.add("Rider");
		}
		else if(role.equals("chariot"))
		{
			possiblenames.add("Charioteer");
			possiblenames.add("Chariot");
		}
	

	
		Random r = n.random;
		NamePart part = new NamePart(n.nationGen);
		part.name = "UNNAMED";
		
		if(possiblenames.size() == 0)
			return part;
		
		part.name = capitalize(possiblenames.get(r.nextInt(possiblenames.size())));

		return part;
		
	}
	

	
	private static boolean sameName(Name name1, Name name2)
	{
		if(!name1.type.equals(name2.type))
			return false;
		
		if(name1.prefix != null && name2.prefix != null)
		{
			if(!name1.prefix.equals(name2.prefix))
				return false;
		}
		
		if((name1.prefix == null) != (name2.prefix == null))
			return false;
		
		return true;
	}

	public static void addHeavyLightPrefix(List<Unit> units)
	{
		for(Unit named : units)
		{
			for(Unit unit : units)
			{
				if(sameName(named.name, unit.name) && unit.getTotalProt() != named.getTotalProt())
				{
					
					if(unit.getTotalProt() < named.getTotalProt() && named.isHeavy())
					{
						String part = "Heavy";
						named.name.setPrefixprefix(part);
						break;
					}
					else if(unit.getTotalProt() > named.getTotalProt() && named.isLight())
					{
						String part = "Light";
						named.name.setPrefixprefix(part);
						break;
					}

				}
			}
		}
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
    	else if(line.toLowerCase().endsWith("samurai") || line.toLowerCase().endsWith("vanir") || line.toLowerCase().endsWith(" sheep") || line.toLowerCase().endsWith(" fish")){/*do nothing*/}
    	else if(line.toLowerCase().endsWith("van"))
    	{
    		line = line + "ir";
    	}
    	else
    		line = line + "s";
    	

    	
    	return line + end;
    }
    
	public static void addNationalPrefix(List<Unit> units, Nation n)
	{

		Random r = n.random;
		boolean suffix = r.nextBoolean();
		
		
		for(Unit u : units)
		{
			String part = "";
			
			if(!suffix)
			{
				part = n.name + n.nationalitysuffix;
				u.name.setPrefixprefix(part);

			}
			else
			{
				part = n.name;
				u.name.setSuffix(part);
			}	

		}
			
	}
	
	public static String capitalizeFirst(String s)
	{
		if(s.length() < 2)
			return s.toUpperCase();
		
		String string = "";
		string = (s.substring(0, 1).toUpperCase() + s.substring(1));
		return string;
	}
	
	public static String capitalize(String s)
	{
		
		if(s.length() < 2)
			return s.toUpperCase();
		
		String string = "";
		for(String str : s.split(" "))
		{
			str = str.trim();
			if(str.length() < 1)
				continue;
			
			if(str.equals("of") || str.equals("the"))
				string = string + " " + str;
			else
				string = string + " " + (str.substring(0, 1).toUpperCase() + str.substring(1));
		}
		string = string.substring(1);

		return string;
	}
	

	


	public String getNationalitySuffix(Nation n, String name) {
		
		
		List<String> possible = new ArrayList<String>();
		Random r = n.random;

		name = name.trim();
		if(!isVowel(name.charAt(name.length() - 1 ) +""))
		{
			possible.add("ine");
			possible.add("ian");
			possible.add("ian");
			possible.add("ic");
			possible.add("ese");
		}
		
		if(name.charAt(name.length() - 1) == 'a')
		{
			possible.add("n");
			possible.add("n");
			possible.add("n");
			possible.add("n");
			possible.add("n");
			possible.add("n");
		}
		else
		{				
			possible.add("an");
			possible.add("an");
		}
		
		if(name.charAt(name.length() - 1) != 'i')
			possible.add("ish");
		else
			possible.add("sh");
		
		if(name.charAt(name.length() - 1) == 'n')
			possible.add("e");
		
		
		return possible.get(r.nextInt(possible.size()));
	}
	
	
}
