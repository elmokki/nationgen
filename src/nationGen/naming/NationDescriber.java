package nationGen.naming;

import nationGen.NationGenAssets;
import nationGen.entities.Filter;
import nationGen.misc.Arg;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.ShapeShift;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import com.elmokki.Generic;


public class NationDescriber {
	private Nation n;
	private Random random;
	private NationGenAssets assets;
	private ChanceIncHandler chandler;
	
	public LinkedHashMap<String,String> descDictionary = new LinkedHashMap<>(); 
	public Filter superlative = null;
	
	public NationDescriber(Nation n, NationGenAssets assets)
	{
		random = new Random(n.random.nextInt());
		this.n = n;
		this.assets = assets;
		this.chandler = new ChanceIncHandler(n);
		describeTroops();
		describeCommanders();
		describeHeroes();
		describeForms();
	}
	
	//if a filter has multiple possible description keys, this randomly selects one and logs it
	//previously logged synonyms are reused within the same nation for consistency
	public String getRandomDescription(Filter f)
	{
		String temp = "";
		if (!f.tags.containsName("uniquedescription") && this.descDictionary.containsKey(f.name))
			temp = this.descDictionary.get(f.name);
		else if (f.thesaurus.size() > 0)
		{
			this.descDictionary.put(f.name, f.thesaurus.get(random.nextInt(f.thesaurus.size())));
			temp = this.descDictionary.get(f.name);
		}
		else if (f.tags.containsName("description"))
			temp = f.tags.getString("description").orElse(""); 
		
		if (temp != "" && !(temp.substring(0,1).equals(".") || temp.substring(0,1).equals(",") || temp.substring(0,1).equals(";")))
			temp = " " + temp;	
		
		return temp;
	}
	
	//chain together description fragments leading backwards
	public StringBuilder expandPrevDesc(StringBuilder compiledDesc, Filter currentFilter, Unit currentUnit, List<Filter> bridge)
	{
		if (!currentFilter.prevDesc.isEmpty())
		{																
			currentFilter = chandler.handleChanceIncs(currentUnit, currentFilter.prevDesc).getRandom(random);
			if (currentFilter != null)	
			{
				if (currentFilter.prevDesc.isEmpty())
					return expandPrevDesc(compiledDesc, currentFilter, currentUnit, bridge);
				else
					return expandPrevDesc(compiledDesc, currentFilter, currentUnit, bridge).append(getRandomDescription(currentFilter));
			}
			else
				return compiledDesc;
		}	
		else
		{
			//if we've reached the start of the description set and the previous set is trying to segue, use its segue instead
			if (bridge != null)
			{
				currentFilter = chandler.handleChanceIncs(currentUnit, bridge).getRandom(random);
				if (currentFilter != null)				
					return compiledDesc.append(getRandomDescription(currentFilter));			
				else
					return compiledDesc;
			}
			else				
				return compiledDesc.append(getRandomDescription(currentFilter));
		}
	}
	
	//chain together description fragments leading forwards
	public StringBuilder expandNextDesc(StringBuilder compiledDesc, Filter currentFilter, Unit currentUnit, int position)
	{
		if (!currentFilter.nextDesc.isEmpty())
		{							
			currentFilter = chandler.handleChanceIncs(currentUnit, currentFilter.nextDesc).getRandom(random);
			if (currentFilter != null)				
				return expandNextDesc(compiledDesc, currentFilter, currentUnit, position).insert(position,getRandomDescription(currentFilter));
		}	
				
		return compiledDesc;
	}
	
	private void describeUnits(DescriptionReplacer dr, List<Unit> units, Filter descf, boolean skipRaceDesc)
	{
		dr.calibrate(units);
		
		for (Unit u : units)
			describeUnit(dr, u, descf, skipRaceDesc);
	}
	
