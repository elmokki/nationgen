package nationGen.naming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.elmokki.Generic;

import nationGen.items.Item;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class DescriptionReplacer {
	
	public HashMap<String, String> descs = new HashMap<String, String>();
	private Nation n;
	
	public DescriptionReplacer(Nation n)
	{
		this.n = n;
		descs.put("%nation%", n.name);
		descs.put("%primaryrace%", n.races.get(0).toString());
		descs.put("%primaryrace_plural%", Generic.capitalizeFirst(n.races.get(0).toString()));
		descs.put("%mages%", MageDescriber.getCommonName(n).toString());
		descs.put("%magenoun%", MageDescriber.getCommonNoun(n).toString());
		descs.put("%sacredname%", n.unitlists.get("sacreds").get(0).name.toString());
		descs.put("%sacredname_plural%", Generic.capitalizeFirst(n.unitlists.get("sacreds").get(0).name.toString()));
		

	}
	
	public String replace(String line)
	{
		for(String key : descs.keySet())
			line = line.replaceAll(key, descs.get(key));
		
		return line;
	}
	
	public void calibrate(Unit u)
	{
		descs.put("%unitname%", u.name.toString());
		descs.put("%race%", u.race.toString().toLowerCase());
		descs.put("%race_plural%", Generic.plural(u.race.toString().toLowerCase()));
		if(u.getSlot("mount") != null && !Generic.getTagValue(u.getSlot("mount").tags, "animal").equals(""))
		{
	
			descs.put("%mount%", Generic.getTagValue(u.getSlot("mount").tags, "animal"));
			descs.put("%mount_plural%", Generic.plural(Generic.getTagValue(u.getSlot("mount").tags, "animal")));

		}
		else
		{
			descs.put("%mount%", "unspecified mount");
			descs.put("%mount_plural%", "unspecified mounts");

		}	
	}
	
	public void calibrate(List<Unit> units)
	{
		List<String> weapons = new ArrayList<String>();
		List<String> weapons_plural = new ArrayList<String>();

	
		for(Unit u : units)
		{
			for(Item i : u.slotmap.values())
			{
				if(i != null && !i.armor && !i.id.equals("-1"))
				{
					if(!n.nationGen.weapondb.GetValue(i.id, "weapon_name").equals("") && !weapons.contains(n.nationGen.weapondb.GetValue(i.id, "weapon_name")))
					{
						weapons.add(NameGenerator.addPreposition(n.nationGen.weapondb.GetValue(i.id, "weapon_name").toLowerCase()));
						weapons_plural.add(NameGenerator.plural(n.nationGen.weapondb.GetValue(i.id, "weapon_name").toLowerCase()));

					}
					else
					{
						weapons.add("NOT IN WEAPONDB: " + i.name + " in " + i.slot);
						weapons_plural.add("NOT IN WEAPONDB: " + i.name + " in " + i.slot);	

					}
				}
			}
		}
		if(weapons.size() > 3)
		{
			weapons.clear();
			weapons_plural.clear();
			
			
			for(Unit u : units)
			{
				for(Item i : u.slotmap.values())
				{
					if(i != null && !i.armor && !i.id.equals("-1"))
					{
						if(!n.nationGen.weapondb.GetValue(i.id, "weapon_name").equals("") && !weapons.contains(n.nationGen.weapondb.GetValue(i.id, "weapon_name")))
						{
							String s = "";
							List<String> tmp = new ArrayList<String>();
							
							if(n.nationGen.weapondb.GetValue(i.id, "dt_b").equals("1"))
								tmp.add("blunt");
							if(n.nationGen.weapondb.GetValue(i.id, "dt_s").equals("1"))
								tmp.add("slashing");
							if(n.nationGen.weapondb.GetValue(i.id, "dt_p").equals("1"))
								tmp.add("piercing");
							
							for(String str : tmp)
								if(!str.equals("") && !weapons.contains(str))
									weapons.add(str);

						}
					}
				}
			}
			
		
			
			if(weapons.size() < 3)
			{
				descs.put("%weapons%", NameGenerator.writeAsList(weapons, false) + " weapons");
				descs.put("%weapons_plural%", NameGenerator.writeAsList(weapons, false) + " weapons");
			}
			else
			{
				descs.put("%weapons%", "various weapons");
				descs.put("%weapons_plural%", "various weapons");
			}

		}
		else
		{
			descs.put("%weapons%", NameGenerator.writeAsList(weapons, false, "or"));
			descs.put("%weapons_plural%", NameGenerator.writeAsList(weapons_plural, false));
		}
		weapons.clear();
		int minprot = 100;
		int maxprot = 0;
		
		for(Unit u : units)
		{
			Item i = u.getSlot("armor");
			
			
			int prot = 0;
			
			if(i != null && i.armor && !i.id.equals("-1"))
			{
				prot = n.nationGen.armordb.GetInteger(i.id, "prot", 0);

				if(!n.nationGen.weapondb.GetValue(i.id, "armorname").equals("") && !weapons.contains(n.nationGen.weapondb.GetValue(i.id, "armorname")))
					weapons.add(n.nationGen.weapondb.GetValue(i.id, "armorname"));
				else
					weapons.add("NOT IN ARMORDB: " + i.name + " in " + i.slot);	
			}
		
			
			if(prot < minprot)
				minprot = prot;
			else if(prot > maxprot)
				maxprot = prot;
			
			
		}
		
		descs.put("%armors%", NameGenerator.writeAsList(weapons, false));

		weapons.clear();
		

		if(minprot <= 10)
			weapons.add("light");
		
		if(minprot <= 15 && maxprot > 10)
			weapons.add("medium");
		
		if(maxprot > 15)
			weapons.add("heavy");

		
		if(weapons.size() >= 3)
		{
			weapons.clear();
			weapons.add("all kinds of");
		}
		
		descs.put("%armortypes%",  NameGenerator.writeAsList(weapons, false));
		
		
	}
}
