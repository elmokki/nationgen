package nationGen.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;



import nationGen.entities.Entity;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;





import nationGen.nation.Nation;

import com.elmokki.Dom3DB;
import com.elmokki.Generic;


public class ItemSet extends ArrayList<Item> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String slot = "";
	
	// Overwriting add to avoid nulls
	public boolean add(Item value)
	{
		if(value != null)
			return super.add(value);
		
		//System.out.println("Tried to add null to an itemset.");
		return false;
	}


	
	public ItemSet filterProt(Dom3DB armordb, int min, int max)
	{
		return filterProt(armordb, min, max, false);
	}
	
	public ItemSet filterSameSprite(ItemSet old)
	{
		ItemSet newlist = new ItemSet();
		newlist.addAll(this);
		
		for(Item i : this)
			for(Item i2 : old)
				if(i.sprite.equals(i2.sprite))
					newlist.remove(i);
		
		return newlist;
	}
	
	public ItemSet filterProt(Dom3DB armordb, int min, int max, boolean includeDef)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
		{
			if(!includeDef && armordb.GetInteger(i.id, "prot") <= max && armordb.GetInteger(i.id, "prot") >= min)
				newlist.add(i);
			else if(includeDef && armordb.GetInteger(i.id, "prot") +  armordb.GetInteger(i.id, "def") <= max && armordb.GetInteger(i.id, "prot") >= min)
				newlist.add(i);
		}
		return newlist;
	}
	
	public ItemSet filterDef(Dom3DB armordb, int min, int max)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
			if(armordb.GetInteger(i.id, "def") <= max && armordb.GetInteger(i.id, "def") >= min)
				newlist.add(i);
		
		
		
		return newlist;
	}
	
	public ItemSet filterMinMaxProt(int itemprot)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
		{
			int minprot = 0;
			int maxprot = 100;
			for(String tag : i.tags)
			{
				List<String> args = Generic.parseArgs(tag);
				if(args.get(0).equals("minprot"))
					minprot = Integer.parseInt(args.get(1));
				else if(args.get(0).equals("maxprot"))
					maxprot = Integer.parseInt(args.get(1));
			}
			
			
			if(itemprot >= minprot && itemprot <= maxprot)
			{
				newlist.add(i);
			}
		}
		
		
		
		return newlist;
	}
		
	public Item getItemWithID(String string, String slot)
	{
		ItemSet possibles = this; //this.filterTheme("elite", false).filterTheme("sacred", false);
		for(Item item : possibles)
		{
			if(item.id.equals(string) && item.slot.equals(slot))
				return item;
		}
		return null;
	}
	
	public ItemSet getItemsWithID(String string, String slot)
	{
		ItemSet newset = new ItemSet();
		ItemSet possibles = this; //this.filterTheme("elite", false).filterTheme("sacred", false);
		for(Item item : possibles)
		{
			if(item.id.equals(string) && item.slot.equals(slot))
				newset.add(item);
		}
		return newset;
	}
	
	public Item getItemWithName(String name, String slot)
	{
		Iterator<Item> itr = this.iterator();
		while(itr.hasNext())
		{
			Item item = itr.next();
			if(item.name.equals(name) && item.slot.equals(slot))
				return item;
		}
		return null;
	}
	
	public boolean alreadyHas(Item i)
	{
		if(i == null)
			return true;
		
		Iterator<Item> itr = this.iterator();
		while(itr.hasNext())
		{
			Item item = itr.next();
			if(!item.id.equals("-1"))
			{
				if(item.id.equals(i.id) && i.armor == item.armor)
					return true;
			}
			else
				if(item.id.equals(i.id) && item.name.equals(i.name))
					return true;
		}
		return false;
	}
	
	public ItemSet filterDom3DB(String value, String wanted, boolean keepwanted, Dom3DB db)
	{

		
		ItemSet newlist = new ItemSet();
		for(Item i : this)
			if(db.GetValue(i.id, value, "0").equals(wanted) == keepwanted)
				newlist.add(i);
		
		return newlist;
	}
	
	public ItemSet filterDom3DBInteger(String value, int wanted, boolean below, Dom3DB db)
	{
		
		ItemSet newlist = new ItemSet();
		for(Item i : this)
			if(below && db.GetInteger(i.id, value) < wanted)
				newlist.add(i);
			else if(!below && db.GetInteger(i.id, value) > wanted)
				newlist.add(i);
		
		return newlist;
	}
	
	public ItemSet filterForRole(String role, Race race)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
		{
			for(Pose p : race.poses)
			{
				if(!p.roles.contains(role))
					continue;
				
				if(p.getItems(i.slot) != null)
					for(Item i2 : p.getItems(i.slot))
					{
						if(i2.id.equals(i.id) && i.id.equals("-1"))
							newlist.add(i2);
						else if(i2.id.equals(i.id) && i.name.equals(i2.name))
							newlist.add(i2);
						else if(i2.id.equals(i.id) && i.sprite.equals(i2.sprite))
							newlist.add(i2);
					}
			}
		}
		
		return newlist;
	}
	
	public ItemSet filterForPosesWith(String role, Race race, Item olditem)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
		{
			for(Pose p : race.poses)
			{
				if(!p.roles.contains(role) || p.getItems(olditem.slot) == null || !p.getItems(olditem.slot).contains(olditem))
					continue;
				
				if(p.getItems(i.slot) != null && p.getItems(i.slot).contains(i) && !newlist.contains(i))
					newlist.add(i);
			}
		}
		
		return newlist;
	}
	
	public Item getRandom(ChanceIncHandler ch, Random random)
	{
		return Entity.getRandom(random, ch.handleChanceIncs(this));
	}
	
	public ItemSet filterForPose(Pose p)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
		{
	
			if(p.getItems(i.slot) != null)
				for(Item i2 : p.getItems(i.slot))
				{
					if(i.id.equals(i2.id) && !i.id.equals("-1") && (i.name.equals(i2.name) || i.sprite.equals(i2.name) || i2.tags.contains("replacement " + i.name)))
						newlist.add(i2);
					else if(i.id.equals(i2.id) && (i.name.equals(i2.name) || i.sprite.equals(i2.name)))
						newlist.add(i2);
		
				}
		}
		
		if(newlist.possibleItems() == 0)
		{	
			for(Item i : this)
			{
				if(p.getItems(i.slot) != null)
					for(Item i2 : p.getItems(i.slot))
					{	
						if(i.id.equals(i2.id) && !i.id.equals("-1"))
							newlist.add(i2);
						else if(i.id.equals(i2.id) && (i.name.equals(i2.name) || i.sprite.equals(i2.sprite)))
							newlist.add(i2);
			
					}
			}
		}
		
		

		
		return newlist;
	}
	
	
	public ItemSet getCopy()
	{
		ItemSet newset = new ItemSet();
		newset.slot = this.slot;
		for(Item i : this)
			newset.add(i);
		
		return newset;
	}
	

	


	
	
	public ItemSet filterSlot(String slot)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
			if(i.slot.equals(slot))
				newlist.add(i);
		
		return newlist;
	}
	
	
	public ItemSet filterTag(String tag, boolean keepTag)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
			if(i.tags.contains(tag) == keepTag)
				newlist.add(i);

		return newlist;
	}
	
	public ItemSet filterTheme(String tag, boolean keepTag)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
			if(i.themes.contains(tag) == keepTag)
				newlist.add(i);

		return newlist;
	}
	
	
	public int possibleItems()
	{
		int items = 0;
		for(Item item : this)
			if(item.basechance > 0)
				items++;
		
		return items;
	}
	
	public ItemSet filterArmor(boolean keepArmor)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
			if(i.armor == keepArmor)
				newlist.add(i);

		return newlist;
	}
	
	public ItemSet filterAbstracts()
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
			if(!i.id.equals(-1))
				newlist.add(i);
		
		return newlist;

	}
	
	public ItemSet filterThoseNotInArmorRange(int armor)
	{
		ItemSet newlist = new ItemSet();
		for(Item i : this)
		{
			int minprot = 0;
			int maxprot = 100;
			for(String tag : i.tags)
			{
				if(tag.startsWith("minprot"))
					minprot = Integer.parseInt(tag.split(" ")[1]);
				if(tag.startsWith("maxprot"))
					maxprot = Integer.parseInt(tag.split(" ")[1]);
			}
			if(armor >= minprot && armor <= maxprot)
				newlist.add(i);
		}
		
		return newlist;

	}
	
	public ItemSet filterImpossibleAdditions(ItemSet old)
	{
		if(old == null)
			return this;
		
		List<Item> list = new ArrayList<Item>();
		list.addAll(old);
		return filterImpossibleAdditions(list);
	}
	
	public ItemSet filterImpossibleAdditions(List<Item> list)
	{
	
		ItemSet newlist = new ItemSet();
		newlist.addAll(this);
		
		for(Item i : list)
		{
			newlist.remove(i);
			
			// Remove stuff with the same ids or names or customid tag
			List<Item> derps = new ArrayList<Item>();

			for(Item i2 : newlist)
			{
				
			
				
				if(i2.armor == i.armor && i2.id.equals(i.id) && i.slot.equals(i2.slot) && (!i.id.equals("-1") || !i.id.equals("-2")))
					derps.add(i2);
				else if(i2.name.equals(i.name) && i.slot.equals(i2.slot))
					derps.add(i2);
				
				
			}
			
			
			
			for(Item i2 : derps)
				newlist.remove(i2);
			

		}
		return newlist;
	}
	
	
	
	public LinkedHashMap<Item, Double> getHashMap()
	{
		LinkedHashMap<Item, Double> set = new LinkedHashMap<Item, Double>();
		for(Item i : this)
		{
			set.put(i, i.basechance);			
		}
		return set;
	}


	

	



}
