package nationGen.rostergeneration.montagtemplates;

import java.util.Random;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Pose;
import nationGen.nation.Nation;
import nationGen.rostergeneration.SacredGenerator;
import nationGen.units.Unit;

public class SacredMontagTemplate extends MontagTemplate {

  public int power = 1;
  public boolean sacred = true;
  private SacredGenerator sGen;
  private Random r;

  public SacredMontagTemplate(
    Nation n,
    NationGen ngen,
    NationGenAssets assets
  ) {
    sGen = new SacredGenerator(ngen, n, assets);
    r = new Random(n.random.nextInt());
  }

  public Unit generateUnit(Unit u, Pose p) {
    double epicchance = r.nextDouble() * 0.5 + power * 0.25 + 0.25;

    Unit newunit = null;
    int tries = 0;
    while (tries < 100 && newunit == null) {
      tries++;
      int powernow = (int) Math.round(r.nextDouble() * (power + 2));

      newunit = sGen.unitGen.generateUnit(u.race, p);
      handleFilterInheritance(u, newunit);
      newunit = sGen.getSacredUnit(newunit, powernow, sacred, epicchance);

      sGen.addSacredCostMultipliers(u, power);
      sGen.determineIfCapOnly(u, true);
    }

    return newunit;
  }
}
