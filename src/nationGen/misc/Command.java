package nationGen.misc;


import com.elmokki.Generic;

import java.util.*;
import java.util.stream.Collectors;


public class Command {
	public final String command;
	public final Args args;
	public final String comment;
	
	public Command(String cmd, Arg... args) {
		this(cmd, Args.of(args), null);
	}
	
	public Command(String cmd, Args args) {
		this(cmd, args, null);
	}
	
	public Command(String cmd, Args args, String comment) {
		if (Generic.containsSpace(cmd)) {
			throw new IllegalArgumentException("Command name can't contain a space.");
		}
		this.command = cmd;
		this.args = args;
		this.comment = comment;
	}
	
	/**
	 * Static "constructor" method which turns String arguments into Args for convenience.
	 * @param command The command name
	 * @param args The args as strings
	 * @return The new Command
	 */
	public static Command args(String command, String... args) {
		return new Command(command, Arrays.stream(args).map(Arg::new).collect(Collectors.toCollection(Args::new)));
	}
	
	/**
	 * Returns a copy of this command.  Since Args is a mutable list, we have to take care in sharing commands that may
	 * have their args modified.
	 * @return A copy of this command.
	 */
	public Command copy() {
		return new Command(this.command, this.args.copy(), comment);
	}
	
	public static Command parse(String line) {
		ArgParser allArgs = Args.parse(line);
		if (allArgs.isEmpty()) {
			throw new IllegalArgumentException("Command line is empty!");
		}
		String commandName = allArgs.nextString();
		Args args = new Args();
		String comment = null;
		while (!allArgs.isEmpty()) {
			Arg arg = allArgs.next("argument");
			if (arg.get().startsWith("--")) {
				comment = arg.get().replaceAll("^-*", "")
						+ allArgs.remaining().stream().map(Arg::get).collect(Collectors.joining(" "));
				break;
			} else {
				args.add(arg);
			}
		}
		return new Command(commandName, args, comment);
	}
	
	/**
	 * Writes a String for how Dominions expects a mod command line to look like, with strings in quotes and comments
	 * following dashes.
	 * @return A string suitable to be written to a Dominions mod file.
	 */
	public String toModLine() {
		return this.command + (this.args.isEmpty() ? "" : " ")
			+ this.args.stream()
				.map(a -> a.isNumeric() ? a.get() : Generic.quote(a.get().replaceAll("\"", "''"), '"'))
				.collect(Collectors.joining(" "))
			+ (this.comment != null ? (" --- " + this.comment) : "");
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Command command1 = (Command) o;
		return command.equals(command1.command) &&
				args.equals(command1.args);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(command, args);
	}
	
	public String toString() {
		return this.command + (this.args.isEmpty() ? "" : " ") + this.args;
	}
}
