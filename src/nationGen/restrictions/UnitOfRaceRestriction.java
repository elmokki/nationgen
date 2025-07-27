package nationGen.restrictions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import nationGen.NationGenAssets;
import nationGen.entities.Race;
import nationGen.nation.Nation;

public class UnitOfRaceRestriction
  extends TwoListRestrictionWithComboBox<Race, String> {

  public List<String> possibleRaceNames = new ArrayList<String>();

  private NationGenAssets assets;

  public UnitOfRaceRestriction(NationGenAssets assets) {
    super(
      "Nation needs to have at least one unit of a race on the right box",
      "Unit of race"
    );
    this.assets = assets;

    this.comboboxlabel = "Units to match:";

    this.assets.races
      .stream()
      .sorted(Comparator.comparing(Race::getName))
      .forEach(r -> rmodel.addElement(r));

    this.comboboxoptions = new String[] {
      "All",
      "Troops",
      "Commanders",
      "Sacred troops",
    };
  }

  @Override
  public NationRestriction getRestriction() {
    UnitOfRaceRestriction res = new UnitOfRaceRestriction(assets);
    for (
      int i = 0;
      i < chosen.getModel().getSize();
      i++
    ) res.possibleRaceNames.add(chosen.getModel().getElementAt(i).name);

    res.comboselection = this.comboselection;
    return res;
  }

  @Override
  public boolean doesThisPass(Nation n) {
    if (possibleRaceNames.size() == 0) {
      System.out.println("Units of race nation restriction has no races set!");
      return true;
    }

    if (comboselection == null) comboselection = "All";

    return (
      ((comboselection.equals("Troops") || comboselection.equals("All")) &&
        n
          .selectTroops()
          .anyMatch(u -> possibleRaceNames.contains(u.race.name))) ||
      ((comboselection.equals("Commanders") || comboselection.equals("All")) &&
        n
          .selectCommanders()
          .anyMatch(u -> possibleRaceNames.contains(u.race.name))) ||
      ((comboselection.equals("Sacred troops")) &&
        n
          .selectTroops("sacred")
          .anyMatch(u -> possibleRaceNames.contains(u.race.name)))
    );
  }

  @Override
  public RestrictionType getType() {
    return RestrictionType.UnitRace;
  }
}
