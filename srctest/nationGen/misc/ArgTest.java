package nationGen.misc;

import org.junit.Assert;
import org.junit.Test;

public class ArgTest {

  @Test
  public void testLex() {
    ArgParser parser = Args.parse(
      "one two \"three four\" five (six and 7) 8 'nine \"ten\"' \"'eleven twelve' 13\""
    );

    Assert.assertEquals(parser.nextString(), "one");
    Assert.assertEquals(parser.nextString(), "two");
    Assert.assertEquals(parser.nextString(), "three four");
    Arg five = parser.next("String");
    Assert.assertFalse(five.isParenthesis());
    Assert.assertFalse(five.isNumeric());
    Assert.assertEquals(five.get(), "five");

    Arg parenthesis = parser.next("parenthesis");
    Assert.assertTrue(parenthesis.isParenthesis());
    Assert.assertEquals(parenthesis.getParenthesis().get(0).get(), "six");
    Assert.assertEquals(parenthesis.getParenthesis().get(1).get(), "and");
    Assert.assertEquals(parenthesis.getParenthesis().get(2).getInt(), 7);

    Assert.assertEquals(parser.nextInt(), 8);
    Assert.assertEquals(parser.nextString(), "nine \"ten\"");
    Assert.assertEquals(parser.nextString(), "'eleven twelve' 13");
  }

  @Test
  public void testNestedQuotes() {
    // the number of backslashes needed for each layer grows exponentially

    // zero 'one "two \'three \\"four \\\\\'five\\\\\' four\\" three\' two" one' zero
    ArgParser zero = Args.parse(
      "zero 'one \"two \\'three \\\\\"four \\\\\\\\\\'five\\\\\\\\\\' four\\\\\" three\\' two\" one' zero"
    );
    Assert.assertEquals(zero.nextString(), "zero");

    // one "two 'three \"four \\'five\\' four\" three' two" one
    ArgParser one = Args.parse(zero.nextString());
    Assert.assertEquals(one.nextString(), "one");

    // two 'three "four \'five\' four" three' two
    ArgParser two = Args.parse(one.nextString());
    Assert.assertEquals(two.nextString(), "two");

    // three "four 'five' four" three
    ArgParser three = Args.parse(two.nextString());
    Assert.assertEquals(three.nextString(), "three");

    // four 'five' four
    ArgParser four = Args.parse(three.nextString());
    Assert.assertEquals(four.nextString(), "four");

    // five
    ArgParser five = Args.parse(four.nextString());
    Assert.assertEquals(five.nextString(), "five");
    Assert.assertEquals(four.nextString(), "four");
    Assert.assertEquals(three.nextString(), "three");
    Assert.assertEquals(two.nextString(), "two");
    Assert.assertEquals(one.nextString(), "one");
    Assert.assertEquals(zero.nextString(), "zero");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testQuotes_mismatch_throwException() {
    Args.lex("'hello");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParentheses_missingEnd_throwException() {
    Args.lex("(hello");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParentheses_missingStart_throwException() {
    Args.lex("hello)");
  }

  @Test
  public void testParentheses() {
    Args args = Args.lex("() (()) (()())");
    Assert.assertEquals(args.size(), 3);

    ArgParser parser = args.parse();
    Assert.assertTrue(parser.next("parenthesis").getParenthesis().isEmpty());
    Assert.assertTrue(
      parser.next("parenthesis").getParenthesis().get(0).isParenthesis()
    );
    Args innerArgs = parser.next("parenthesis").getParenthesis();

    Assert.assertEquals(innerArgs.size(), 2);
    Assert.assertTrue(innerArgs.get(0).isParenthesis());
    Assert.assertTrue(innerArgs.get(1).isParenthesis());
  }
}
