package nationGen.restrictions;

import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;

public class UnitCommandRestriction extends TextBoxListRestriction {
	private List<String> commandRestrictions = new ArrayList<>();
	
	public UnitCommandRestriction()
	{
		super("<html>Nation needs to have at least one unit with a command on the right box. If you leave arguments empty, arguments on unit do not matter. "
				+ "If you specify arguments, they need to match exactly.</html>", "Unit command");

		this.hascombobox = true;
		String[] ops = {"All", "Troops", "Commanders", "Sacred troops"};
		this.comboboxoptions = ops;
		this.comboboxlabel = "Units to match:";
		this.textFieldLabel = "Command to add:";
		this.textfieldDefaultText = "#flying";
		
	}
	


	@Override
	public NationRestriction getRestriction() {
		UnitCommandRestriction res = new UnitCommandRestriction();
		for(int i =0; i < chosen.getModel().getSize(); i++)
			res.commandRestrictions.add(chosen.getModel().getElementAt(i));
		
		res.comboselection = this.comboselection;
		return res;
	}
	
	@Override
	public boolean doesThisPass(Nation n) {
		if(commandRestrictions.size() == 0)
		{
			System.out.println("Unit command nation restriction has no races set!");
			return true;
		}
		
		boolean pass = false;
	
		if(comboselection == null)
			comboselection = "All";
		
		if(comboselection.equals("Troops") || comboselection.equals("All"))
			pass = checkUnits(n.generateTroopList());
		
		if(!pass && (comboselection.equals("Commanders") || comboselection.equals("All")))
			pass = checkUnits(n.generateComList());
			
		if(!pass && comboselection.equals("Sacred troops"))
			pass = checkUnits(n.generateUnitList("sacred"));
		
		
		return pass;
	}
	
	private boolean checkUnits(List<Unit> list)
	{
		
		for(Unit u : list)
		{
			for(String str : commandRestrictions)
			{
				Command oc = Command.parse(str);
				for(Command c : u.getCommands())
				{
					if(c.command.equals(oc.command) && (oc.args.isEmpty() || c.equals(oc)))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

    @Override
    public RestrictionType getType()
    {
        return RestrictionType.UnitCommand;
    }
	
}
