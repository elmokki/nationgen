package nationGen.misc;


import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * Allows a collection of T tokens to be parsed sequentially by using required and optional matchers.
 * @param <T> The type of token being parsed
 */
public class Parser<T> {
	
	private Deque<T> tokens;
	
	public Parser(Collection<T> tokens) {
		this.tokens = new LinkedList<>(tokens);
	}
	
	public T next(String description) {
		try {
			return this.tokens.pop();
		} catch (NoSuchElementException e) {
			throw new ParseException("Expected " + description + " but reached end of tokens.", e);
		}
	}
	
	public T last(String description) {
		try {
			return this.tokens.removeLast();
		} catch (NoSuchElementException e) {
			throw new ParseException("Expected " + description + " but reached end of tokens.", e);
		}
	}
	
	public Optional<T> nextIf(Predicate<T> matcher) {
		T token = this.tokens.peek();
		if (token != null && matcher.test(token)) {
			return Optional.of(this.tokens.pop());
		}
		return Optional.empty();
	}
	
	public <O> Optional<O> nextIfConvertable(Function<T, Optional<O>> translator) {
		T token = this.tokens.peek();
		if (token != null) {
			Optional<O> translated = translator.apply(token);
			if (translated.isPresent()) {
				this.tokens.pop();
			}
			return translated;
		}
		return Optional.empty();
	}
	
	public List<T> remaining() {
		return List.copyOf(this.tokens);
	}
	
	public int size() {
		return this.tokens.size();
	}
	
	public boolean isEmpty() {
		return this.tokens.isEmpty();
	}
}
