package nationGen.entities;


import com.elmokki.Generic;
import nationGen.NationGen;
import nationGen.items.CustomItem;
import nationGen.items.Item;
import nationGen.misc.ArgParser;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;


public class Pose extends Filter {
	
	private LinkedHashMap<String, ItemSet> items = new LinkedHashMap<>();
	public List<String> roles = new ArrayList<>();
	public String renderorder = "shadow cloakb mount basesprite legs shirt armor cloakf bonusweapon weapon offhandw hands hair helmet offhanda overlay extra1 extra2 extra3";
	public List<AbilityTemplate> templates = new ArrayList<>();
	
	public Pose(NationGen nationGen)
	{
		super(nationGen);
	}
	
	public Set<String> getListOfSlots()
	{
		return items.keySet();
	}
	
	public ItemSet getItems(String slot)
	{
		if(items.get(slot) == null)
			return null;
		return items.get(slot).getCopy();
	}
	
	/**
	 * Loads item definitions from file.
	 * @param file
	 */
	private ItemSet loadItems(String file, int offsetx, int offsety, String slot)
	{


			ItemSet items = new ItemSet();
			
		
			items.addAll(Item.readFile(nationGen, file, Item.class));
			for(Item i : items)
			{
				try
				{
					i.slot = slot;
					if(i.renderslot.equals(""))
						i.renderslot = slot;
				}
				catch(NullPointerException e)
				{
					System.out.println("WARNING! File " + file + " produced a null item! Make sure there is a #new command for each item!");
				}
			}
			items.slot = slot;
			
			
			
			
			
			if(offsetx != 0 || offsety != 0)
			{
				for(Item i : items)
				{
					i.setOffsetX(i.getOffsetX() + offsetx);
					i.setOffsetY(i.getOffsetY() + offsety);
					

				}
			}
		

			
	

			return items;
	}
	
	
	public boolean compatibleWith(Unit u, List<String> slots, boolean include)
	{
		for(String slot : u.slotmap.keySet())
		{
			if(slots != null && slots.contains(slot) == !include)
				continue;
			
			Item ui = u.getSlot(slot);
			
			// Same name and slot
			for(Item i : this.items.get(slot))
			{
				if(i.name.equals(ui.name) && i.id.equals(ui.id))
					return true;
			}
			
			// Same image and id
			for(Item i : this.items.get(slot))
			{
				if(i.sprite.equals(ui.sprite) && i.id.equals(ui.id))
					return true;
			}
		}
		return false;
	}
	
	
	public String toString()
	{
		String str = this.roles.toString();
		if(this.name != null && !this.name.equals(""))
			str = this.name;
		
		return str;
	}
        
	@Override
	public void handleOwnCommand(Command str)
	{
		switch (str.command) {
			case "#role":
				this.roles.add(str.args.get(0).get());
				break;
			case "#renderorder":
				this.renderorder = str.args.get(0).get();
				break;
			case "#command":
				this.commands.add(str.args.get(0).getCommand());
				break;
			case "#load":
				ArgParser parser = str.args.parse();
				String slot = parser.nextString();
				String file = parser.nextString();
				parser.nextOptionalFlag("offsetx");
				int offsetx = parser.nextOptionalInt().orElse(0);
				parser.nextOptionalFlag("offsety");
				int offsety = parser.nextOptionalInt().orElse(0);
				
				ItemSet set = loadItems(file, offsetx, offsety, slot);
				
				for(Item i : set)
				{
					if(!Generic.isNumeric(i.id))
					{
						CustomItem citem = nationGen.GetCustomItemsHandler().getCustomItem(i.id);
						if(!citem.armor)
							nationGen.weapondb.addToMap(i.id, citem.getHashMap());
						else
						{
							nationGen.armordb.addToMap(i.id, citem.getHashMap());
						}
					}
				}
				
				// Put itemset to it's place
				if(items.get(slot) == null)
					this.items.put(slot, set);
				else
					items.get(slot).addAll(set);
				
				break;
			default:
				super.handleOwnCommand(str);
		}
	}
}
