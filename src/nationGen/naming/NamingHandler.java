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
		MageNamer mNamer = new MageNamer(ng, assets);
		mNamer.execute(n);
	}
	
	public void namePriests(Nation n)
	{
		
		PriestNamer pNamer = new PriestNamer(ng, assets);
		pNamer.execute(n);
	}
	
	public void nameSacreds(Nation n)
	{
		SacredNamer sNamer = new SacredNamer(assets);
		sNamer.nameSacreds(n);
	}
	
	public void nameHeroes(Nation n, NameGenerator nGen)
	{
		
		
	}
	
	public void nameTroops(Nation n)
	{
		TroopNamer tnamer = new TroopNamer(assets);
		tnamer.execute(n);
	}
	
	public void giveEpithet(Nation n)
	{
		EpithetGenerator epiGen = new EpithetGenerator(assets);
		epiGen.giveEpithet(n);
	}
	
	public void describeNation(Nation n)
	{
		new NationDescriber(n, assets);
	}
	

}
