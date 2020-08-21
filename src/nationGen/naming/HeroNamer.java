package nationGen.naming;


import com.elmokki.Dom3DB;
import com.elmokki.Generic;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Race;
import nationGen.misc.Command;
import nationGen.misc.FileUtil;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/*
 * HACK: for now, hero names are literally just nation names with the suffix trimmed off
 * write more interesting personal name generator later 
 */

public class HeroNamer extends Namer {

	protected Random random;
	

	List<String> nationsyllable = new ArrayList<>();
	
	
	List<NationNamePart> longsyllables = new ArrayList<>();
	List<NationNamePart> shortsyllables = new ArrayList<>();
	
	public HeroNamer(NationGen ng, NationGenAssets assets) {


	}

	
	public void execute(Nation n)
	{
		this.n = n;
		random = new Random(n.random.nextInt());
		loadNameData();
					
		for (Unit u : n.heroes)
		{			
			Random r = new Random();
			if(n != null)
				r = new Random(n.random.nextInt());
			
			String str;
			do
			{
				List<NationNamePart> name = new ArrayList<>();
				name.add(longsyllables.get(r.nextInt(longsyllables.size())));
				if(r.nextDouble() > 0.75)
				{
					NationNamePart newPart;
					do {
						newPart = shortsyllables.get(r.nextInt(shortsyllables.size()));
					} while(!newPart.canBeAfter(name.get(name.size() - 1)) || !name.get(name.size() - 1).canBeAfter(newPart));
					name.add(newPart);
				}
				
				str = name.stream().map(part -> part.text).collect(Collectors.joining());
				
			} while(!validateName(str));
			
			u.commands.add(Command.args("#fixedname", capitalizeFirst(str)));
		}
	}
	
	
	public void loadNameData()
	{		
		for(String str : FileUtil.readLines(n.races.get(0).longsyllables))
		{
			NationNamePart part = NationNamePart.fromLine(str);
			if(part != null)
				longsyllables.add(part);
		}
		
		for(String str : FileUtil.readLines(n.races.get(0).shortsyllables))
		{
			NationNamePart part = NationNamePart.fromLine(str);
			if(part != null)
				shortsyllables.add(part);
		}
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
	
	public boolean validateName(String name)
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
	
	
	public static boolean sameName(Name name1, Name name2)
	{
		if(!name1.type.toString().equals(name2.type.toString()))
			return false;
		
		if(name1.prefix != null && name2.prefix != null)
		{
			if(!name1.prefix.toString().equals(name2.prefix.toString()))
				return false;
		}
		
		if((name1.prefix == null) != (name2.prefix == null))
			return false;
		
		return true;
	}

	

	
	public static String capitalizeFirst(String s)
	{
		if(s.length() < 2)
			return s.toUpperCase();
		
		String string = "";
		string = (s.substring(0, 1).toUpperCase() + s.substring(1));
		return string;
	}
}
