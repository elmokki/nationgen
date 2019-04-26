package nationGen.misc;


/**
 * An exception that can occur while parsing a string of text.
 */
public class ParseException extends RuntimeException {
	public ParseException(String message) {
		super(message);
	}
	
	public ParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
