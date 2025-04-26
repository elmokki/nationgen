package nationGen.GUI;

import com.elmokki.Drawing;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Drawable;
import nationGen.entities.Flag;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.FileUtil;
import nationGen.units.Unit;

// TODO: second color? color on each slot?
// TODO: themes checkboxes / add all automatically
// TODO: attacking checkbox
public class SpriteGen extends JFrame {

  private static final long serialVersionUID = 1L;

  private JFileChooser chooser = new JFileChooser() {
    @Override
    public void approveSelection() {
      File f = getSelectedFile();
      if (f.exists() && getDialogType() == SAVE_DIALOG) {
        int result = JOptionPane.showConfirmDialog(
          this,
          "The file exists, overwrite?",
          "Existing file",
          JOptionPane.YES_NO_OPTION
        );
        switch (result) {
          case JOptionPane.NO_OPTION:
          case JOptionPane.CLOSED_OPTION:
            return;
          case JOptionPane.YES_OPTION:
          default:
            break;
        }
      }
      super.approveSelection();
    }
  };

  private static class ImagePanel extends JPanel {

    BufferedImage image;

    @Override
    public void paint(Graphics g) {
      if (image != null) {
        g.drawImage(image, 0, 0, null);
      }
    }
  }

  private class ImageSaverPanel extends JPanel {

    String fileType = ".tga";
    BufferedImage tga;
    BufferedImage png;
    ImagePanel display = new ImagePanel();
    JPanel controls;
    JButton button;

    ImageSaverPanel() {
      super(new TableLayout(2, 1, 8, 8, TableLayout.Alignment.NORTH));
      JPanel displayCenterer = new JPanel(
        new TableLayout(1, 1, 0, 0, TableLayout.Alignment.SOUTH)
      );
      displayCenterer.add(display);
      add(displayCenterer);

      controls = new JPanel(
        new TableLayout(2, 1, 8, 8, TableLayout.Alignment.NORTH)
      );
      JPanel radios = new JPanel(
        new TableLayout(1, 2, 8, 8, TableLayout.Alignment.CENTER)
      );
      JRadioButton tgaButton = new JRadioButton(".tga", true);
      JRadioButton pngButton = new JRadioButton(".png");
      ButtonGroup group = new ButtonGroup();
      group.add(tgaButton);
      group.add(pngButton);
      tgaButton.addActionListener(e -> drawType(".tga"));
      pngButton.addActionListener(e -> drawType(".png"));
      radios.add(tgaButton);
      radios.add(pngButton);
      controls.add(radios);
      button = new JButton("Save as...");
      button.addActionListener(a -> saveImage());
      controls.add(button);
    }

    private void drawType(String type) {
      fileType = type;
      display.image = ".tga".equals(type) ? tga : png;
      getParent().repaint();
    }

    public void setImage(BufferedImage image) {
      if (display.image == null && image != null) {
        add(controls);
      }
      if (display.image != null && image == null) {
        remove(controls);
      }
      tga = image;
      if (image == null) {
        png = null;
      } else if (tga.getTransparency() == Transparency.OPAQUE) {
        png = Drawing.convertToAlpha(tga);
      } else {
        png = image;
      }
      if (image != null) {
        Dimension d = new Dimension(image.getWidth(), image.getHeight());
        display.setSize(d);
        display.setPreferredSize(d);
      }
      invalidate();
      drawType(fileType);
    }