	private void describeUnit(DescriptionReplacer dr, Unit u, Filter descf, boolean skipRaceDesc)
	{			
		LinkedHashMap<String,ArrayList<Filter>> descSets = new LinkedHashMap<>();
			
		dr.calibrate(u);
		
		StringBuilder desc = new StringBuilder();
				
		Command tmpDesc = null;

		//big important units already have complicated enough descriptions, so no need to paste the whole description in
		if (!skipRaceDesc)
		{
			desc.append(u.race.tags.getString("description").map(s -> s + " ").orElse(""));
			for(Command c : u.getCommands())
			{
				if(c.command.equals("#descr"))
					tmpDesc = c;			
			}
					
			if(tmpDesc != null)
			{
				desc.append(" ").append(tmpDesc.args.get(0));
			}
		}
			
		//sort filters into logical groupings; negative-sounding filters go at the end of their respective lists
		for(Filter f : u.appliedFilters)
		{
			if(f != null)
			{
				Filter tempFilter = f.description;
				String tempSet = tempFilter.descSet.isEmpty() ? "misc" : tempFilter.descSet;				
					
				if (!descSets.containsKey(tempSet))				
				{
					descSets.put(tempSet, new ArrayList<Filter>());
					descSets.get(tempSet).add(tempFilter);
				}
				else if (tempFilter.tags.containsName("negative"))
					descSets.get(tempSet).add(tempFilter);
				else
					descSets.get(tempSet).add(0, tempFilter);
			}
		}
		
		if(descf != null)
		{
			String tempSet = descf.descSet.isEmpty() ? "misc" : descf.descSet;			
				
			if (!descSets.containsKey(tempSet))						
				descSets.put(tempSet, new ArrayList<Filter>());
			
			descSets.get(tempSet).add(descf);
		}		

					
		List<Filter> bridge = null;
		String[] descCategories = {"mage","priest","army role","troop","commander","terrain","lineage","recruitment","physique","skill","immunity","vulnerability","senses","misc","wondrous","shapeshift","retinue","death"};
		
		for (String k : descCategories)
		{
			if (descSets.containsKey(k) && descSets.get(k).size() > 0)
			{				
				Filter tempFilter = descSets.get(k).get(0); 
				boolean hasDesc = !getRandomDescription(tempFilter).isEmpty();
				boolean negative = false;
					
				if (!tempFilter.prevDesc.isEmpty() || (bridge != null && !tempFilter.nextDesc.isEmpty()))
				{
					hasDesc = true;
					desc = expandPrevDesc(desc, tempFilter, u, bridge);
				}
				
				for (int i=0; i < descSets.get(k).size(); i++)
				{	
					tempFilter = descSets.get(k).get(i);
					if (!getRandomDescription(descSets.get(k).get(i)).isEmpty()) 
					{			
						hasDesc = true;
												
						if (i > 0 || (bridge == null || !tempFilter.prevDesc.isEmpty() || tempFilter.nextDesc.isEmpty()))
						{
							//insert up to 1 random superlative													
							if (superlative == null && tempFilter.tags.containsName("allowsuperlative"))
								superlative = tempFilter;
							
							if (superlative == tempFilter && descSets.get(k).size() == 1)
							{	
								List<Filter> possibles = ChanceIncHandler.retrieveFilters("filterdescriptions", "filterdescs", assets.descriptions, null, u.race).stream().filter(f -> f.name.equals("superlative")).collect(Collectors.toList());
								if (possibles.size() > 0)
									desc.append(getRandomDescription(chandler.handleChanceIncs(u,possibles).getRandom(random)));
							}				
							
							desc.append(getRandomDescription(descSets.get(k).get(i)));
						}						
					}
					
					if (i < descSets.get(k).size() - 1)
					{
						if (hasDesc)
						{
							if (!negative && descSets.get(k).size() > 2)
								desc.append(",");
							
							if (!negative && descSets.get(k).get(i+1).tags.containsName("negative"))
							{
								negative = true;
								desc.append(" but");
							}
							else if (i == descSets.get(k).size() - 2 || (i < descSets.get(k).size() - 2 && descSets.get(k).get(i+2).tags.containsName("negative")))
								desc.append(" and");
						}
					}					
					else
					{													
						if (!tempFilter.nextDesc.isEmpty())
						{
							hasDesc = true;
							desc = expandNextDesc(desc, tempFilter, u, desc.length());
						}
						
						//if we've already joined 2 sentences, do not bridge to a 3rd
						if (hasDesc)
						{
							if (bridge != null)
								bridge = null;							
							else if (tempFilter.bridgeDesc.size() > 0)
								bridge = tempFilter.bridgeDesc;
						}
						
						//if the description is short, elaborate freely on filters if possible
						if (descSets.values().size() < 5 && descSets.get(k).size() == 1)
						{
							String tempDesc = tempFilter.tags.getString("extendeddescription").map(s -> " " + s).orElse(""); 
							if (!tempDesc.isEmpty())
							{
								desc.append(tempDesc);							
								bridge = null;
							}
						}
					}
				}
				
				if (hasDesc && bridge == null)
					desc.append(".");
			}
		}
		
		//if we get here it means the last filter on the list was looking for another description to segue into and didn't have one, so cap it with a period
		if (bridge != null)
			desc.append(".");
		
		desc.append(" ");
		
		desc.append(u.slotmap.items()
				.map(i -> i.tags.getString("description"))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.joining(" "))).append(" ");
				
			
		desc = new StringBuilder(dr.replace(desc.toString().trim()));		
	
		
		if(u.tags.containsName("montagunit"))
		{
			if(desc.length() > 0)
				desc.append("\n\n");
			
			desc.append("When recruited, one unit of this category will appear instead of the unit shown here.");
		}
		
