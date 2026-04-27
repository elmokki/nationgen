package nationGen;

import com.elmokki.Generic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import nationGen.entities.Filter;
import nationGen.entities.Flag;
import nationGen.entities.MagicItem;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.items.CustomItem;
import nationGen.magic.MagicPattern;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.FileUtil;
import nationGen.misc.ResourceStorage;
import nationGen.naming.NamePart;
import nationGen.units.Mount;
import nationGen.units.ShapeShift;

/**
 * This class will mirror the constant elements in Nationgen.java but eventually, it'll replace it.
 * This will allow me to incrementally replace files and allow easier testing.
 * For simplicity of refactoring, they'll start public. But, eventually, better accessors will be made.
 * Ideally, NationGen would NOT be depended upon at all. That's the end goal.  But, one step at a time.
 * @author flash-fire
 *
 */
public class NationGenAssets {

  public ResourceStorage<MagicPattern> patterns = new ResourceStorage<>(
    MagicPattern.class
  );

  public ResourceStorage<Filter> templates = new ResourceStorage<>(
    Filter.class
  );
  public ResourceStorage<Filter> descriptions = new ResourceStorage<>(
    Filter.class
  );
  public ResourceStorage<Race> races = new ResourceStorage<>(Race.class);
  public ResourceStorage<ShapeShift> monsters = new ResourceStorage<>(
    ShapeShift.class
  );
  public ResourceStorage<Pose> poses = new ResourceStorage<>(Pose.class);
  public ResourceStorage<Filter> filters = new ResourceStorage<>(Filter.class);
  public ResourceStorage<Theme> themes = new ResourceStorage<>(Theme.class);
  public ResourceStorage<Filter> spells = new ResourceStorage<>(Filter.class);
  public ResourceStorage<Filter> customspells = new ResourceStorage<>(Filter.class);
  public ResourceStorage<NamePart> magenames = new ResourceStorage<>(
    NamePart.class
  );
  public ResourceStorage<NamePart> miscnames = new ResourceStorage<>(
    NamePart.class
  );
  public ResourceStorage<Filter> gods = new ResourceStorage<>(Filter.class);
  public ResourceStorage<Filter> miscdef = new ResourceStorage<>(Filter.class);
  public ResourceStorage<Flag> flagparts = new ResourceStorage<>(Flag.class);
  public ResourceStorage<CustomItem> customitems = new ResourceStorage<>(CustomItem.class);
  public ResourceStorage<MagicItem> magicitems = new ResourceStorage<>(MagicItem.class);

  public ResourceStorage<ShapeShift> secondshapes = new ResourceStorage<>(ShapeShift.class);
  public ResourceStorage<Mount> mounts = new ResourceStorage<>(Mount.class);

  private List<Command> commandsInheritedByMounts = new ArrayList<>();
  private List<Command> commandsInheritedBySecondShape = new ArrayList<>();
  private List<Command> racePoseCommandsInheritedBySecondShape = new ArrayList<>();

  public void load(NationGen gen) {
    patterns.load(gen, "/data/magic/magicpatterns.txt");
    templates.load(gen, "/data/templates/templates.txt");
    descriptions.load(gen, "/data/descriptions/descriptions.txt");
    monsters.load(gen, "/data/monsters/monsters.txt");
    poses.load(gen, "/data/poses/poses.txt");
    filters.load(gen, "/data/filters/filters.txt");
    themes.load(gen, "/data/themes/themes.txt");
    spells.load(gen, "/data/spells/vanilla/index.txt");
    customspells.load(gen, "/data/spells/custom/index.txt");
    magenames.load(gen, "/data/names/magenames/magenames.txt");
    miscnames.load(gen, "/data/names/naming.txt");
    gods.load(gen, "/data/gods/gods.txt");
    miscdef.load(gen, "/data/misc/miscdef.txt");
    flagparts.load(gen, "/data/flags/flagdef.txt");
    customitems.load(gen, "./data/items/custom/index.txt");
    magicitems.load(gen, "/data/items/magic/index.txt");
    races.load(gen, "./data/races/races.txt");
    secondshapes.load(gen, "/data/shapes/secondshapes.txt");
    mounts.load(gen, "/data/mounts/mounts.txt");

    loadCommandsCanBeInheritedByMounts("/data/mounts/mountinheritance.txt");
    loadSecondShapeInheritance("/data/shapes/secondshapeinheritance.txt");
    initializeAllFilters();
  }

  /**
   * Loads the list of commands that second shapes should inherit from the primary shape
   * @param filename
   * @return
   */
  private void loadSecondShapeInheritance(String filename) {
    for (String line : FileUtil.readLines(filename)) {
      if (FileUtil.isLineComment(line) || line.isBlank()) {
        continue;
      }

      Command inheritableCommand = Command.parse(line);
      List<String> args = Generic.parseArgs(line);

      if (args.isEmpty()) {
        continue;
      }

      switch (args.get(0)) {
        case "all":
          commandsInheritedBySecondShape.add(inheritableCommand);
          break;
        case "racepose":
          racePoseCommandsInheritedBySecondShape.add(inheritableCommand);
          break;
      }
    }
  }

