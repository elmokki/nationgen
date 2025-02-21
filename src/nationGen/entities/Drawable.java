package nationGen.entities;

import com.elmokki.Drawing;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import nationGen.NationGen;
import nationGen.misc.Command;
import nationGen.misc.FileUtil;

public class Drawable extends Filter {

  public String sprite = "";
  public String mask = "";
  protected int offsetx = 0;
  protected int offsety = 0;
  public String renderslot = "";
  public int renderprio = 5;

  public Drawable(NationGen nationGen) {
    super(nationGen);
  }

  @Override
  public void handleOwnCommand(Command str) {
    try {
      switch (str.command) {
        case "#sprite":
          this.sprite = str.args.get(0).get();
          break;
        case "#renderslot":
          this.renderslot = str.args.get(0).get();
          break;
        case "#renderprio":
          this.renderprio = str.args.get(0).getInt();
          break;
        case "#recolormask":
        case "#mask":
          this.mask = str.args.get(0).get();
          break;
        case "#offsetx":
          this.offsetx = str.args.get(0).getInt();
          break;
        case "#offsety":
          this.offsety = str.args.get(0).getInt();
          break;
        default:
          super.handleOwnCommand(str);
      }
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException(
        "WARNING: " + str + " has insufficient arguments (" + this.name + ")",
        e
      );
    }
  }

  public int getOffsetX() {
    return offsetx;
  }

  public void setOffsetX(int x) {
    this.offsetx = x;
  }

  public int getOffsetY() {
    return offsety;
  }

  public void setOffsetY(int y) {
    this.offsety = y;
  }

  public void render(Graphics g, Color c) {
    render(g, false, 0, 0, c);
  }

  public void render(
    Graphics g,
    boolean useoffsets,
    int offsetx,
    int offsety,
    Color color
  ) {
    Drawable i = this;
    if (i.sprite == null || i.sprite.equals("")) return;

    int xoff = i.offsetx + offsetx;
    int yoff = i.offsety + offsety;
    if (!useoffsets) {
      xoff = 0;
      yoff = 0;
    }

    BufferedImage image = FileUtil.readImage(i.sprite);

    // Handle "black_to_alpha"
    if (this.tags.containsName("convert_to_alpha")) image =
      Drawing.convertToAlpha(image);

    g.drawImage(image, xoff, yoff, null);
    drawRecolorMask(g, this, color, xoff, yoff);
  }

  private void drawRecolorMask(Graphics g, Drawable i, Color c, int x, int y) {
    if (!i.mask.equals("")) {
      if (i.mask.equals("self")) i.mask = i.sprite;

      BufferedImage image = FileUtil.readImage(i.mask);
      BufferedImageOp colorizeFilter;

      // Handle "black_to_alpha"
      if (this.tags.containsName("convert_to_alpha")) image =
        Drawing.convertToAlpha(image);

      if (i.tags.containsName("alternaterecolor")) colorizeFilter =
        Drawing.createColorizeOp_alt(c);
      else colorizeFilter = Drawing.createColorizeOp(c);

      BufferedImage targetImage = colorizeFilter.filter(image, image);

      g.drawImage(targetImage, x, y, null);
    }
  }
}
