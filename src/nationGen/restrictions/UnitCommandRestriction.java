package nationGen.restrictions;



import java.util.List;
import java.util.ArrayList;




import nationGen.NationGen;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class UnitCommandRestriction extends TextBoxListRestriction {
	public List<String> possibleRaceNames = new ArrayList<String>();
	
	private NationGen ng;
	public UnitCommandRestriction(NationGen ng)
	{
		super(ng, "<html>Nation needs to have at least one unit with a command on the right box. If you leave arguments empty, arguments on unit do not matter. "
				+ "If you specify arguments, they need to match exactly.</html>", "Unit command");
		this.ng = ng;
		

		
		this.hascombobox = true;
		String[] ops = {"All", "Troops", "Commanders", "Sacred troops"};
		this.comboboxoptions = ops;
		this.comboboxlabel = "Units to match:";
		this.textFieldLabel = "Command to add:";
		this.textfieldDefaultText = "#flying";
		
	}
	


	@Override
	public NationRestriction getRestriction() {
		UnitCommandRestriction res = new UnitCommandRestriction(ng);
		for(int i =0; i < chosen.getModel().getSize(); i++)
			res.possibleRaceNames.add(chosen.getModel().getElementAt(i));
		
		res.comboselection = this.comboselection;
		return res;
	}
	
	@Override
	public boolean doesThisPass(Nation n) {
		if(possibleRaceNames.size() == 0)
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
			for(String str : possibleRaceNames)
			{
				Command oc = Command.parseCommand(str);
				for(Command c : u.getCommands())
				{
					if(c.command.equals(oc.command))
					{

						if(c.args.size() == oc.args.size() || oc.args.size() == 0)
						{
							boolean fine = true;

							for(int i = 0; i < oc.args.size(); i++)
								if(!c.args.get(i).equals(oc.args.get(i)))
									fine = false;
							
							if(fine)
								return true;
						}
						
					}
				}
	
				
			}
		}
		return false;
	}
	
	@Override
	public NationRestriction getInstanceOf() {
		return new UnitCommandRestriction(ng);
	}
	
}
