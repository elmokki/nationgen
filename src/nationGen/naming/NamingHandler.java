package nationGen.naming;

import nationGen.NationGen;
import nationGen.nation.Nation;

public class NamingHandler {
	
	private NationGen ng;

	public NamingHandler(NationGen ng)
	{
		this.ng = ng;
	}

	public void nameMages(Nation n)
	{
		MageNamer mNamer = new MageNamer(ng);
		mNamer.execute(n);
	}
	
	public void namePriests(Nation n)
	{
		
		PriestNamer pNamer = new PriestNamer(ng);
		pNamer.execute(n);
	}
	
	public void nameSacreds(Nation n)
	{
		SacredNamer sNamer = new SacredNamer();
		sNamer.nameSacreds(n);
	}
	
	public void nameTroops(Nation n)
	{
		TroopNamer tnamer = new TroopNamer();
		tnamer.execute(n);
	}
	
	public void giveEpithet(Nation n)
	{
		EpithetGenerator epiGen = new EpithetGenerator(ng);
		epiGen.giveEpithet(n);
	}
	
	public void describeNation(Nation n)
	{
		new NationDescriber(n);
	}
	

}
