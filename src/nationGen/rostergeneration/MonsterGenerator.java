package nationGen.rostergeneration;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.misc.ChanceIncHandler;
import nationGen.nation.Nation;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.ShapeShift;
import nationGen.units.Unit;

import java.util.List;
import java.util.Random;

public class MonsterGenerator {
	
	Nation n;
	NationGen ng;
	private NationGenAssets assets;
	
	public MonsterGenerator(Nation n, NationGen ng, NationGenAssets assets)
	{
		this.n = n;
		this.ng = ng;
		this.assets = assets;
	}
	
	public Unit generateMonster()
	{
		Race r = new Race(ng);
		r.unitcommands.clear();
		r.name = "Monster";
		r.visiblename = "Monster";
		ChanceIncHandler chandler = new ChanceIncHandler(n, "monstergen");
		
		List<ShapeShift> poses = ChanceIncHandler.retrieveFilters("monsters", "default_monsters", assets.monsters, null, n.races.get(0));
		
		if(poses.size() == 0)
		{
			System.out.println("No monsters for " + n.races.get(0).name + ". Try #monsterchance 0 if this race is not supposed to receive monsters.");
			return null;
		}
		
		Random rand = new Random(n.random.nextInt());
		
		Pose pose = new Pose(ng);
		
		Unit templateUnit = new Unit(ng, n.races.get(0), pose);  // Set a dummy unit with race[0] so the monster will know where to get its themeincs
		ShapeShift p = chandler.handleChanceIncs(templateUnit, poses).getRandom(rand);
	
		
		ShapeChangeUnit u = new ShapeChangeUnit(ng, assets, r, pose, null, p);
		
		double chance = p.tags.getDouble("caponlychance").orElse(0.95);
		
		if(n.random.nextDouble() < chance)
			u.caponly = true;

		p.tags.getInt("invariantid").ifPresent(id -> {
			u.id = id;
			u.invariantMonster = true;
			u.name.setType(p.name);  // Give the unit the same name as the pose for documentation purposes
		});
		
		u.polish(n.nationGen, n);
		
		return u;
		
	}
}
