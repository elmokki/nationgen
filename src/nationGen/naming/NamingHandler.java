package nationGen.naming;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.nation.Nation;

public class NamingHandler {
	
	private NationGen ng;
	private NationGenAssets assets;

	public NamingHandler(NationGen ng, NationGenAssets assets)
	{
		this.ng = ng;
		this.assets = assets;
	}

	public void nameMages(Nation n)
	{
		MageNamer mNamer = new MageNamer(ng);
		mNamer.execute(n, assets);
	}
	
	public void namePriests(Nation n)
	{
		
		PriestNamer pNamer = new PriestNamer(ng);
		pNamer.execute(n);
	}
	
	public void nameSacreds(Nation n)
	{
		SacredNamer sNamer = new SacredNamer();
		sNamer.nameSacreds(n, assets);
	}
	
	public void nameTroops(Nation n)
	{
		TroopNamer tnamer = new TroopNamer();
		tnamer.execute(n, assets);
	}
	
	public void giveEpithet(Nation n)
	{
		EpithetGenerator epiGen = new EpithetGenerator(ng);
		epiGen.giveEpithet(n, assets);
	}
	
	public void describeNation(Nation n)
	{
		new NationDescriber(n, assets);
	}
	

}
