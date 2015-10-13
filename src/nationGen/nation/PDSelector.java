package nationGen.nation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.misc.Command;
import nationGen.units.Unit;

public class PDSelector {
    
	private NationGen ng;
	private Nation n;
	private Random r;
	public PDSelector(Nation n, NationGen ng)
	{
		this.ng = ng;
		this.n = n;
		r = new Random(n.random.nextInt());
	}
	
	
	
	public Unit getStartArmyCommander()
	{
		return getCommander(1, true);
	}
	
	public Unit getPDCommander(int rank)
	{
		return getCommander(rank, false);
	}

	private Unit getCommander(int rank, boolean startarmy)
	{
		if(rank < 1)
			rank = 1;
		if(rank > 2)
			rank = 2;
		
		List<Unit> commanders = n.generateComList("commander");
		
		
		// Chance to get a priest as potential commander for PD
		if(!startarmy)
		{
			double rand = r.nextDouble();
			if(rand < 0.025)
				commanders.add(0, n.generateComList("priest").get(0));
			else if(rand < 0.1)
				commanders.add(1, n.generateComList("priest").get(0));
		}
		
		List<Unit> others = new ArrayList<Unit>();
		if(startarmy)
		{
			others.add(getMilitia(1, 1));
			others.add(getMilitia(1, 2));
		}
		else if(rank == 1)
		{
			others.add(getMilitia(1, 1));
			others.add(getMilitia(2, 1));
		}
		else if(rank == 2)
		{
			others.add(getMilitia(1, 1));
			others.add(getMilitia(2, 1));
		}
		
		boolean ud_demon = false;
		boolean magicbeing = false;
		
		for(Unit u : others)
			for(Command c : u.commands)
			{
				if(c.command.equals("#undead") || c.command.equals("#demon"))
					ud_demon = true;
				else if(c.command.equals("#magicbeing"))
					magicbeing = true;
			}
		
		
		List<String> badprefixes = new ArrayList<String>();
		badprefixes.add("no");
		if(startarmy)
			badprefixes.add("poor");
		
		// First pass at trying to find a suitable commander
		List<Unit> validComs = new ArrayList<Unit>();
		for(Unit u : commanders)
		{
			boolean udvalid = false;
			boolean magicvalid = false;
			for(Command c : u.commands)
			{
				if(ud_demon && c.command.contains("undeadleader"))
				{
					String leader = c.command.substring(1, c.command.indexOf("undeadleader"));
					if(!badprefixes.contains(leader))
					{
						udvalid = true;
					}
				}
				if(magicbeing && c.command.contains("magicleader"))
				{
					String leader = c.command.substring(1, c.command.indexOf("magicleader"));
					if(!badprefixes.contains(leader))
					{
						magicvalid = true;
					}
				}
			}
			

			if(!ud_demon || ud_demon == udvalid)
				if(!magicbeing || magicbeing == magicvalid)
					validComs.add(u);
			
			
		}
		
		
		boolean failsafe = false;
		if(validComs.size() == 0 || (validComs.size() == 1 && rank == 2))
		{
			failsafe = true;
			validComs = commanders;
		}

		
		Unit com = validComs.get(rank - 1);
		if(failsafe)
		{
			if(magicbeing)
				com.commands.add(new Command("#magiccommand", "40"));
			if(ud_demon)
				com.commands.add(new Command("#undcommand", "40"));
		}
		
		return commanders.get(0);
	}
	
