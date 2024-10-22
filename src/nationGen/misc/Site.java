package nationGen.misc;

import java.util.ArrayList;
import java.util.List;
import nationGen.entities.Filter;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.units.Unit;

public class Site {

  public MagicPathInts gemMap = new MagicPathInts();
  public List<Unit> troops = new ArrayList<>();
  public List<Unit> coms = new ArrayList<>();
  public List<Command> othercommands = new ArrayList<>();
  public List<Filter> appliedfilters = new ArrayList<>();

  public int id = -1;
  public String name = "UNNAMED";
  public int level = 0;

  public List<String> writeLines() {
    if (id == -1) {
      throw new IllegalStateException("Site ID is -1");
    }
    //		if(name.equals("UNNAMED"))
    //		{
    //			throw new IllegalStateException("Site name is 'UNNAMED'");
    //		}

    List<String> lines = new ArrayList<>();

    lines.add("#newsite " + id);
    lines.add("#level " + level);
    lines.add("#rarity 5");
    lines.add("#path " + getPath().number);
    lines.add("#name \"" + name + "\"");

    lines.addAll(writeFeatureLines());

    lines.add("#end");
    lines.add("");

    return lines;
  }

  private List<String> writeFeatureLines() {
    List<String> lines = new ArrayList<>();

    for (Unit u : coms) lines.add("#homecom " + u.id + " --- " + u.name);
    for (Unit u : troops) lines.add("#homemon " + u.id + " --- " + u.name);
    for (Command str : othercommands) lines.add(str.toModLine());
    for (Filter f : this.appliedfilters) for (Command str : f.commands) lines.add(
      str.toModLine()
    );

    for (MagicPath path : MagicPath.values()) {
      if (gemMap.get(path) > 0) lines.add(
        "#gems " + path.number + " " + gemMap.get(path)
      );
    }
    return lines;
  }

  public MagicPathInts getSitePathDistribution() {
    MagicPathInts paths = new MagicPathInts();

    for (Unit u : coms) {
      for (Args args : u.tags.getAllArgs("sitepath")) {
        MagicPath path = MagicPath.fromInt(args.get(0).getInt());
        int power = args.get(1).getInt();
        paths.add(path, power);
      }

      MagicPathInts unitpaths = u.getMagicPicks();
      for (MagicPath path : MagicPath.values()) {
        paths.add(path, unitpaths.get(path));
      }
    }

    for (Unit u : troops) {
      for (Args args : u.tags.getAllArgs("sitepath")) {
        MagicPath path = MagicPath.fromInt(args.get(0).getInt());
        int power = args.get(1).getInt();
        paths.add(path, power);
      }
    }

    for (Filter f : appliedfilters) {
      for (Args args : f.tags.getAllArgs("sitepath")) {
        MagicPath path = MagicPath.fromInt(args.get(0).getInt());
        int power = args.get(1).getInt();
        paths.add(path, power);
      }
    }

    for (MagicPath path : MagicPath.NON_HOLY) {
      if (gemMap.get(path) > 0) {
        paths.add(path, gemMap.get(path) * 2);
      }
    }

    return paths;
  }

  /**
   * Gets magic path for a site
   * @return
   */
  public MagicPath getPath() {
    MagicPathInts paths = getSitePathDistribution();

    int highestvalue = -1;
    MagicPath highest = null;

    for (MagicPath path : MagicPath.values()) {
      if (paths.get(path) > highestvalue) {
        highestvalue = paths.get(path);
        highest = path;
      }
    }
    MagicPath path = MagicPath.HOLY;
    if (highest != null) path = highest;

    // If there were no paths, it's a holy site.
    if (highestvalue == 0) path = MagicPath.HOLY;

    return path;
  }

  /**
   * Gets magic path for a site
   * @return
   */
  public MagicPath getSecondaryPath() {
    MagicPathInts paths = getSitePathDistribution();

    int highestvalue = -1;
    MagicPath highest = null;

    for (MagicPath p : MagicPath.values()) {
      if (paths.get(p) > highestvalue) {
        highestvalue = paths.get(p);
        highest = p;
      }
    }
    MagicPath path = MagicPath.HOLY;
    if (highest != null && paths.get(highest) != 0) path = highest;

    paths.set(highest, 0);

    highestvalue = -1;
    MagicPath secondhighest = null;

    for (MagicPath p : MagicPath.values()) {
      if (paths.get(p) > highestvalue) {
        highestvalue = paths.get(p);
        secondhighest = p;
      }
    }

    if (secondhighest != null && paths.get(secondhighest) != 0) path =
      secondhighest;

    return path;
  }
}
