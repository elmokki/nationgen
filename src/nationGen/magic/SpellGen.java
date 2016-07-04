package nationGen.magic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.misc.ChanceIncHandler;
import nationGen.nation.Nation;

public class SpellGen {
	private Nation n;
	private NationGen ng;
	private Random r;
	
	public SpellGen(NationGen ng, Nation n)
	{
		this.n = n;
		this.ng = ng;
		this.r = new Random(n.random.nextInt());
	}
	
	
	public void execute()
	{
		
		// Guaranteed spells
		List<Filter> possible = new ArrayList<Filter>();
		possible.addAll(ChanceIncHandler.retrieveFilters("guaranteedspells", "guaranteedspells_default", ng.spells, null, n.races.get(0)));
		ChanceIncHandler chandler = new ChanceIncHandler(n);
		
		LinkedHashMap<Filter, Double> map = chandler.handleChanceIncs(possible);
		while(map.size() > 0)
		{
			Filter f = Entity.getRandom(r, map);
			
			if(f != null)
				n.spells.add(f);

			possible.remove(f);
			map = chandler.handleChanceIncs(possible);
		}
	
		// Count spell count weighted by square roots
		int spellcount = 0;
		for(Filter f : n.spells)
			spellcount += Math.sqrt(Generic.getTagValues(f.tags, "spell").size());
		
		int randompicks = 1 + r.nextInt(6); // 1-6
		if(spellcount >= 8)
			randompicks--;
		if(spellcount >= 12)
			randompicks--;
		if(spellcount >= 14)
			randompicks--;
		if(spellcount >= 16)
			randompicks--;
		
		possible.clear();
		possible.addAll(ChanceIncHandler.retrieveFilters("randomspells", "randomspells_default", ng.spells, null, n.races.get(0)));
		for(int i = 0; i < randompicks; i++)
		{
			Filter f = chandler.getRandom(possible);
			
			if(f != null && Generic.getTagValues(f.tags, "spell").size() > 0)
			{
				possible.remove(f);
				n.spells.add(f);
			}
		}
		
		ng.handleSpells(n.spells, n);
		
	}
}
