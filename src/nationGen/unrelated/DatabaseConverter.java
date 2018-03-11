package nationGen.unrelated;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.elmokki.Dom3DB;
import com.elmokki.Generic;

/**
 * Converts LarzM's mod inspector database files to weapons/armor files for nationgen
 * You'll need to convert the .csvs to semicolon-separated first.
 * @author Elmokki
 *
 */
public class DatabaseConverter {

	public static void main(String[] args) throws IOException
	{
		
		// Weapons
		Dom3DB base = new Dom3DB("db_conversion/weapons.csv");
		
		addAttributes(base, "db_conversion/attributes_by_weapon.csv");
		addEffects(base);
		
		base.saveToFile("db_conversion/output_weapon.csv");
		
		
		// Armor 
		base = new Dom3DB("db_conversion/armors.csv");
		addAttributes(base, "db_conversion/attributes_by_armor.csv");
		addArmorProt(base);
		base.saveToFile("db_conversion/output_armor.csv");


	}
	
	
	/**
	 * Adds armor protection
	 * 
	 * ...turns out cuirasses add limb protection etc and the formula is
	 * (chest + mean_limb) / 2 
	 * 
	 * @param db
	 */
	private static void addArmorProt(Dom3DB db)
	{

		
        Scanner file = null;

    	
		try {
			file = new Scanner(new FileInputStream("db_conversion/protections_by_armor.csv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file.nextLine();

        String line;

        List<String> lines = new ArrayList<String>();
        // Let's read the unit data then.

        while (file.hasNextLine())
        {
            // Read line
            line = file.nextLine();
            
            // Set units[id] to line of that unit.
            if(line.length() > 0 && line.split(";").length > 0)
            	lines.add(line);
            else if(line.length() > 0 && line.split("\t").length > 0)
            	lines.add(line);

        }

        file.close();
		
		for(String key : db.entryMap.keySet())
		{
			int[] zone = new int[7];
			for(String str : lines)
			{
				String[] stuff = str.split(";");
				if(stuff.length < 2)
					stuff = str.split("\t");
				
				if(stuff[2].equals(key))
				{
					int zonenbr = Integer.parseInt(stuff[0]);
					zone[zonenbr] = Integer.parseInt(stuff[1]);
				}
				
			}
			
			int prot = 0;

			
			// misc
			if(zone[6] != 0)
				prot = zone[6];
			
			// head
			if(zone[1] != 0)
				prot = zone[1];
			
			// shield
			if(zone[5] != 0)
				prot = zone[5];
			
			// others
			
			int tempprot = (zone[2] + (zone[3] + zone[4]) / 2) / 2;
			if(tempprot > 0)
				prot = tempprot;
			
			db.setValue(key, prot + "", "prot");
			
		}

		
			
			
	}
	
	/**
	 * Handles attributes.csv
	 * @param db
	 * @param attributes_by
	 */
	private static void addAttributes(Dom3DB db, String fname)
	{
        Scanner file = null;
		try {
			file = new Scanner(new FileInputStream(fname));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		file.nextLine();
		
		for(String key : db.entryMap.keySet())
		{
			db.setValue(key, "0", "ferrous");
			db.setValue(key, "0", "flammable");
			db.setValue(key, "0", "mmpenalty");

		}
		
		
        while (file.hasNextLine())
        {
            // Read line
        	String ssgd = file.nextLine();
            String[] line = ssgd.split("\t");
            
            
            if(!db.entryMap.keySet().contains(line[0]))
            	continue;
            
            String key = line[0];
    		String attr = line[1];

			if(attr != "")
			{
				if(attr.equals("266") || attr.equals("267"))
				
					db.setValue(key, "1", "ferrous");

			
				if(attr.equals("268") || attr.equals("269"))
					db.setValue(key, "1", "flammable");
		
				if(attr.equals("582"))
					db.setValue(key, line[2], "mmpenalty");

			}


        }
		
	}

	
	/**
	 * Adds effects for weapons from effects.csv
	 * @param db
	 */
	private static void addEffects(Dom3DB db)
	{
		Dom3DB attr = null;
		try {
			attr = new Dom3DB("db_conversion/effects.csv");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		for(String key : db.entryMap.keySet())
		{
			String attr_id = db.GetValue(key, "effect_record_id");
			
			// Damage
			db.setValue(key, attr.GetValue(attr_id, "raw_argument"), "dmg");

			// Effect numbers
			int effnbr = Integer.parseInt(attr.GetValue(attr_id, "effect_number"));
			if(effnbr == 2)
				db.setValue(key, "1", "dt_norm");
			else
				db.setValue(key, "0", "dt_norm");
			
			if(effnbr == 3)
				db.setValue(key, "1", "dt_stun");
			else
				db.setValue(key, "0", "dt_stun");
			
			if(effnbr == 7)
				db.setValue(key, "1", "dt_poison");
			else
				db.setValue(key, "0", "dt_poison");
			
			if(effnbr == 24)
				db.setValue(key, "1", "dt_holy");
			else
				db.setValue(key, "0", "dt_holy");
			
			if(effnbr == 32)
				db.setValue(key, "1", "dt_large");
			else
				db.setValue(key, "0", "dt_large");
			
			if(effnbr == 33)
				db.setValue(key, "1", "dt_small");
			else
				db.setValue(key, "0", "dt_small");
			
			if(effnbr == 66)
				db.setValue(key, "1", "dt_paralyze");
			else
				db.setValue(key, "0", "dt_paralyze");
			
			if(effnbr == 67)
				db.setValue(key, "1", "dt_weakness");
			else
				db.setValue(key, "0", "dt_weakness");
			
			if(effnbr == 73)
				db.setValue(key, "1", "dt_magic");
			else
				db.setValue(key, "0", "dt_magic");
			
			if(effnbr == 74)
				db.setValue(key, "1", "dt_raise");
			else
				db.setValue(key, "0", "dt_raise");
			
			if(effnbr == 103)
				db.setValue(key, "1", "dt_drain");
			else
				db.setValue(key, "0", "dt_drain");
			
			if(effnbr == 104)
				db.setValue(key, "1", "dt_weapondrain");
			else
				db.setValue(key, "0", "dt_weapondrain");
			
			if(effnbr == 107)
				db.setValue(key, "1", "dt_demon");
			else
				db.setValue(key, "0", "dt_demon");
			
			if(effnbr == 109)
				db.setValue(key, "1", "dt_cap");
			else
				db.setValue(key, "0", "dt_cap");
			
			// Effect bitmasks
			
			long effbm = Long.parseLong(attr.GetValue(attr_id, "modifiers_mask"));
			
			if(Generic.containsLongBitmask(effbm, 1))
				db.setValue(key, "0", "nostr");
			else
				db.setValue(key, "1", "nostr");
			
			if(Generic.containsLongBitmask(effbm, 2))
				db.setValue(key, "1", "2h");
			else
				db.setValue(key, "0", "2h");
			
			if(Generic.containsLongBitmask(effbm, 4))
				db.setValue(key, "1", "flail");
			else
				db.setValue(key, "0", "flail");
			
			if(Generic.containsLongBitmask(effbm, 8))
				db.setValue(key, "1", "demononly");
			else
				db.setValue(key, "0", "demononly");
			
			if(Generic.containsLongBitmask(effbm, 32))
				db.setValue(key, "1", "fire");
			else
				db.setValue(key, "0", "fire");
			
			if(Generic.containsLongBitmask(effbm, 64))
				db.setValue(key, "1", "ap");
			else
				db.setValue(key, "0", "ap");
			
			
			if(Generic.containsLongBitmask(effbm, 128))
				db.setValue(key, "1", "an");
			else
				db.setValue(key, "0", "an");
			
			if(Generic.containsLongBitmask(effbm, 512))
				db.setValue(key, "1", "cold");
			else
				db.setValue(key, "0", "cold");
			
			if(Generic.containsLongBitmask(effbm, 2048))
				db.setValue(key, "1", "shock");
			else
				db.setValue(key, "0", "shock");
			
			if(Generic.containsLongBitmask(effbm, 4096))
				db.setValue(key, "1", "mrnegates");
			else
				db.setValue(key, "0", "mrnegates");
			
			if(Generic.containsLongBitmask(effbm, 32768))
				db.setValue(key, "1", "sacredonly");
			else
				db.setValue(key, "0", "sacredonly");
			
			if(Generic.containsLongBitmask(effbm, 131072))
				db.setValue(key, "1", "mind");
			else
				db.setValue(key, "0", "mind");
			
			if(Generic.containsLongBitmask(effbm, 2097152))
				db.setValue(key, "0", "magic");
			else
				db.setValue(key, "1", "magic");
			
			if(Generic.containsLongBitmask(effbm, 134217728))
				db.setValue(key, "1", "bonus");
			else
				db.setValue(key, "0", "bonus");
			
			if(Generic.containsLongBitmask(effbm, 2147483648L))
				db.setValue(key, "1", "charge");
			else
				db.setValue(key, "0", "charge");
			
			if(Generic.containsLongBitmask(effbm, 137438953472L))
				db.setValue(key, "1", "norepel");
			else
				db.setValue(key, "0", "norepel");
		
			if(Generic.containsLongBitmask(effbm, 274877906944L))
				db.setValue(key, "1", "dt_pierce");
			else
				db.setValue(key, "0", "dt_pierce");
		
			if(Generic.containsLongBitmask(effbm, 549755813888L))
				db.setValue(key, "1", "dt_blunt");
			else
				db.setValue(key, "0", "dt_blunt");
			
			if(Generic.containsLongBitmask(effbm, 1099511627776L))
				db.setValue(key, "1", "dt_slash");
			else
				db.setValue(key, "0", "dt_slash");
			
			
			if(Generic.containsLongBitmask(effbm, 2199023255552L))
				db.setValue(key, "1", "acid");
			else
				db.setValue(key, "0", "acid");
			
			
			if(Generic.containsLongBitmask(effbm, 4398046511104L))
				db.setValue(key, "1", "sizeresist");
			else
				db.setValue(key, "0", "sizeresist");
		
			
			// Misc
			db.setValue(key, attr.GetValue(attr_id, "range_base"), "rng");
			
			
			if(!attr.GetValue(attr_id, "range_strength_divisor").equals(""))
			{
				db.setValue(key, "-" + attr.GetValue(attr_id, "range_strength_divisor"), "rng");
			}

			
			
			db.setValue(key, attr.GetValue(attr_id, "area_base"), "aoe");
			db.setValue(key, attr.GetValue(attr_id, "flight_sprite_number"), "flyspr");
			db.setValue(key, attr.GetValue(attr_id, "flight_sprite_length"), "animlength");

			if(db.GetValue(key, "ammo").equals("1"))
				db.setValue(key, "1", "onestrike");
			else
				db.setValue(key, "0", "onestrike");



		}
	}

}