  private void loadCommandsCanBeInheritedByMounts(String filename) {
    for (String line : FileUtil.readLines(filename)) {
      if (FileUtil.isLineComment(line) || line.isBlank()) {
        continue;
      }

      Command inheritableCommand = Command.parse(line);
      commandsInheritedByMounts.add(inheritableCommand);
    }
  }

  public Boolean isCommandInheritableByMount(Command command) {
    return commandsInheritedByMounts.stream().anyMatch(c -> c.contains(command));
  }

  public Boolean isCommandInheritableByShape(Command command) {
    return commandsInheritedByMounts.stream().anyMatch(c -> c.contains(command));
  }

  public Boolean isRacePoseCommandInheritableByShape(Command command) {
    return racePoseCommandsInheritedBySecondShape.stream().anyMatch(c -> c.contains(command));
  }

  /**
   * Copies any poses from each race's theme's poses into its poses list
   */
  public void addThemePoses() {
    for (Race race : races.getAllValues()) {
      ChanceIncHandler.retrieveFilters(
        "racethemes",
        "default_racethemes",
        themes,
        null,
        race
      )
        .stream()
        .flatMap(theme -> theme.nationeffects.stream())
        .filter(command -> "#pose".equals(command.command))
        .map(command -> command.args.get(0).get())
        .distinct()
        .forEach(poseSetName -> {
          List<Pose> set = poses.get(poseSetName);
          if (set == null) {
            throw new IllegalArgumentException(
              "Pose set " + poseSetName + " was not found."
            );
          } else {
            race.poses.addAll(set);
          }
        });
    }
  }

  public void initializeFilters(List<Filter> newFilters, String filterSet) {
    List<Filter> newDescriptions = new ArrayList<>();

    for (String s : descriptions.keySet()) newDescriptions.addAll(
      descriptions.get(s)
    );

    for (Filter f : newFilters) {
      if (f.tags.containsName("filterdesc")) {
        f.description = newDescriptions
        .stream()
        .filter(pf -> {
          String filterDesc = f.tags.getString("filterdesc").orElse("");
          return pf.name.equals(filterDesc);
        })
        .findFirst()
        .orElseThrow(() ->
          new IllegalStateException(
            "Filter named " +
            f.name +
            " in " +
            filterSet +
            " can't find #filterdesc " +
            f.tags.getString("filterdesc").orElse("")
          )
        );
      }

      if (f.tags.containsName("prev")) {
        f.prevDesc = newDescriptions
          .stream()
          .filter(pf -> pf.descSet.equals(f.tags.getString("prev").orElse("")))
          .collect(Collectors.toList());
        if (f.prevDesc.isEmpty()) throw new IllegalStateException(
          "Filter named " +
          f.name +
          " in " +
          filterSet +
          " can't find #prev " +
          f.tags.getString("prev").orElse("")
        );
      }

      if (f.tags.containsName("next")) {
        f.nextDesc = newDescriptions
          .stream()
          .filter(nf -> nf.descSet.equals(f.tags.getString("next").orElse("")))
          .collect(Collectors.toList());

        if (f.nextDesc.isEmpty()) throw new IllegalStateException(
          "Filter named " +
          f.name +
          " in " +
          filterSet +
          " can't find #next " +
          f.tags.getString("next").orElse("")
        );
      }

      if (f.tags.containsName("bridge")) {
        f.bridgeDesc = newDescriptions
          .stream()
          .filter(bf -> bf.descSet.equals(f.tags.getString("bridge").orElse(""))
          )
          .collect(Collectors.toList());

        if (f.bridgeDesc.isEmpty()) throw new IllegalStateException(
          "Filter named " +
          f.name +
          " in " +
          filterSet +
          " can't find #bridge " +
          f.tags.getString("bridge").orElse("")
        );
      }
    }
  }

  public void initializeAllFilters() {
    for (String s : descriptions.keySet()) initializeFilters(
      descriptions.get(s),
      s
    );

    for (String s : filters.keySet()) initializeFilters(filters.get(s), s);

    for (String s : templates.keySet()) initializeFilters(templates.get(s), s);
  }

  public Mount getMount(String mountId) {
    Optional<Mount> mount = this.mounts
      .getAllValues()
      .stream()
      .filter(m -> m.name.equals(mountId))
      .findFirst();

    if (mount.isPresent() == false) {
      throw new IllegalArgumentException("Mount id " + mountId + " could not be found. Make sure it's spelled correctly in the data definitions.");
    }

    return new Mount(mount.get());
  }
}
