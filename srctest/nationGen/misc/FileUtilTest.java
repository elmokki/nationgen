package nationGen.misc;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class FileUtilTest {

  @Test
  public void readImage() {
    BufferedImage image = FileUtil.readImage("/testResources/image.png");

    Assert.assertEquals(image.getRGB(0, 0), Color.BLACK.getRGB());
  }

  @Test(expected = IllegalArgumentException.class)
  public void readImage_textFile_throwException() {
    FileUtil.readImage("/testResources/text.txt");
  }

  @Test
  public void readLines() {
    List<String> lines = FileUtil.readLines("/testResources/text.txt");

    Assert.assertEquals(lines, List.of("one", "two", "three", "four"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void readLines_imageFile_throwException() {
    FileUtil.readLines("/testResources/image.png");
  }

  @Test(expected = IllegalStateException.class)
  public void createDirectory_alreadyExisting_throwException() {
    FileUtil.createDirectory("/testResources");
  }

  @Test
  public void isMissing_existingFile_false() {
    Assert.assertFalse(FileUtil.isMissing("/testResources/text.txt"));
  }

  @Test
  public void isMissing_missingFile_true() {
    Assert.assertTrue(
      FileUtil.isMissing("/testResources/do_not_create_this_file")
    );
  }
}
