package nationGen.restrictions;

import com.elmokki.Generic;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathLevel;
import nationGen.nation.Nation;

public class MageWithAccessRestriction
  extends TwoListRestrictionWithComboBox<String, String> {

  private List<String> neededPaths = new ArrayList<>();

  public MageWithAccessRestriction() {
    super(
      "<html>Nation needs have 1 in 4 (100% random for 4 paths or better) access to at least one of the paths listed in the right box on a single mage</html>",
      "Magic: Mage with access"
    );
    this.extraTextField = true;
    this.textfieldDefaultText = "1";
    this.textFieldLabel = "Minimum magic level for added paths";

    this.comboboxlabel = "25% probability randoms allowed";
    this.comboboxoptions = new String[] { "True", "False" };

    for (MagicPath path : MagicPath.NON_HOLY) rmodel.addElement(path.name);
  }

  @Override
  public NationRestriction getRestriction() {
    MageWithAccessRestriction res = new MageWithAccessRestriction();

    for (int i = 0; i < chosen.getModel().getSize(); i++) res.neededPaths.add(
      chosen.getModel().getElementAt(i)
    );

    res.comboselection = this.comboselection;

    return res;
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    // Add button
    if (arg0.getSource().equals(addButton) && all.getSelectedIndex() > -1) {
      if (rmodel.getElementAt(all.getSelectedIndex()) != null) {
        if (!rmodel2.contains(rmodel.getElementAt(all.getSelectedIndex()))) {
          if (textfield == null) rmodel2.addElement(
            rmodel.getElementAt(all.getSelectedIndex())
          );
          else if (Generic.isNumeric(textfield.getText())) rmodel2.addElement(
            rmodel.getElementAt(all.getSelectedIndex()) +
            " " +
            textfield.getText()
          );
        }
      }
    }
    // remove button
    if (
      arg0.getSource().equals(removeButton) && chosen.getSelectedIndex() > -1
    ) {
      rmodel2.remove(chosen.getSelectedIndex());
    }
  }

  @Override
  public boolean doesThisPass(Nation n) {
    if (neededPaths.size() == 0) {
      System.out.println(
        "Magic: Mage with Access nation restriction has no paths set!"
      );
      return true;
    }

    List<MagicPathLevel> pathLevels = neededPaths
      .stream()
      .map(Generic::parseArgs)
      .map(args ->
        new MagicPathLevel(
          MagicPath.fromName(args.get(0)),
          Integer.parseInt(args.get(1))
        )
      )
      .collect(Collectors.toList());

    boolean randoms = comboselection == null || comboselection.equals("True");
    return n
      .selectCommanders("mage")
      .map(u -> u.getMagicPicks(randoms))
      .anyMatch(paths ->
        pathLevels.stream().allMatch(p -> paths.get(p.path) >= p.level)
      );
  }

  @Override
  public RestrictionType getType() {
    return RestrictionType.MageWithAccess;
  }
}