	/**
	 * Gets the rank:th best militia unit of tier:th tier
	 * @param rank rankth best (1-2)
	 * @param tier tier increases possible resource cost.
	 * @return
	 */
	public Unit getMilitia(int rank, int tier)
	{
		
		if(rank < 1)
			rank = 1;
		if(rank > 2)
			rank = 2;
		
		
		List<List<Unit>> militia = new ArrayList<List<Unit>>();

		List<Unit> units = n.combineTroopsToList("infantry");
		units.addAll(n.combineTroopsToList("mounted"));
		units.addAll(n.combineTroopsToList("ranged"));
		
		List<Unit> unsuitable = new ArrayList<Unit>();
		for(Unit u : units)
			if(Generic.containsTag(Generic.getAllUnitTags(u), "cannot_be_pd"))
				unsuitable.add(u);
		
		if(units.size() > unsuitable.size())
			units.removeAll(unsuitable);
		
		double targetgcost = 10;
		double targetrcost = 10;
		
		
		// Target resource/gcost is instead means
		double totalr = 0;
		double totalg = 0;
		for(Unit u : units)
		{
			totalr += u.getResCost(true);
			totalg += u.getGoldCost();
		}
		targetgcost = totalg / units.size();
		targetrcost = totalr / units.size();
		
		
		
		
		
		// handle commands for increasing/decreasing the target
		List<String> tags = Generic.getAllNationTags(n);

		if(Generic.containsTag(tags, "pd_targetrcost"))
			targetrcost = Double.parseDouble(Generic.getTagValue(tags, "pd_targetrcost"));
		if(Generic.containsTag(tags, "pd_targetgcost"))
			targetgcost = Double.parseDouble(Generic.getTagValue(tags, "pd_targetgcost"));
		
		if(Generic.containsTag(tags, "pd_targetrcostmulti"))
			for(String str : Generic.getTagValues(tags, "pd_targetrcostmulti"))
				targetrcost *= Double.parseDouble(str);
		if(Generic.containsTag(tags, "pd_targetgcostmulti"))
			for(String str : Generic.getTagValues(tags, "pd_targetgcostmulti"))
				targetgcost *= Double.parseDouble(str);
		
		// Do the magic
		List<Unit> units2 = new ArrayList<Unit>();
		units2.addAll(units);
		
		for(int i = 0; i < tier; i++)
		{
			if(units.size() == 0)
			{
				//System.out.println("NOT ENOUGH UNITS FOR MILITIA?! WHAT IS THIS MADNESS? (Nation " + this.nationid + ")");
				
				units = n.combineTroopsToList("infantry");
				units.addAll(n.combineTroopsToList("mounted"));
				units.addAll(n.combineTroopsToList("ranged"));
				//break;
			}
			
			List<Unit> rankunits = new ArrayList<Unit>();
			
			targetrcost = targetrcost * 1.2;
			while(rankunits.size() < 2 && units.size() > 0)
			{	
				boolean canBeRanged = true;
				if(i > 0 && militia.get(i - 1).get(0).isRanged())
					canBeRanged = false;
				if(rankunits.size() > 0 && rankunits.get(0).isRanged())
					canBeRanged = false;
				
				Unit best = units.get(0);
				double bestscore = scoreForMilitia(best, targetrcost, targetgcost);
				for(Unit u : units)
				{
					if(!u.isRanged() || (u.isRanged() && canBeRanged))
					{
						double score = scoreForMilitia(u, targetrcost, targetgcost);
						if(bestscore >= score)
						{
							bestscore = score;
							best = u;
						}		
					}
				}
				units.remove(best);
				rankunits.add(best);
			}
			

			if(rankunits.size() < 2)
			{
				//System.out.println("MILITIA PROBLEM - FAILSAFE INITIATED FOR RACE " + this.races.get(0).name + " NATION " + this.nationid);
				rankunits.add(rankunits.get(0));
			}

			militia.add(i, rankunits);
		}

		
		return militia.get(tier - 1).get(rank - 1);
	}
	
	private double scoreForMilitia(Unit u, double targetres, double targetgold)
	{

		double goldscore = 0;
		double resscore = 0;
		
		
		goldscore = Math.abs(u.getGoldCost() - targetgold);
		if(u.getGoldCost() < targetgold * 0.7)
			goldscore = goldscore * 2;
		if(u.getGoldCost() > targetgold * 2)
			goldscore = goldscore * 2;
		
		resscore = Math.abs(u.getResCost(false) - targetres);
		if(u.getResCost(false) < targetres * 0.7)
			resscore = resscore * 2;
		if(u.getResCost(false) > targetres * 2.25)
			resscore = resscore * 2;
	
		if(u.isRanged() && u.getResCost(false) < 20)
			resscore = resscore * 0.25;
		
		//System.out.println("TARGET: " + targetres + " " + targetgold);
		//System.out.println("UNIT  : " + u.getResCost() + " " + u.getGoldCost() + " - " + u.id + " / " + u.name);
		//System.out.println("--> " + (0.75*goldscore + resscore));
		return 0.75*goldscore + resscore;
	}
	
	public int getStartArmyAmount(Unit u)
	{
		// Handle filter etc stuff
		double filtermulti = 1;
		List<String> tags = Generic.getAllNationTags(n);
		if(Generic.containsTag(tags, "startarmy_amountmulti"))
			for(String str : Generic.getTagValues(tags, "startarmy_amountmulti"))
				filtermulti *= Double.parseDouble(str);
		
		
		double amount = militiaAmount(u) * 1.2 * filtermulti;
		

		return (int)Math.round(amount);
	}
	
	
	public int getMilitiaAmount(Unit u)
	{
		// Handle filter etc stuff
		double filtermulti = 1;
		List<String> tags = Generic.getAllNationTags(n);
		if(Generic.containsTag(tags, "pd_amountmulti"))
			for(String str : Generic.getTagValues(tags, "pd_amountmulti"))
				filtermulti *= Double.parseDouble(str);
		
		
		double amount = militiaAmount(u) * filtermulti;
		

		return (int)Math.round(amount);
		
	}
	
	private double militiaAmount(Unit u)
	{
		int res = u.getResCost(true);
		int gold = u.getGoldCost();
		
		if(res >= ng.settings.get("resUpperTreshold"))
			res += ng.settings.get("resUpperTresholdChange");
		if(res <= ng.settings.get("resLowerTreshold"))
			res += ng.settings.get("resLowerTresholdChange");
		if(gold >= ng.settings.get("goldUpperTreshold"))
			gold += ng.settings.get("goldUpperTresholdChange");
		if(gold <= ng.settings.get("goldLowerTreshold"))
			gold += ng.settings.get("goldLowerTresholdChange");
		
		if(res >= ng.settings.get("resMultiTreshold"))
			res *= ng.settings.get("resMulti");
		
		// The higher the score, the smaller your starting army will be:
		// 20 / (rescost + goldcost) * 10 * militiaMultiplier = amount of specified unit in militia
		// multiplier is 1 by default in settings, but may be increased by commands in generation
		
		double score = res + gold;
		
		double multi = 20 / score;
		
		double result = multi * 10;

		result = result * ng.settings.get("militiaMultiplier");
		

		return result;
		
	}
}
