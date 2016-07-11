package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Drawing;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.MagicFilter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.magic.RandomEntry;
import nationGen.magic.MageGenerator;
import nationGen.magic.MagicPattern;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class HeroGenerator {

	SacredGenerator sacGen;
	MageGenerator mageGen;
	Nation n;
	NationGen ng;
	public HeroGenerator(NationGen g, Nation n) {
		this.mageGen = new MageGenerator(g, n);
		this.sacGen = new SacredGenerator(g, n);
		this.n = n;
		this.ng = g;
	}
	

	
	public List<Unit> generateHeroes(int count)
	{
		List<Unit> units = new ArrayList<Unit>();
		int ticks = 0;
		while(units.size() < count && ticks < 100)
		{	
			Unit u = null;
			if(n.random.nextDouble() < 0.5)
				u = (generateMageHero(false));
			else
				u = (generateHero(false));
			
			if(u != null)
				units.add(u);
		}
		
		// Remove upkeep
		for(Unit u : units)
		{
			u.commands.add(new Command("#gcost", "*0"));
		}
		
		return units;
	}
	
	private void addHeroFilter(Unit u)
	{
		// Add a filter to g
		Filter tf = new Filter(ng);
		
		
		tf.name = "Hero";
	

		// Remove recruitment filters
		tf.themeincs.add("thisitemtheme recruitment *0");
		tf.themeincs.add("thisitemtheme trooponly *0");


		

		u.appliedFilters.add(tf);
	}
	
	private Unit generateHero(boolean multihero)
	{
		SacredGenerator sacGen = new SacredGenerator(ng, n);
		ChanceIncHandler chandler = new ChanceIncHandler(n, "herogen");
		sacGen.setIdentifier("herogen");

		int power = 5;
		if(multihero)
			power += n.random.nextInt(6); // +0-5;
		else
			power += 3 + n.random.nextInt(8); // +3-10;

		Race r = getRace("mage");
		
		boolean sacred = n.random.nextBoolean();
		Pose p = sacGen.getPose(true, power, r);
		
		double epicchance = n.random.nextDouble() * 0.5 + power * 0.25 + 0.25;

		Unit u = sacGen.unitGen.generateUnit(r, p);
		this.addHeroFilter(u);

		Unit hero = sacGen.getSacredUnit(u, power, sacred, epicchance);

		if(n.races.contains(hero.race))
			hero.color = n.colors[1];
		else
			hero.color = n.colors[2];
		
		if(n.random.nextBoolean())
			hero.color = Drawing.getColor(n.random);

		if(n.random.nextBoolean())
		{
			int tier = 1;
			if(n.random.nextBoolean())
				tier = 2;
			
			MageGenerator mg = new MageGenerator(ng, n);
			mg.setIdentifier("herogen");
			
			MagicPattern pat = Entity.getRandom(n.random, chandler.handleChanceIncs(hero, MageGenerator.getPatternsOfLevel(mg.possiblePatterns, tier)));
			List<Integer> prio = this.getPrio(n.races.contains(hero.race));
			MagicFilter f = new MagicFilter(ng);
			f.prio = prio;
			f.pattern = pat;
			f.tags.add("do_not_show_in_descriptions");
			hero.appliedFilters.add(f);
		}
		
		hero.name.setType("Hero");
		if(!multihero)
			hero.commands.add(new Command("#unique"));
		
		return hero;
	}
	

	
	private Unit generateMageHero(boolean multihero)
	{

		
		Race r = getRace("mage");
		

		ChanceIncHandler chandler = new ChanceIncHandler(n, "herogen");
		MageGenerator mg = new MageGenerator(ng, n);
		mg.chandler = chandler;

		int tier = 3;
		if(multihero && n.random.nextDouble() < 0.5)
			tier = 2;
		
		Unit hero = mg.generateBases("mage", r, 1, tier).get(0);
		if(hero == null)
			return null;
		
		this.addHeroFilter(hero);
		
		if(n.races.contains(r))
			hero.color = n.colors[1];
		else
			hero.color = n.colors[2];
		
		if(n.random.nextBoolean())
			hero.color = Drawing.getColor(n.random);
		
		MagicPattern p = generatePattern(multihero);
		List<Integer> prio = getPrio(n.races.contains(r));
		
		MagicFilter f = new MagicFilter(ng);
		f.prio = prio;
		f.pattern = p;
		
		hero.appliedFilters.add(f);
		
		List<Unit> heroes = new ArrayList<Unit>();
		heroes.add(hero);
		
		int power = 2;
		if(!multihero)
			power += 2 + n.random.nextInt(7); // 2-8;
		else
			power +=  n.random.nextInt(5); // 0-4;
		
		String[] defaults = {"default_magefilters", "default_magefilters_shapeshift", "default_magefilters_strongdefence"};
		mg.applyFilters(heroes, power, defaults, "magefilters");
		
		mg.equipBase(hero, tier);
		
		hero.name.setType("Hero");
		if(!multihero)
			hero.commands.add(new Command("#unique"));
		return hero;
	}
	
	
	private MagicPattern generatePattern(boolean multihero)
	{
		MagicPattern p = new MagicPattern(ng);


		int picks = 4;
		if(multihero)
			picks += n.random.nextInt(5); // +0-4
		else
			picks += 2 + n.random.nextInt(5); // +2-6
		int maxpicks = picks;
		
		int step = 0;
		int level = 4;
		if(multihero)
			level--;
		if(multihero && n.random.nextBoolean())
		{	
			level--;
		}
		else if(!multihero && n.random.nextBoolean())
		{
			level--;
			if(picks > 10)
				picks = 10;
		}
	
		while(picks > 0)
		{
			p.picks.put((int)Math.pow(2, step), level);

			

			
	
			
			picks -= level;
			step++;
			
			if(picks - level < 0)
				level = picks;
			
			if(n.random.nextDouble() >  picks / maxpicks && level > 1 && ((level - 1) / picks > 0.25 || picks < 4))
				level--;
			else if(level == 4 && n.random.nextDouble() < 0.8)
				level--;
			
			if(n.random.nextDouble() > picks / maxpicks && multihero)
				break;
		}

		while(picks > 0)
		{
			int randoms = 15;
			if(n.random.nextDouble() < 0.75)
				randoms -= 8;
			if(n.random.nextDouble() < 0.75)
				randoms -= 4;
			
			
			double amount = 1;
			if(picks > 1 && n.random.nextDouble() < 0.33)
			{
				amount++;
				if(picks > 2 && n.random.nextDouble() < 0.33)
				{
					amount++;
				}
			}
			
			if(amount == 1 && n.random.nextDouble() < 0.1)
				amount = 0.5;
			
			RandomEntry e = new RandomEntry(randoms, amount);
			p.randoms.add(e);

			
			picks -= amount;
		}


		return p;
	}
	
	
	private List<Integer> getPrio(boolean primaryrace)
	{
		List<List<Unit>> all = n.getMagesInSeparateLists();
		List<Unit> primaries = all.get(0);
		
		double[] allpaths = MageGenerator.getAllPicks(primaries, false);
		
		double highest = 0;
		for(double i : allpaths)
			if(i > highest)
				highest = i;
		
		
		List<Integer> prio = new ArrayList<Integer>();
		if(primaryrace)
		{
			for(int i = 0; i < 8; i++)
				if(allpaths[i] == highest)
					prio.add(i);
		}
		
		List<Filter> derp = new ArrayList<Filter>();
		for(int i = 0; i < 8; i++)
		{
			if(!prio.contains(i))
			{
				Filter f = new Filter(ng);
				f.name = i + "";
				derp.add(f);
			}
		}
		
		while(derp.size() > 0)
		{
			Filter f = Entity.getRandom(n.random, derp);
			derp.remove(f);
			prio.add(Integer.parseInt(f.name));
		}
		
		return prio;
	}
	
	private Race getRace(String role)
	{
		double secamount = n.percentageOfRace(n.races.get(1));
		ChanceIncHandler chandler = new ChanceIncHandler(n, "herogen");
		
		Race r = null;
		while(r == null)
		{
			if(n.random.nextDouble() < 0.05 && ng.races.size() > 2)
			{
				List<Race> races = new ArrayList<Race>();
				races.addAll(ng.races);
				races.removeAll(n.races);
				r = Entity.getRandom(n.random, chandler.handleChanceIncs(races));
				
				//System.out.println(r + " " + role + " " + hasRole(r, role));
				if(!hasRole(r, role))
					r = null;
				
			}
			else
			{
				if(n.random.nextDouble() < secamount * 0.75 && n.races.size() > 1)
				{
					r = n.races.get(1);
				}
				else
				{
					r = n.races.get(0);
				}
				
				if(!hasRole(r, role))
					r = null;
			}
			
		}
		
		return r;
	}
	
	
	private boolean hasRole(Race r, String role)
	{
	
		for(Pose p : r.poses)
		{
			if(p.roles.contains(role))
				return true;
		}
		
		
		
		return false;

	}

}
