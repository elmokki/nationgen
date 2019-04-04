package nationGen.misc;


/**
 * An operator that denotes a method of modifying a value.  Used in chanceincs and themeincs, for example.
 */
public enum Operator {
	ADD,
	SUBTRACT,
	MULTIPLY,
	DIVIDE;
	
	public static Operator fromChar(char c) {
		switch (c) {
			case '+':
				return ADD;
			case '-':
				return SUBTRACT;
			case '*':
				return MULTIPLY;
			case '/':
				return DIVIDE;
			default:
				return null;
		}
	}
}
