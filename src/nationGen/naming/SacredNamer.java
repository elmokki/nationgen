package nationGen.naming;


import com.elmokki.Generic;
import nationGen.NationGenAssets;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;



public class SacredNamer {
	

	List<NamePart> parts = new ArrayList<NamePart>();
	List<NamePart> bases = new ArrayList<NamePart>();
	List<NamePart> combases = new ArrayList<NamePart>();
	List<NamePart> eliteparts = new ArrayList<NamePart>();
	private Random r;
	private NationGenAssets assets;
	
	ChanceIncHandler chandler = null;

	public SacredNamer(NationGenAssets assets)
	{
	    this.assets = assets;
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
		return u.tags.containsName("elite");
	}
	
	public void nameSacreds(Nation n)
	{
		chandler = new ChanceIncHandler(n);
		parts = assets.miscnames.get("defaultparts");
		bases = assets.miscnames.get("defaultbases");
		combases = assets.miscnames.get("defaultcommandernames");
		eliteparts = assets.miscnames.get("defaulteliteparts");

		this.r = new Random(n.random.nextInt());
		
		n.selectTroops("sacred").forEach(u -> name(u, n));
		n.selectTroops("montagsacreds").forEach(u -> name(u, n));
		n.selectTroops().filter(u -> u.tags.containsName("elite")).forEach(u -> name(u, n));
		n.selectTroops("montagtroops").filter(u -> u.tags.containsName("elite")).forEach(u -> name(u, n));
	}
	
	private void name(Unit u, Nation n) {
		// #forcedname
		if(Generic.getAllUnitTags(u).containsName("forcedname")) {
			u.name.setType(Generic.getAllUnitTags(u).getString("forcedname").orElseThrow());
			getMatchingCom(u, n).ifPresent(com -> Generic.getAllUnitTags(com).getString("forcedname").ifPresent(com.name::setType));
		} else {
			u.name = nameSacred(u, n);
			getMatchingCom(u, n).ifPresent(com -> {
				NamePart part = chandler.handleChanceIncs(u, combases).getRandom(n.random);
				com.name = u.name.getCopy();
				com.name.type = part.getCopy();
			});
		}
	}
	
	private Optional<Unit> getMatchingCom(Unit u, Nation n)
	{
		return n.selectCommanders("commander")
			.filter(com -> isSacred(u) && isSacred(com)) // holy
			.filter(com -> isElite(u) && isElite(com)) // elite
			.filter(com -> com.getSlot("weapon").id.equals(u.getSlot("weapon").id))
			.filter(com -> com.getSlot("armor").id.equals(u.getSlot("armor").id))
			.findFirst();
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
				
				NamePart part = chandler.handleChanceIncs(u, parts).getRandom(n.random);
				
				boolean suffix = r.nextBoolean();
				if((suffix && !part.tags.containsName("notsuffix")) || part.tags.containsName("notprefix"))
					name.suffix = part.getCopy();
				else
					name.prefix = part.getCopy();
		
			
			}
			else if(num == 0) // Base
			{
				NamePart part = chandler.handleChanceIncs(u, bases).getRandom(n.random);
				name.type = part.getCopy();
			}
			
			num--;
			changes++;
		}
		return name;	
	}
	


}
