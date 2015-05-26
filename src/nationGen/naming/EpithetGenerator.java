package nationGen.naming;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;

public class EpithetGenerator {
	private NationGen ngen;
	
	List<NameFilter> mageepithetparts = new ArrayList<NameFilter>();
	
	public EpithetGenerator(NationGen ngen)
	{
		this.ngen = ngen;
	}
	
	public void giveEpithet(Nation n)
	{
		mageepithetparts = ChanceIncHandler.retrieveFilters("epithet_era_names", "default_epithet_parts", ngen.miscnames, null, n.races.get(0));

		
		String epithet = "";
		
		
		double r = n.random.nextDouble();
		
		if(r > 0.55 && !MageDescriber.getCommonNoun(n).equals("")) // Second condition is relatively rare
		{
			NameFilter part = NameFilter.getRandom(n.random, mageepithetparts);
			
			if(n.random.nextDouble() < 0.5 && MageDescriber.getCommonNoun(n).toString().split(" ").length < 2)
			{
				epithet = MageDescriber.getCommonNoun(n) + " " + part.name;
			}
			else
				epithet = part.name + " of the " + MageDescriber.getCommonNoun(n);
		}
		else if(r > 0.3 && !MageDescriber.getCommonName(n).toString().equals("Mage"))
		{
			NameFilter part = NameFilter.getRandom(n.random, mageepithetparts);
			Name common = MageDescriber.getCommonName(n);
			common.setType(Generic.plural(common.type.toString()));
			epithet = part.name + " of the " + common.toString();

			
		}
		else
		{

			epithet = n.sites.get(0).name;
		}
		
		epithet = Generic.capitalize(epithet);
		n.commands.add(new Command("#epithet", "\"" + epithet + "\""));
	}
}
