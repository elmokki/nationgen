package nationGen.misc;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TagsTest {

  private Tags tags;

  @Before
  public void setUpTags() {
    this.tags = new Tags();
    this.tags.add("string", "hi");
    this.tags.add("int", 1);
    this.tags.add("intAsString", "2");
    this.tags.add("double", 5.2);
    this.tags.addArgs("multiple", "hi", "hello");
  }

  @Test
  public void testContainsName() {
    String name = "tagWithNoArgs";

    Assert.assertFalse(this.tags.containsName(name));

    this.tags.addName(name);

    Assert.assertTrue(this.tags.containsName(name));
  }

  @Test
  public void testContainsCommand() {
    Command command = Command.args(
      "tagWithMultipleArgs",
      "one",
      "two",
      "three"
    );

    Assert.assertFalse(this.tags.containsName("tagWithMultipleArgs"));
    Assert.assertFalse(this.tags.containsTag(command));

    this.tags.addFromCommand(command);

    Assert.assertTrue(this.tags.containsName("tagWithMultipleArgs"));
    Assert.assertTrue(this.tags.containsTag(command));

    Optional<Args> args = this.tags.getArgs("tagWithMultipleArgs");

    Assert.assertTrue(args.isPresent());
    Assert.assertEquals(args.get().size(), 3);
    Assert.assertEquals(args.get().get(0).get(), "one");
    Assert.assertEquals(args.get().get(1).get(), "two");
    Assert.assertEquals(args.get().get(2).get(), "three");
  }

  @Test
  public void testGetValue_string() {
    Assert.assertTrue(this.tags.getString("string").isPresent());
    Assert.assertEquals(this.tags.getString("string").get(), "hi");
  }

  @Test
  public void testGetValue_int() {
    Assert.assertTrue(tags.getString("int").isPresent());
    Assert.assertEquals(tags.getString("int").get(), "1");
    Assert.assertTrue(tags.getInt("int").isPresent());
    Assert.assertEquals((long) tags.getInt("int").get(), 1);
  }

  @Test
  public void testGetValue_intAsString() {
    Assert.assertTrue(this.tags.getString("intAsString").isPresent());
    Assert.assertEquals(this.tags.getString("intAsString").get(), "2");
    Assert.assertTrue(this.tags.getInt("intAsString").isPresent());
    Assert.assertEquals((long) this.tags.getInt("intAsString").get(), 2);
  }

  @Test
  public void testGetValue_double() {
    Assert.assertTrue(this.tags.getString("double").isPresent());
    Assert.assertEquals(this.tags.getString("double").get(), "5.2");
    Assert.assertTrue(this.tags.getInt("double").isPresent());
    Assert.assertEquals((long) this.tags.getInt("double").get(), 5);
    Assert.assertTrue(this.tags.getDouble("double").isPresent());
    Assert.assertEquals(this.tags.getDouble("double").get(), 5.2, 0);
  }

  @Test
  public void testGetValue_multipleArgs() {
    Assert.assertTrue(this.tags.getArgs("multiple").isPresent());
    Assert.assertEquals(this.tags.getArgs("multiple").get().size(), 2);
    Assert.assertEquals(this.tags.getArgs("multiple").get().get(0).get(), "hi");
    Assert.assertEquals(
      this.tags.getArgs("multiple").get().get(1).get(),
      "hello"
    );
  }
}
