package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.List;

import nationGen.entities.Entity;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.ItemSet;
import nationGen.units.Unit;

import com.elmokki.Generic;

public class TroopTemplate
{
	Item armor;
	Unit template;
	Race race;
	String role;
	Pose pose;
	int maxvar;
	boolean canBeSecondary;				// Flag to denote units that can't be allied auxiliaries
	int dws = 0;
	ItemSet weapons = new ItemSet();
	ItemSet bonusweapons = new ItemSet();
	List<Unit> units = new ArrayList<Unit>();
	
	
	public static TroopTemplate getNew(Item armor, Race race, Unit template, String role, Pose pose, TroopGenerator tGen)
	{
		TroopTemplate tt = new TroopTemplate(armor, race, template, role, pose);
		tt.maxvar = tGen.getMaxVarieties(template);
		return tt;
	}
	
	public TroopTemplate(Item armor, Race race, Unit template, String role, Pose pose)
	{
		this.armor = armor;
		this.race = race;
		this.template = template;
		this.role = role;
		this.pose = pose;
		this.maxvar = 3;
		this.canBeSecondary = true;
	
		if(Generic.getTagValue(pose.tags, "primaryraceonly") != null)
			this.canBeSecondary = false;

	}
}