    public void saveImage() {
      if (
        chooser.getSelectedFile() == null ||
        !chooser.getSelectedFile().getName().endsWith(fileType)
      ) {
        chooser.setSelectedFile(new File("derp" + fileType));
      }
      int returnVal = chooser.showSaveDialog(SpriteGen.this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith("." + fileType)) {
          file = new File(file.getPath() + fileType);
        }

        if (".tga".equals(fileType)) {
          FileUtil.writeTGA(display.image, file);
        } else if (".png".equals(fileType)) {
          FileUtil.writePng(display.image, file);
        }
      }
    }
  }

  private NationGen nGen;
  private NationGenAssets assets;

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (
      ClassNotFoundException
      | InstantiationException
      | IllegalAccessException
      | UnsupportedLookAndFeelException e
    ) {
      e.printStackTrace();
    }
    SwingUtilities.invokeLater(() -> new SpriteGen().setVisible(true));
  }

  public SpriteGen() {
    nGen = new NationGen();
    assets = nGen.getAssets();
    assets.addThemePoses();

    setTitle("SpriteGen");

    initGUI();
    pack();

    setResizable(false);

    setToPreferredSize();

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
  }

  public void initGUI() {
    JTabbedPane tabs = new JTabbedPane();

    tabs.addTab("Unit", new UnitSpriteMixer().content);
    tabs.addTab("Flag", new FlagSpriteMixer().content);

    setContentPane(tabs);
  }

  private void setToPreferredSize() {
    setSize(getPreferredSize());
  }

  private static class DrawableComboBoxRenderer extends BasicComboBoxRenderer {

    @Override
    public Component getListCellRendererComponent(
      JList list,
      Object value,
      int index,
      boolean isSelected,
      boolean cellHasFocus
    ) {
      super.getListCellRendererComponent(
        list,
        value,
        index,
        isSelected,
        cellHasFocus
      );

      Drawable drawable = (Drawable) value;

      setText(drawable.toString());
      if (index == -1 || "".equals(drawable.sprite)) {
        setIcon(null);
      } else {
        BufferedImage image = FileUtil.readImage(drawable.sprite);
        setIcon(new ImageIcon(image));
      }

      return this;
    }
  }

  private class UnitSpriteMixer {
    private Unit unit = null;
    private boolean attacking = false;
    private Item dummy;
    private JColorChooser colorChooser = new JColorChooser(Color.WHITE);
    private ImageSaverPanel drawpanel = new ImageSaverPanel();
    private ImageSaverPanel drawPanel2x = new ImageSaverPanel();
    private JComboBox<Race> racecombo = new JComboBox<>();
    private JComboBox<Pose> posecombo = new JComboBox<>();
    private List<SlotControls> slots = new ArrayList<>();
    private JPanel itempanel = new JPanel();

    public final JPanel content = new JPanel(
      new TableLayout(1, 2, 8, 8, TableLayout.Alignment.NORTH_WEST)
    );

    private class SlotControls {

      final String slot;
      JLabel lbl = new JLabel();
      JSpinner offsetx = new JSpinner(new SpinnerNumberModel());
      JSpinner offsety = new JSpinner(new SpinnerNumberModel());
      JComboBox<Item> items = new JComboBox<>();

      public SlotControls(String slot, List<Item> itemsToAdd) {
        this.slot = slot;

        lbl.setText(slot);
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);

        items.addItem(dummy);
        for (Item i : itemsToAdd) {
          items.addItem(i);
        }
        items.setRenderer(new DrawableComboBoxRenderer());
        items.addItemListener(e -> {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            Item i = (Item) items.getSelectedItem();
            unit.setSlot(slot, i == dummy ? null : i);

            offsetx.setValue(i.getOffsetX());
            offsety.setValue(i.getOffsetY());

            syncItems();
          }
        });

        // fix preferred size because it's too dumb to factor in the image sizes
        Dimension d = items.getPreferredSize();
        int maxImageWidth = itemsToAdd
          .stream()
          .filter(item -> !"".equals(item.sprite))
          .mapToInt(item -> FileUtil.readImage(item.sprite).getWidth())
          .max()
          .orElse(0);
        items.setPreferredSize(
          new Dimension(d.width + maxImageWidth, d.height)
        );

        offsetx.setPreferredSize(
          new Dimension(50, offsetx.getPreferredSize().height)
        );
        offsetx.addChangeListener(e -> {
          if (unit.getSlot(slot) != null) {
            unit.getSlot(slot).setOffsetX((int) offsetx.getValue());
            drawUnit();
          }
        });
        offsety.setPreferredSize(
          new Dimension(50, offsety.getPreferredSize().height)
        );
        offsety.addChangeListener(e -> {
          if (unit.getSlot(slot) != null) {
            unit.getSlot(slot).setOffsetY((int) offsety.getValue());
            drawUnit();
          }
        });
      }

      private void syncItems() {
        for (SlotControls entry : slots) {
          Item item = unit.getSlot(entry.slot);
          entry.items.setSelectedItem(item == null ? dummy : item);
        }

        drawUnit();
      }
    }

    UnitSpriteMixer() {
      dummy = new Item(nGen);
      dummy.name = "nothing";

      content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

      // LEFT SIDE:
      // Colors
      colorChooser
        .getSelectionModel()
        .addChangeListener(a -> {
          unit.color = colorChooser.getColor();
          drawUnit();
        });
      colorChooser.setPreviewPanel(new JPanel()); // get rid of it and use a different one
      JPanel colorPreviewPanel = new JPanel();
      colorPreviewPanel.setBackground(colorChooser.getColor());
      colorPreviewPanel.setPreferredSize(new Dimension(32, 32));
      colorChooser
        .getSelectionModel()
        .addChangeListener(c ->
          colorPreviewPanel.setBackground(colorChooser.getColor())
        );

      // Renders
      JPanel renders = new JPanel(
        new TableLayout(1, 0, 8, 8, TableLayout.Alignment.NORTH)
      );
      renders.add(drawpanel);
      renders.add(drawPanel2x);

      JPanel leftPanel = new JPanel(
        new TableLayout(3, 1, 8, 8, TableLayout.Alignment.NORTH_WEST)
      );
      leftPanel.add(colorChooser);
      leftPanel.add(colorPreviewPanel);
      leftPanel.add(renders);

      // RIGHT SIDE:
      // race/pose
      JPanel raceposepanel = new JPanel(
        new TableLayout(1, 0, 8, 8, TableLayout.Alignment.NORTH_EAST)
      );

      // Race
      raceposepanel.add(new JLabel("Race:"));
      for (Race r : assets.races) {
        racecombo.addItem(r);
      }
      racecombo.addItemListener(e -> {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updatePoses();
        }
      });
      raceposepanel.add(racecombo);

      // Pose
      raceposepanel.add(new JLabel("Pose:"));
      Race r = (Race) racecombo.getSelectedItem();
      for (Pose p : r.poses) {
        posecombo.addItem(p);
      }
      posecombo.addItemListener(e -> {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updateItems();
        }
      });
      raceposepanel.add(posecombo);

      JCheckBox attackCheckbox = new JCheckBox("Attacking");
      attackCheckbox.addActionListener(a -> {
        attacking = attackCheckbox.isSelected();
        drawUnit();
      });
      raceposepanel.add(attackCheckbox);

      updateItems();

      JPanel rightPanel = new JPanel(
        new TableLayout(0, 1, 8, 16, TableLayout.Alignment.NORTH_WEST)
      );
      rightPanel.add(raceposepanel);
      rightPanel.add(itempanel);

      content.add(leftPanel);
      content.add(rightPanel);
    }

    public void updatePoses() {
      posecombo.removeAllItems();
      for (Pose p : ((Race) racecombo.getSelectedItem()).poses) posecombo.addItem(
        p
      );

      updateItems();
    }

    private void updateItems() {
      Pose p = (Pose) posecombo.getSelectedItem();
      if (p == null) {
        System.out.println("pose was null");
        return;
      }

      itempanel.removeAll();

      slots.clear();

      for (String str : p.getListOfSlots()) {
        slots.add(new SlotControls(str, p.getItems(str)));
      }

      itempanel.setLayout(
        new TableLayout(
          1 + p.getListOfSlots().size(),
          4,
          8,
          8,
          TableLayout.Alignment.NORTH_EAST
        )
      );

      itempanel.setName("ITEM PANEL");

      itempanel.add(new JLabel("Slot", SwingConstants.RIGHT));
      itempanel.add(new JLabel("Item", SwingConstants.CENTER));
      itempanel.add(new JLabel("X", SwingConstants.CENTER));
      itempanel.add(new JLabel("Y", SwingConstants.CENTER));

      for (SlotControls se : slots) {
        itempanel.add(se.lbl);
        itempanel.add(se.items);
        itempanel.add(se.offsetx);
        itempanel.add(se.offsety);
      }

      resetUnit();
    }

    public void resetUnit() {
      this.unit = new Unit(
        nGen,
        (Race) racecombo.getSelectedItem(),
        (Pose) posecombo.getSelectedItem()
      );
      this.unit.color = colorChooser.getColor();
      drawUnit();
    }

    public void drawUnit() {
      System.out.println("Drawing.");

      BufferedImage tga = unit.render(attacking ? -5 : 0);
      drawpanel.setImage(tga);
      drawPanel2x.setImage(Drawing.scale2x(tga));

      setToPreferredSize();
      content.revalidate();
    }
  }

  private class FlagSpriteMixer {

    class FlagPart {

      JRadioButton color = new JRadioButton();
      JButton applyColor = new JButton("Apply");
      JLabel label;
      JComboBox<Flag> choices = new JComboBox<>();

      FlagPart(String name, List<Flag> flags) {
        color.setBackground(Color.WHITE);
        color.addActionListener(e ->
          colorChooser.getSelectionModel().setSelectedColor(getColor())
        );
        colorButtonGroup.add(color);

        applyColor.addActionListener(e ->
          setColor(colorChooser.getSelectionModel().getSelectedColor())
        );

        label = new JLabel(name, SwingConstants.RIGHT);

        choices.addItem(dummy);
        flags.forEach(choices::addItem);

        choices.setRenderer(new DrawableComboBoxRenderer());
        choices.addItemListener(e -> {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            drawFlag();
          }
        });
        // fix preferred size because it's too dumb to factor in the image sizes
        Dimension d = choices.getPreferredSize();
        choices.setPreferredSize(new Dimension(d.width + 128, d.height));
      }

      boolean isSelected() {
        return this.color.isSelected();
      }

      Color getColor() {
        return this.color.getBackground();
      }

      void setColor(Color color) {
        this.color.setBackground(color);
        drawFlag();
      }

      Flag getSelection() {
        return (Flag) choices.getSelectedItem();
      }
    }

    private Flag dummy;
    List<FlagPart> parts = new ArrayList<>();

    public final JPanel content = new JPanel(
      new TableLayout(1, 2, 8, 8, TableLayout.Alignment.NORTH_WEST)
    );
    private JColorChooser colorChooser = new JColorChooser(Color.WHITE);
    private ImageSaverPanel drawpanel = new ImageSaverPanel();
    private ImageSaverPanel drawPanel2x = new ImageSaverPanel();
    private ButtonGroup colorButtonGroup = new ButtonGroup();

    FlagSpriteMixer() {
      dummy = new Flag(nGen);
      dummy.name = "nothing";

      content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

      // LEFT SIDE:
      // Colors
      colorChooser.getSelectionModel().addChangeListener(a -> updateColors());
      colorChooser.setPreviewPanel(new JPanel()); // get rid of it and use a different one

      // Renders
      JPanel renders = new JPanel(
        new TableLayout(1, 0, 8, 8, TableLayout.Alignment.NORTH)
      );
      renders.add(drawpanel);
      renders.add(drawPanel2x);

      JPanel leftPanel = new JPanel(
        new TableLayout(0, 1, 8, 8, TableLayout.Alignment.NORTH_WEST)
      );
      leftPanel.add(colorChooser);
      leftPanel.add(renders);

      JPanel partsTable = new JPanel(
        new TableLayout(5, 4, 8, 8, TableLayout.Alignment.NORTH_WEST)
      );
      partsTable.add(new JLabel(""));
      partsTable.add(new JLabel("Color", SwingConstants.CENTER));
      partsTable.add(new JLabel("Flag Part", SwingConstants.CENTER));
      partsTable.add(new JLabel("Image", SwingConstants.CENTER));

      parts.add(new FlagPart("Pole", assets.flagparts.get("poles")));
      parts.add(new FlagPart("Base", assets.flagparts.get("baseflags")));
      parts.add(new FlagPart("Border", assets.flagparts.get("borders")));
      parts.add(new FlagPart("Topper", assets.flagparts.get("topicons")));
      parts.forEach(part -> {
        partsTable.add(part.color);
        partsTable.add(part.applyColor);
        partsTable.add(part.label);
        partsTable.add(part.choices);
      });
      parts.get(1).color.setSelected(true);

      content.add(leftPanel);
      content.add(partsTable);
    }

    public void updateColors() {
      parts
        .stream()
        .filter(FlagPart::isSelected)
        .forEach(part -> part.setColor(colorChooser.getColor()));
    }

    public void drawFlag() {
      System.out.println("Drawing.");

      BufferedImage tga = new BufferedImage(
        128,
        128,
        BufferedImage.TYPE_INT_ARGB
      );
      Graphics g = tga.getGraphics();
      g.setColor(new Color(0, 0, 0, 0));
      g.fillRect(0, 0, tga.getHeight(), tga.getWidth());

      for (FlagPart flagPart : parts) {
        flagPart.getSelection().render(g, flagPart.getColor());
      }

      drawpanel.setImage(tga);
      drawPanel2x.setImage(Drawing.scale2x(tga));

      setToPreferredSize();
      content.revalidate();
    }
  }
}
