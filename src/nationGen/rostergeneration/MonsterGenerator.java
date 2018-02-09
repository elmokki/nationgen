package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.misc.ChanceIncHandler;
import nationGen.nation.Nation;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.ShapeShift;
import nationGen.units.Unit;

public class MonsterGenerator {
	
	Nation n;
	NationGen ng;
	
	public MonsterGenerator(Nation n, NationGen ng)
	{
		this.n = n;
		this.ng = ng;
	}
	
	public Unit generateMonster()
	{
		Race r = new Race(ng);
		r.unitcommands.clear();
		r.name = "Monster";
		r.visiblename = "Monster";
		ChanceIncHandler chandler = new ChanceIncHandler(n, "monstergen");
		
		List<ShapeShift> poses = ChanceIncHandler.retrieveFilters("monsters", "default_monsters", ng.monsters, null, n.races.get(0));
		
		if(poses.size() == 0)
		{
			System.out.println("No monsters for " + n.races.get(0).name + ". Try #monsterchance 0 if this race is not supposed to receive monsters.");
			return null;
		}
		
		Random rand = new Random(n.random.nextInt());
		
		Pose pose = new Pose(ng);
		
		Unit templateUnit = new Unit(ng, n.races.get(0), pose);  // Set a dummy unit with race[0] so the monster will know where to get its themeincs
		ShapeShift p = ShapeShift.getRandom(rand, chandler.handleChanceIncs(templateUnit, poses, null));
	
		
		ShapeChangeUnit u = new ShapeChangeUnit(ng, r, pose, null, p);
		
		double chance = 0.95;
		if(Generic.containsTag(p.tags, "caponlychance"))
			chance = Double.parseDouble(Generic.getTagValue(p.tags, "caponlychance"));
		
		if(n.random.nextDouble() < chance)
			u.caponly = true;

		if(Generic.containsTag(p.tags, "invariantid"))
		{
			u.id = Integer.parseInt(Generic.getTagValue(p.tags, "invariantid"));
			u.invariantMonster = true;
			u.name.setType(p.name);  // Give the unit the same name as the pose for documentation purposes
		}
		
		u.polish(n.nationGen, n);
		
		return u;
		
	}
}
