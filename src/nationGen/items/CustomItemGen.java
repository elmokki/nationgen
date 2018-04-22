package nationGen.items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import com.elmokki.Dom3DB;
import com.elmokki.Generic;

import nationGen.entities.MagicItem;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class CustomItemGen {
	
	
	Nation n;
	Random random;
	public CustomItemGen(Nation n)
	{
		this.n = n;
		random = new Random(n.random.nextInt());
	}
	
	
	public CustomItem getMagicItem(Unit u, Item olditem, int maxpower, double epicness, List<MagicItem> magicItems)
	{

		boolean named = false;
		CustomItem i = this.getFromItem(olditem);
		if(i == null)
			return null;
		
		boolean ranged = false;
		boolean lowshots = false;
		
		if(n.nationGen.weapondb.GetInteger(olditem.id, "rng") != 0)
			ranged = true;
		if(n.nationGen.weapondb.GetInteger(olditem.id, "shots", 100) < 4)
			lowshots = true;
			

		if(random.nextDouble() > epicness)
		{

			List<MagicItem> possibles = new ArrayList<MagicItem>();
			for(MagicItem m : magicItems)
			{
				boolean good = true;
				for(String tag : m.tags)
					if(ranged && !lowshots && tag.equals("no ranged"))
						good = false;
					else if(ranged && lowshots && tag.equals("no lowshots"))
						good = false;
					else if(!ranged && tag.equals("no melee"))
						good = false;
			
				if(good && m.power <= maxpower)
					possibles.add(m);
			}
			
			if(possibles.size() > 0)
			{
				
				ChanceIncHandler chandler = new ChanceIncHandler(n, "customitemgenerator");
				LinkedHashMap<MagicItem, Double> itemMap = chandler.handleChanceIncs(u, possibles); 
				
				MagicItem mitem = MagicItem.getRandom(random, itemMap);
				
				// Special looks
				if(Generic.containsTag(mitem.tags, "weapon"))
				{
					List<Item> pos = new ArrayList<Item>();
					for(String tag : mitem.tags)
					{
						List<String> args = Generic.parseArgs(tag);
						if(args.get(0).equals("weapon") && args.size() > 2 && args.get(1).equals(olditem.id))
						{
							Item temp = u.pose.getItems(olditem.slot).getItemWithName(args.get(2), olditem.slot);
							if(temp != null)
								pos.add(temp);
						}
					}
					
					if(pos.size() > 0)
					{
						CustomItem miten = this.getFromItem(Item.getRandom(random, pos));
						if(miten != null)
							i = miten;
					}
				}
				
				
				for(Command c : mitem.getCommands())
				{
					String key = c.command.substring(1);
					
					
					if(c.args.size() > 0)
					{
						String value = c.args.get(0);
						
						String oldvalue = "";
						if(i.values.get(key) != null)
							oldvalue = i.values.get(key);
				
						int temp = chandler.applyModifier(Integer.parseInt(oldvalue), value).intValue();
						i.values.put(key, temp + "");
					}
					else
					{
						i.values.put(key, "");
					}
				}
				
				
				
				if(!mitem.effect.equals("-1"))
					i.values.put("secondaryeffect", mitem.effect);
				
				String name = n.nationGen.weapondb.GetValue(olditem.id, "weapon_name");
				List<String> prefixes = new ArrayList<String>();
				List<String> suffixes = new ArrayList<String>();
				
				for(String part : mitem.names)
				{
					List<String> args = Generic.parseArgs(part, "'");
					if(args.get(0).equals("prefix"))
						prefixes.add(args.get(1));
					else if(args.get(1).equals("suffix"))
						suffixes.add(args.get(1));
				}
				
				if(suffixes.size() > 0 || prefixes.size() > 0)
				{
					String part = "";
					int rand = random.nextInt(suffixes.size() + prefixes.size()) + 1;
					if(rand > suffixes.size() && suffixes.size() > 0)
					{
						part = suffixes.get(random.nextInt(suffixes.size()));
						name = "\"" + Generic.capitalize(name + " " + part) + "\"";
					}
					else
					{
						part = prefixes.get(random.nextInt(prefixes.size()));
						name = "\"" + Generic.capitalize(part + " " + name) + "\"";
					}
	
					named = true;
					i.values.put("name", name);
				}
				
				i.magicItem = mitem;
				
				
				// Increase gcost
				for(String tag : mitem.tags)
				{
					
					List<String> args = Generic.parseArgs(tag);

					if(ranged && !lowshots && args.get(0).equals("gcost") && args.get(1).equals("ranged"))
						i.commands.add(new Command("#gcost", args.get(2)));
					else if(ranged && lowshots && args.get(0).equals("gcost") && args.get(1).equals("lowshots"))
						i.commands.add(new Command("#gcost", args.get(2)));
					else if(!ranged && args.get(0).equals("gcost") && args.get(1).equals("melee"))
						i.commands.add(new Command("#gcost", args.get(2)));
					
					if(ranged && !lowshots && args.get(0).equals("rcost") && args.get(1).equals("ranged"))
						i.commands.add(new Command("#rcost", args.get(2)));
					else if(ranged && lowshots && args.get(0).equals("rcost") && args.get(1).equals("lowshots"))
						i.commands.add(new Command("#rcost", args.get(2)));
					else if(!ranged && args.get(0).equals("rcost") && args.get(1).equals("melee"))
						i.commands.add(new Command("#rcost", args.get(2)));
				}
				i.tags.addAll(mitem.tags);
			}
		}

		
		int runs = 1 + random.nextInt(1); // 1-2
		if(random.nextDouble() > epicness)
			runs++;
		if(random.nextDouble() > epicness / 2)
			runs++;
		if(random.nextDouble() > epicness / 4)
			runs++;
		
		boolean magic = false;
		if(!ranged && i.magicItem != null && random.nextDouble() > 0.75)
		{
			i.values.put("magic", "");
			magic = true;
		}
		
		// Add gold cost
		int potentialgcost = runs;
		if(magic)
			potentialgcost = Math.max((int)Math.round(1.5 * (double)(potentialgcost)), 3);

		int gcost = u.getGoldCost();
		if(gcost * 0.1 < potentialgcost)
			u.commands.add(new Command("#gcost","+" + potentialgcost));
		else
			u.commands.add(new Command("#gcost","*1.1"));
		
		// Add res cost
		int potentialrcost = runs;
		if(magic)
			potentialrcost = Math.max((int)Math.round(1.5 * (double)(potentialrcost)), 3);
		
		int rcost = u.getResCost(true);
		if(rcost * 0.1 < potentialrcost)
			u.commands.add(new Command("#rcost","+" + potentialrcost));
		else
			u.commands.add(new Command("#rcost","*1.1"));
		
		double[] chances = {1, 1, 1, 1};
		while(runs > 0)
		{
	
			
			int sum = 0;
			for(int j = 0; j < chances.length; j++)
				sum += chances[j];
			
			double rand = random.nextDouble() * sum;
			
			if(ranged)
				chances[3] = 0;
			if(ranged || lowshots)
				chances[1] = 0;
		
			if((runs <= 2 && ranged))
				chances[0] = 0;
			
			if(rand <= chances[0] && !ranged || (runs > 2 && ranged))
			{
				chances[0] *= 0.33;
				int att = Integer.parseInt(i.values.get("att"));
				att++;
				i.values.put("att", "" + att);
				
				if(ranged && !lowshots)
					runs -= 2;
				
				runs--;
			}
			else if(rand <= chances[0] + chances[1] && !ranged)
			{

				chances[1] *= 0.33;
				int def = Integer.parseInt(i.values.get("def"));
				def++;
				i.values.put("def", "" + def);
				runs--;

			}
			else if(rand <= chances[0] + chances[1] + chances[2])
			{

				chances[2] *= 0.33;
				int dmg = Integer.parseInt(i.values.get("dmg"));
				if(dmg < 63)					 // Weapons with dmg 64+ are (with one exception, the Deadliest Poison at 75) not actually damage, but instead special effects encoded as damage, so we don't want to screw them up
					dmg++;
				i.values.put("dmg", "" + dmg);
				runs--;
			}
			else if(rand <= chances[0] + chances[1] + chances[2] + chances[3] && runs > 1 && !ranged && !magic)
			{

				chances[3] = 0;
				i.values.put("magic", "");
				magic = true;
				runs--;
			}
			
			

			
			
		}
		String name = n.nationGen.weapondb.GetValue(olditem.id, "weapon_name");
		

		if(!magic && (i.magicItem == null || !named))
			i.values.put("name", "\"Exceptional " + name + "\"");
		else if(magic && (i.magicItem == null || !named))
			i.values.put("name", "\"Enchanted " + name + "\"");
		
		String dname = "nation_" + n.nationid + "_customitem_" + (n.customitems.size() + 1);
		i.id = dname;
		i.name = dname;
		
		if(i.magicItem != null)
			i.tags.addAll(i.magicItem.tags);
		
		n.customitems.add(i);
		n.nationGen.customitems.add(i);
		n.nationGen.weapondb.addToMap(i.id, i.getHashMap());
		

		return i;
	}
	
	public CustomItem getFromItem(Item item)
	{

		
		if(item == null)
		{
			return null;
		}
		
		if(!Generic.isNumeric(item.id))
			return null;
		
		CustomItem newitem = new CustomItem(n.nationGen);
		newitem.sprite = item.sprite;
		newitem.mask = item.mask;
		newitem.commands.addAll(item.commands);
		newitem.tags.addAll(item.tags);
		newitem.dependencies.addAll(item.dependencies);
		newitem.setOffsetX(item.getOffsetX());
		newitem.setOffsetY(item.getOffsetY());
		newitem.slot = item.slot;
		newitem.basechance = item.basechance;
		newitem.renderslot = item.renderslot;
		newitem.renderprio = item.renderprio;
		newitem.armor = item.armor;
		newitem.olditem = item;
		
		Dom3DB db = null;
		if(item.armor)
			db = n.nationGen.armordb;
		else
			db = n.nationGen.weapondb;
		
		
		
		if(!item.armor)
		{
			List<String> boolargs = db.getBooleanArgs();
			
			if(db.GetValue(item.id, "weapon_name").equals(""))
			{
				return null;
			}
			
			newitem.values.put("att", "0");
			newitem.values.put("len", "0");
			newitem.values.put("dmg", "0");

			for(String def : db.getDefinition())
			{
				
	
				if(def.equals("id"))
				{
					// do nothing
				}
				else if(def.equals("weapon_name"))
				{
					newitem.values.put("name", "\"" + db.GetValue(item.id, "weapon_name") + "\"");
				}
				else if(def.equals("res"))
				{
					newitem.values.put("rcost", "" + db.GetValue(item.id, "res"));
				}
	
				else if(def.equals("dt_blunt"))
				{	
					if(db.GetValue(item.id, def).equals("1"))
						newitem.values.put("blunt", "");
				}	
				else if(def.equals("dt_slash"))
				{
					if(db.GetValue(item.id, def).equals("1"))
					newitem.values.put("slash", "");
				}
				else if(def.equals("dt_pierce"))
				{
					if(db.GetValue(item.id, def).equals("1"))
						newitem.values.put("pierce", "");
				}
				else if(def.equals("lgt"))
				{
					if(db.GetInteger(item.id, "lgt") > 0)
						newitem.values.put("len", "" + db.GetValue(item.id, "lgt"));
				}
				else if(def.equals("rng"))
				{
					if(db.GetInteger(item.id, "rng") != 0)
					{
						
						newitem.values.put("range", db.GetValue(item.id, "rng"));

					}
				}
				
				else if(def.equals("#att"))
				{
					if(db.GetInteger(item.id, "#att") != 1)
						newitem.values.put("nratt", db.GetValue(item.id, "#att"));
				}
				else if(def.equals("shots"))
				{
					if(db.GetInteger(item.id, "shots") > 0)
						newitem.values.put("ammo", db.GetValue(item.id, "shots"));

				}
				else if(def.equals("2h"))
				{
					if(db.GetValue(item.id, "2h").equals("1"))
						newitem.values.put("twohanded", "");
				}
				
				else if(def.equals("flyspr"))
				{
					if(!db.GetValue(item.id, "flyspr", "derp").equals("derp"))
					{
						String flyspr = db.GetValue(item.id, "flyspr");
						String speed = db.GetValue(item.id, "animlength", "derp");
						if(speed.equals("derp"))
							speed = "1";
						
						newitem.values.put("flyspr", flyspr + " " + speed);
					}
				}
				

				// Obsolete?
				else if(def.equals("onestrike"))
				{
					if(db.GetValue(item.id, "onestrike").equals("1"))
						newitem.values.put("ammo", "1");
				}
				else if(def.equals("ap"))
				{
					if(db.GetValue(item.id, "ap").equals("1"))
						newitem.values.put("armorpiercing", "");
				}
				else if(def.equals("an"))
				{
					if(db.GetValue(item.id, "an").equals("1"))
						newitem.values.put("armornegating", "");
				}
				
				// Skippable stuff
				else if(def.equals("effect_record_id"));
				else if(def.equals("secondaryeffect") && db.GetValue(item.id, def).equals("0"));
				else if(def.equals("secondaryeffectalways") && db.GetValue(item.id, def).equals("0"));
				else if(def.equals("animlength"));
				else if(def.equals("dt_norm"));
				else if(def.equals("aoe") && db.GetValue(item.id, def).equals("0"));

						
				// Generic handle for boolean args
				else if(boolargs.contains(def))
				{
					if(db.GetValue(item.id, def).equals("1"))
					{
						newitem.values.put(def, "");
					}
				}
				// Handle non-boolean args
				else if(!db.GetValue(item.id, def).equals(""))
				{
					newitem.values.put(def, db.GetValue(item.id, def));
				}
		
				
			}
			
			

			// No magic item from spc damage items
			if(newitem.values.get("dmg").equals("spc") || item.id.equals("-1"))
				return null;
		}
		
		
		else
		{
			// ARMOR, not done
		}


		return newitem;
	}
	
	
}
