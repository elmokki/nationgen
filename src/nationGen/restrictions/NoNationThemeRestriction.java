package nationGen.restrictions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import nationGen.NationGenAssets;
import nationGen.entities.Theme;
import nationGen.nation.Nation;

public class NoNationThemeRestriction extends TwoListRestriction<String> {

  public List<String> forbiddenThemeNames = new ArrayList<String>();

  private NationGenAssets assets;

  public NoNationThemeRestriction(NationGenAssets assets) {
    super(
      "Nation or primary race cannot have a theme named like one of the themes in the right box",
      "No nation or primary race theme"
    );

    this.assets = assets;

    this.assets.themes
      .keySet()
      .stream()
      .sorted()
      .forEach(themeCategory -> {
        this.assets.themes
          .get(themeCategory)
          .stream()
          .sorted(Comparator.comparing(Theme::getName))
          .forEach(t -> {
            rmodel.addElement(themeCategory + " - " + t);
        });
      });

    this.extraTextField = true;
    this.textFieldLabel = "Search:";
  }

  @Override
  public NationRestriction getRestriction() {
    NoNationThemeRestriction res = new NoNationThemeRestriction(assets);
    for (
      int i = 0;
      i < chosen.getModel().getSize();
      i++
    ) res.forbiddenThemeNames.add(chosen.getModel().getElementAt(i));
    return res;
  }

  @Override
  public boolean doesThisPass(Nation n) {
    if (forbiddenThemeNames.size() == 0) {
      System.out.println(
        "Nation or primary race theme nation restriction has no races set!"
      );
      return true;
    }

    boolean hasAnyForbiddenNationTheme = n.nationthemes.stream().anyMatch(t -> {
      for (String str : forbiddenThemeNames) {
        String name = str.split(" - ")[1];
        if (t.name.equals(name)) {
          return true;
        }
      }

      return false;
    });

    boolean hasAnyForbiddenRaceTheme = n.races.get(0).themefilters.stream().anyMatch(t -> {
      for (String str : forbiddenThemeNames) {
        String name = str.split(" - ")[1];
        if (t.name.equals(name)) {
          return true;
        }
      }

      return false;
    });

    return !hasAnyForbiddenNationTheme && !hasAnyForbiddenRaceTheme;
  }

  @Override
  public RestrictionType getType() {
    return RestrictionType.NoNationTheme;
  }

  @Override
  protected void textFieldUpdate() {
    rmodel.clear();
    for (String str : assets.themes.keySet()) {
      for (Theme t : assets.themes.get(str)) {
        String add = str + " - " + t.toString();
        if (
          add.contains(textfield.getText()) || textfield.getText().length() == 0
        ) {
          rmodel.addElement(add);
        }
      }
    }
  }
}
