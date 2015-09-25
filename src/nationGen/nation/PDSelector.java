package nationGen.nation;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.units.Unit;

public class PDSelector {
    
	private NationGen ng;
	private Nation n;
	public PDSelector(Nation n, NationGen ng)
	{
		this.ng = ng;
		this.n = n;
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