		String description = dr.replace(desc.toString()).replaceAll("\"", "");

		if(tmpDesc != null)
			u.setCommandValue("#descr", description);
		else
			u.commands.add(Command.args("#descr", description));
	}
	
	private void describeForms()
	{
		DescriptionReplacer dr = new DescriptionReplacer(n);
		String description = "No description";
		
		List<ShapeChangeUnit> sus = n.getShapeChangeUnits();
		
		for (ShapeChangeUnit su : sus)
		{
			dr.calibrate(su.otherForm);
			
			if (su.otherForm.hasCommand("#secondtmpshape") || (su.otherForm.hasCommand("#secondshape") && su.thisForm.tags.containsName("#nowayback")))
			{
				if (su.otherForm.hasCommand("#fixedname"))
					description = su.otherForm.getStringCommandValue("#fixedname", "Hero") + " lives on in this form after %pronoun% dies.";
				else
					description = Generic.plural(su.otherForm.getName()) + " leave this behind after they die.";
			}
			else
				description = su.otherForm.getStringCommandValue("#descr", "No description");		
			
			description = dr.replace(description).trim();
			su.commands.add(new Command("#descr", new Arg(description)));
		}
	}
	
	private void describeTroops()
	{
		String[] roles = {"ranged", "infantry", "mounted", "chariot", "sacred"};
		
		List<Filter> descs = ChanceIncHandler.retrieveFilters("troopdescriptions", "troopdescs", assets.descriptions, null, n.races.get(0));
		descs = descs.stream().filter(f -> f.descSet.equals("troop")).collect(Collectors.toList());
		
		DescriptionReplacer dr = new DescriptionReplacer(n);
		for(String role : roles)
		{
			
			List<Unit> units = n.listTroops(role);
			if(units.isEmpty())
				continue;
				
				
			if(!role.equals("mounted"))
				dr.descs.put("%role%", role);
			else
				dr.descs.put("%role%", "cavalry");
	
			if(role.equals("chariot"))
				dr.descs.put("%role%", role + "s");
			if(role.equals("ranged") || role.equals("sacred"))
				dr.descs.put("%role%", role + " units");
	
			
			List<Filter> possibles = new ArrayList<>();
			for(Filter f : descs)
				if(ChanceIncHandler.suitableFor(units.get(0), f, n))
					possibles.add(f);
			
			Filter descf = null;
			if(possibles.size() > 0)
			{
				descf = chandler.handleChanceIncs(units.get(0), possibles).getRandom(random);

			}
			
			describeUnits(dr, units, descf, false);			
		}
		
	}

	private void describeCommanders()
	{
		String[] roles = {"commanders","priests","mages"};
					
		List<Filter> descs = ChanceIncHandler.retrieveFilters("commanderdescriptions", "commanderdescs", assets.descriptions, null, n.races.get(0));

		DescriptionReplacer dr = new DescriptionReplacer(n);
		
		for(String role : roles)
		{
			List<Unit> units = n.listCommanders(role);
	
			dr.calibrate(units);

			for(Unit u : units)
			{
				dr.calibrate(u);

				List<Filter> possibles = new ArrayList<>();
				for(Filter f : descs.stream().filter(f -> f.name.equals("commander start")).collect(Collectors.toList()))
					if(ChanceIncHandler.suitableFor(units.get(0), f, n))
						possibles.add(f);
				
				Filter descf = null;
				if(possibles.size() > 0)
				{
					descf = chandler.handleChanceIncs(units.get(0), possibles).getRandom(random);
				}
				
				boolean skipRaceDesc = false;
				
				//big important mages cut out race descriptions to save space
				if (u.tags.contains("schoolmage",  3) && u.appliedFilters.size() > 5)
					skipRaceDesc = true;
				
				describeUnit(dr, u, descf, false);
			}
		}
	}
	
	private void describeHeroes()
	{
		List<Filter> descs = ChanceIncHandler.retrieveFilters("herodescriptions", "herodescs", assets.descriptions, null, n.races.get(0));

		DescriptionReplacer dr = new DescriptionReplacer(n);
		
		dr.calibrate(n.heroes);
		
		for(Unit u : n.heroes)
		{	
			dr.calibrate(u);

			List<Filter> possibles = new ArrayList<>();
			
			for(Filter f : descs.stream().filter(f -> f.name.equals("hero start")).collect(Collectors.toList()))
				if(ChanceIncHandler.suitableFor(u, f, n))
					possibles.add(f);
				
			Filter descf = null;
			if(possibles.size() > 0)
			{
				descf = chandler.handleChanceIncs(u, possibles).getRandom(random);
			}
			
			describeUnit(dr, u, descf, true);
		}
	}

	
}
