package nationGen.restrictions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.text.html.parser.Entity;

import nationGen.NationGenAssets;
import nationGen.entities.Filter;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class UnitFilterRestriction
  extends TwoListRestrictionWithComboBox<String, String> {

  public List<String> possibleFilterNames = new ArrayList<String>();

  private NationGenAssets assets;

  private String[] ownoptions = {
    "All",
    "Troops",
    "Commanders",
    "Sacred troops",
    "None",
  };

  public UnitFilterRestriction(NationGenAssets assets) {
    super(
      "<html>Nation needs to have at least one unit with a filter on the right box.</html>",
      "Unit filter"
    );

    this.assets = assets;
    this.comboboxlabel = "Units to match:";

    this.assets.filters
      .keySet()
      .stream()
      .sorted()
      .forEach(filterCategory -> {
        this.assets.filters
          .get(filterCategory)
          .stream()
          .sorted(Comparator.comparing(Filter::getName))
          .forEach(f -> {
            rmodel.addElement(filterCategory + ": " + f);
        });
      });

    this.comboboxoptions = ownoptions;
    this.extraTextField = true;
    this.textFieldLabel = "Search:";
  }

  @Override
  public NationRestriction getRestriction() {
    UnitFilterRestriction res = new UnitFilterRestriction(assets);
    for (
      int i = 0;
      i < chosen.getModel().getSize();
      i++
    ) res.possibleFilterNames.add(chosen.getModel().getElementAt(i));

    res.comboselection = this.comboselection;
    return res;
  }

  @Override
  public boolean doesThisPass(Nation n) {
    if (possibleFilterNames.size() == 0) {
      System.out.println("Units of filter restriction has no filters set!");
      return true;
    }

    String targs = comboselection; // what the filter TARGetS (None applies to "all" of the units)
    if (comboselection == null) {
      comboselection = targs = "All";
    } else if (comboselection.equals("None")) {
      targs = "All";
    }

    return (
      (((targs.equals("Troops") || targs.equals("All")) &&
          n.selectTroops().anyMatch(this::checkUnit)) ||
        ((targs.equals("Commanders") || targs.equals("All")) &&
          n.selectCommanders().anyMatch(this::checkUnit)) ||
        ((targs.equals("Sacred troops")) &&
          n.selectTroops("sacred").anyMatch(this::checkUnit))) ^
      comboselection.equals("None")
    );
  }

  private boolean checkUnit(Unit u) {
    for (String str : possibleFilterNames) {
      int index = str.indexOf(": ");
      String comp = str.substring(index + 2);
      for (Filter f : u.appliedFilters) {
        if (f.toString().equals(comp)) return true;
      }
    }
    return false;
  }

  @Override
  public RestrictionType getType() {
    return RestrictionType.UnitFilter;
  }

  @Override
  protected void textFieldUpdate() {
    rmodel.clear();
    for (String str : assets.filters.keySet()) {
      for (Filter f : assets.filters.get(str)) {
        String add = str + ": " + f;
        if (
          add.contains(textfield.getText()) || textfield.getText().length() == 0
        ) {
          rmodel.addElement(add);
        }
      }
    }
  }
}
