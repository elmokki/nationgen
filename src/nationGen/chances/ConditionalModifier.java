package nationGen.chances;


import nationGen.misc.Arg;


/**
 * A class that holds a condition and a modification amount for the purposes of modifying something when the condition
 * is true, such as in a chanceinc or a themeinc.
 * @param <T> The type that is passed to the condition for evaluation
 */
public class ConditionalModifier<T> {
	
	public final Condition<T> condition;
	
	public final Arg modificationAmount;
	
	public String source;
	
	public ConditionalModifier(Condition<T> condition, Arg modificationAmount) {
		this.condition = condition;
		this.modificationAmount = modificationAmount;
	}
}
