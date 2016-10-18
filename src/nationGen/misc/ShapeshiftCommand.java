package nationGen.misc;

import java.util.List;

// The purpose of this subclass is to store the original name of the second form of a shapeshifter,
// e.g. "wyvern" for a unit that turns into a wyvern.
// This is useful since the args of the command changes to an id later, but we might still want to
// compare two Commands that shapeshift a unit to a wyvern, even if they have different id's
public class ShapeshiftCommand extends Command {
	public String shapeName = "";
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// can't use args in hashcode since it's not in equals()
		//result = prime * result + ((args == null) ? 0 : args.hashCode());
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result + ((shapeName == null) ? 0 : shapeName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ShapeshiftCommand other = (ShapeshiftCommand) obj;
		// args don't have to match, but command does
		/*if (args == null) {
			if (other.args != null)
				return false;
		} else if (!args.equals(other.args))
			return false;*/
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		if (shapeName == null) {
			if (other.shapeName != null)
				return false;
		} else if (!shapeName.equals(other.shapeName))
			return false;
		return true;
	}

	public ShapeshiftCommand(Command c) {
		super(c.command, c.args);
		shapeName = args.get(0);
	}
}