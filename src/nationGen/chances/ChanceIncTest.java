package nationGen.chances;


import org.junit.Assert;
import org.junit.Test;


public class ChanceIncTest {
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseJoiners_missingBothArguments_throwException() {
		ChanceInc.parse("and *2");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseJoiners_missingFirstArgument_throwException() {
		ChanceInc.parse("and true *2");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseJoiners_missingSecondArgument_throwException() {
		ChanceInc.parse("true and *2");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseJoiners_consecutiveJoiners_throwException() {
		ChanceInc.parse("true and or true *2");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseConditionNames_unknownConditionName_throwException() {
		ChanceInc.parse("testingUnknownConditionName *2");
	}
	
	@Test
	public void testConditionAnd() {
		Assert.assertFalse(ChanceInc.parse("false and false *2").condition.test(new ChanceIncData()));
		Assert.assertFalse(ChanceInc.parse("false and true *2").condition.test(new ChanceIncData()));
		Assert.assertFalse(ChanceInc.parse("true and false *2").condition.test(new ChanceIncData()));
		Assert.assertTrue(ChanceInc.parse("true and true *2").condition.test(new ChanceIncData()));
	}
	
	@Test
	public void testConditionOr() {
		Assert.assertFalse(ChanceInc.parse("false or false *2").condition.test(new ChanceIncData()));
		Assert.assertTrue(ChanceInc.parse("false or true *2").condition.test(new ChanceIncData()));
		Assert.assertTrue(ChanceInc.parse("true or false *2").condition.test(new ChanceIncData()));
		Assert.assertTrue(ChanceInc.parse("true or true *2").condition.test(new ChanceIncData()));
	}
	
	@Test
	public void testParentheses() {
		Assert.assertTrue(ChanceInc.parse("false and false or true *2").condition.test(new ChanceIncData()));
		Assert.assertFalse(ChanceInc.parse("false and (false or true) *2").condition.test(new ChanceIncData()));
	}
	
	@Test
	public void testDoubleParentheses() {
		Assert.assertTrue(ChanceInc.parse("false and false and false or true *2").condition.test(new ChanceIncData()));
		Assert.assertFalse(ChanceInc.parse("false and (false and (false or true)) *2").condition.test(new ChanceIncData()));
	}
	
	@Test
	public void testModAdd() {
		Assert.assertEquals(ChanceInc.parse("true +3").modificationAmount.applyModTo(4), 7, 0.0001);
		Assert.assertEquals(ChanceInc.parse("true 3").modificationAmount.applyModTo(4), 7, 0.0001);
	}
	
	@Test
	public void testModSubtract() {
		Assert.assertEquals(ChanceInc.parse("true -5").modificationAmount.applyModTo(8.2), 3.2, 0.0001);
	}
	
	@Test
	public void testModMultiply() {
		Assert.assertEquals(ChanceInc.parse("true *1.5").modificationAmount.applyModTo(4), 6, 0.0001);
		Assert.assertEquals(ChanceInc.parse("true *0.2").modificationAmount.applyModTo(10.6), 2.12, 0.0001);
	}
	
	@Test
	public void testModDivide() {
		Assert.assertEquals(ChanceInc.parse("true /2").modificationAmount.applyModTo(6), 3, 0.0001);
		Assert.assertEquals(ChanceInc.parse("true /0.4").modificationAmount.applyModTo(2), 5, 0.0001);
	}
}
