package nationGen.naming;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGenAssets;
import nationGen.entities.Filter;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;



public class SacredNamer {
	

	List<NamePart> parts = new ArrayList<NamePart>();
	List<NamePart> bases = new ArrayList<NamePart>();
	List<NamePart> combases = new ArrayList<NamePart>();
	List<NamePart> eliteparts = new ArrayList<NamePart>();
	private Random r;
	
	ChanceIncHandler chandler = null;

	public SacredNamer()
	{

	}
	

	
	private boolean isSacred(Unit u)
	{
		for(Command cmd : u.getCommands())
		{
			if(cmd.command.equals("#holy"))
			{
				return true;				
			}
			
		}
		return false;
	}
	
	private boolean isElite(Unit u)
	{
		return Generic.containsTag(u.tags, "elite");
	}
	
	public void nameSacreds(Nation n, NationGenAssets assets)
	{
		chandler = new ChanceIncHandler(n);
		parts = assets.miscnames.get("defaultparts");
		bases = assets.miscnames.get("defaultbases");
		combases = assets.miscnames.get("defaultcommandernames");
		eliteparts = assets.miscnames.get("defaulteliteparts");

		this.r = new Random(n.random.nextInt());
		
		


		
		List<Unit> toName = n.generateUnitList("sacred");
		toName.addAll(n.generateUnitList("montagsacreds"));
		for(Unit u : n.generateTroopList())
			if(Generic.containsTag(u.tags, "elite"))
			{
				toName.add(u);
			}
		
		for(Unit u : n.generateUnitList("montagtroops"))
			if(Generic.containsTag(u.tags, "elite"))
			{
				toName.add(u);
			}
		
		// #forcedname
		List<Unit> forcednames = new ArrayList<Unit>();
		for(Unit u : toName)
			if(Generic.containsTag(Generic.getAllUnitTags(u), "forcedname"))
				forcednames.add(u);
		toName.removeAll(forcednames);
		for(Unit u : forcednames)
		{
			u.name.setType(Generic.getTagValue(Generic.getAllUnitTags(u), "forcedname"));
			Unit com = getMatchingCom(u, n);
			if(com != null && Generic.containsTag(Generic.getAllUnitTags(com), "forcedname"))
				com.name.setType(Generic.getTagValue(Generic.getAllUnitTags(com), "forcedname"));

		}
		
		// Name units
		for(Unit u : toName)
		{

			

			u.name = nameSacred(u, n);
			Unit com = getMatchingCom(u, n);
			if(com != null)
			{


				NamePart part = Filter.getRandom(n.random, chandler.handleChanceIncs(u, combases));
				com.name = u.name.getCopy();
				com.name.type = part.getCopy();
			}
		
			
		}
		
		

	}
	
	private Unit getMatchingCom(Unit u, Nation n)
	{
		
		for(Unit com : n.generateComList("commander"))
		{
			
			
			boolean holy = isSacred(u) && isSacred(com);
			boolean elite = isElite(u) && isElite(com);
	
			
			if((holy || elite) && com.getSlot("weapon").id.equals(u.getSlot("weapon").id) && com.getSlot("armor").id.equals(u.getSlot("armor").id))
			{
				return com;
			}
		}
		

		return null;
	}
	
	private Name nameSacred(Unit u, Nation n)
	{

				
		Random r = this.r;
		int num = r.nextInt(2); // 0 or 1
		int changes = 0;
		
		Name name = new Name();
		
		while(changes < 2)
		{
			if(num == 1 || num == -1) // Part
			{


				
				NamePart part = Filter.getRandom(n.random, chandler.handleChanceIncs(u, parts));


				
				boolean suffix = r.nextBoolean();
				if((suffix && !part.tags.contains("notsuffix")) || part.tags.contains("notprefix"))
					name.suffix = part.getCopy();
				else
					name.prefix = part.getCopy();
		
			
			}
			else if(num == 0) // Base
			{
				NamePart part = Filter.getRandom(n.random, chandler.handleChanceIncs(u, bases));
				name.type = part.getCopy();
			}
			
			num--;
			changes++;
		}
		return name;	
	}
	


}
