package nationGen.naming;

import java.util.ArrayList;
import java.util.List;

import nationGen.nation.Nation;
import nationGen.units.Unit;


public class MageDescriber {
	
	
	public static String getCommonNoun(Nation n)
	{
		Name noun = getCommonName(n);
		

		String derp = "";
		if(noun.prefix != null)
			derp = noun.prefix.toString();
		if(noun.prefixprefix != null)
			derp = noun.prefixprefix.toString() + " " + derp;
		if(noun.suffix != null)
			derp = noun.suffix.toString();
		if(noun.suffixprefix != null && noun.prefix == null)
			derp = noun.suffixprefix.toString() + " " + derp;

		return derp.trim();
	}
	

	public static Name getCommonName(Nation n)
	{
		List<Unit> primaryMages = new ArrayList<Unit>();
		for(Unit u : n.generateComList())
			if(u.tags.contains("schoolmage 3"))
			{
				primaryMages.add(u);
			}
		
		
		List<Unit> secondaryMages = new ArrayList<Unit>();
		for(Unit u : n.generateComList())
			if(u.tags.contains("schoolmage 2"))
			{
				secondaryMages.add(u);
			}
		
		List<Unit> tertiaryMages = new ArrayList<Unit>();
		for(Unit u : n.generateComList())
			if(u.tags.contains("schoolmage 1"))
			{
				tertiaryMages.add(u);
			}
		
		List<Unit> compensationMages = new ArrayList<Unit>();
		for(Unit u : n.generateComList())
			if(u.tags.contains("extramage"))
			{
				compensationMages.add(u);
			}
		
		Name common = new Name(); 
		if(primaryMages.size() > 0)
		{
	
			common = primaryMages.get(0).name.getCopy();
			
			for(Unit u : primaryMages)
			{

				if(u.name.prefix == null || !u.name.prefix.equals(common.prefix))
					common.prefix = null;
				if(u.name.type == null || !u.name.type.equals(common.type))
					common.setType("mage");
				if(u.name.suffixprefix == null || !u.name.suffixprefix.equals(common.suffixprefix))
					common.suffixprefix = null;
				if(u.name.suffix == null || !u.name.suffix.equals(common.suffix))
					common.suffix = null;
				

			}
			
			if(common.suffix == null && common.suffixprefix != null)
				common.suffixprefix = null;
			
			if(primaryMages.size() > 1)
				return common;
			
			for(Unit u : secondaryMages)
			{

				if(u.name.prefix == null || !u.name.prefix.equals(common.prefix))
					common.prefix = null;
				if(u.name.type == null || !u.name.type.equals(common.type))
					common.setType("mage");
				if(u.name.suffixprefix == null || !u.name.suffixprefix.equals(common.suffixprefix))
					common.suffixprefix = null;
				if(u.name.suffix == null || !u.name.suffix.equals(common.suffix))
					common.suffix = null;
				

			}
			
			for(Unit u : tertiaryMages)
			{

				if(u.name.prefix == null || !u.name.prefix.equals(common.prefix))
					common.prefix = null;
				if(u.name.type == null || !u.name.type.equals(common.type))
					common.setType("mage");
				if(u.name.suffixprefix == null || !u.name.suffixprefix.equals(common.suffixprefix))
					common.suffixprefix = null;
				if(u.name.suffix == null || !u.name.suffix.equals(common.suffix))
					common.suffix = null;
				

			}
			

		}
		
		if(common.suffix == null && common.suffixprefix != null)
			common.suffixprefix = null;
		
		
		return common;
	}
}
