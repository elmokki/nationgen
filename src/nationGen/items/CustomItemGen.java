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
				}
				i.tags.addAll(mitem.tags);
			}
		}

		
		int runs = 2;
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
		int potentialcost = runs;
		if(magic)
			potentialcost = Math.max((int)Math.round(1.5 * (double)(potentialcost)), 3);
		
		
		int gcost = u.getGoldCost();
		if(gcost * 0.1 < potentialcost)
			u.commands.add(new Command("#gcost","+" + potentialcost));
		else
			u.commands.add(new Command("#gcost","*1.1"));
		
		
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
		
			if((runs <= 2 && ranged && !lowshots))
				chances[0] = 0;
			
			if(rand <= chances[0] && (!ranged || lowshots) || (runs > 2 && ranged && !lowshots))
			{
				chances[0] *= 0.5;
				int att = Integer.parseInt(i.values.get("att"));
				att++;
				i.values.put("att", "" + att);
				
				if(ranged && !lowshots)
					runs -= 2;
				
				runs--;
			}
			else if(rand <= chances[0] + chances[1] && !ranged && !lowshots)
			{

				chances[1] *= 0.5;
				int def = Integer.parseInt(i.values.get("def"));
				def++;
				i.values.put("def", "" + def);
				runs--;

			}
			else if(rand <= chances[0] + chances[1] + chances[2])
			{

				chances[2] *= 0.5;
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
			
			
			if(db.GetValue(item.id, "weapon_name").equals(""))
			{
				return null;
			}
			
			
			if(db.GetValue(item.id, "dt_blunt").equals("1"))
				newitem.values.put("blunt", "");
			if(db.GetValue(item.id, "dt_slash").equals("1"))
				newitem.values.put("slash", "");
			if(db.GetValue(item.id, "dt_pierce").equals("1"))
				newitem.values.put("pierce", "");
			if(db.GetValue(item.id, "magic").equals("1"))
				newitem.values.put("magic", "");
			if(db.GetValue(item.id, "cold").equals("1"))
				newitem.values.put("cold", "");
			if(db.GetValue(item.id, "shock").equals("1"))
				newitem.values.put("shock", "");
			if(db.GetValue(item.id, "poison").equals("1"))
				newitem.values.put("poison", "");
			if(db.GetValue(item.id, "fire").equals("1"))
				newitem.values.put("fire", "");
			if(db.GetValue(item.id, "acid").equals("1"))
				newitem.values.put("acid", "");
			if(db.GetValue(item.id, "flail").equals("1"))
				newitem.values.put("flail", "");
			if(db.GetValue(item.id, "nostr").equals("1"))
				newitem.values.put("nostr", "");
			
			newitem.values.put("name", "\"" + db.GetValue(item.id, "wpname") + "\"");
			newitem.values.put("dmg", "" + db.GetValue(item.id, "dmg"));
			newitem.values.put("att", "" + db.GetValue(item.id, "att"));
			
			if(!db.GetValue(item.id, "def").equals(""))
			{
				newitem.values.put("def", "" + db.GetValue(item.id, "def"));
			}
			if(db.GetInteger(item.id, "lgt") > 0)
				newitem.values.put("len", "" + db.GetValue(item.id, "lgt"));

			
			if(!db.GetValue(item.id, "rng").equals(""))
			{
				if(db.GetValue(item.id, "rng").startsWith("str"))
				{
					String str = db.GetValue(item.id, "rng");
					if(str.startsWith("str/"))
					{
						newitem.values.put("range", "-" + str.charAt(str.length() - 1));
					}
					else
						newitem.values.put("range", "-1");
				}
				else
					newitem.values.put("range", db.GetValue(item.id, "rng"));
			
			}
			
			if(!db.GetValue(item.id, "#att").equals("1"))
				newitem.values.put("nratt", db.GetValue(item.id, "#att"));
			if(!db.GetValue(item.id, "shots").equals(""))
				newitem.values.put("ammo", db.GetValue(item.id, "shots"));
			if(db.GetValue(item.id, "2h").equals("1"))
				newitem.values.put("twohanded", "");
			if(!db.GetValue(item.id, "dt_cap").equals("0"))
				newitem.values.put("dt_cap", "");
			
			if(db.GetValue(item.id, "nostr").equals("nostr"))
				newitem.values.put("nostr", "");
			
			if(!db.GetValue(item.id, "flyspr", "derp").equals("derp"))
			{
				String flyspr = db.GetValue(item.id, "flyspr");
				String speed = db.GetValue(item.id, "animlength", "derp");
				if(speed.equals("derp"))
					speed = "1";
				
				newitem.values.put("flyspr", flyspr + " " + speed);
			}
			
			if(db.GetValue(item.id, "charge").equals("1"))
				newitem.values.put("charge", null);
			
			if(db.GetValue(item.id, "bonus").equals("1"))
				newitem.values.put("bonus", null);
			
			// Obsolete?
			if(db.GetValue(item.id, "onestrike").equals("1"))
				newitem.values.put("ammo", "1");
			
			if(db.GetValue(item.id, "magic").equals("1"))
				newitem.values.put("magic", "");
			if(db.GetValue(item.id, "ap").equals("1"))
				newitem.values.put("armorpiercing", "");
			
			if(db.GetValue(item.id, "an").equals("1"))
				newitem.values.put("armornegating", "");
			
			if(!db.GetValue(item.id, "secondaryeffect").equals("0") && !db.GetValue(item.id, "2nd_effect").equals(""))
				newitem.values.put("secondaryeffect", db.GetValue(item.id, "2nd_effect"));
			
			if(!db.GetValue(item.id, "secondaryeffectalways").equals("0") && !db.GetValue(item.id, "effauto").equals(""))
				newitem.values.put("secondaryeffectalways", db.GetValue(item.id, "effauto"));
			
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
