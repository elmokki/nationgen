package nationGen.rostergeneration;

import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.ItemSet;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;

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
	List<Unit> units = new ArrayList<>();
	
	
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
		this.canBeSecondary = !pose.tags.containsName("primaryraceonly");

	}
}
