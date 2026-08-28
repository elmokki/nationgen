package nationGen.units;

import java.util.stream.Stream;

import com.elmokki.Generic;

import nationGen.items.DominionsItemSlot;
import nationGen.misc.Command;

public enum DominionsBodyType {
    BIRD("#bird"),
    DJINN("#djinn"),
    HUMANOID("#humanoid"),
    LIZARD("#lizard"),
    MISC_SHAPE("#miscshape"),
    MOUNTED_HUMANOID("#mountedhumanoid"),
    NAGA("#naga"),
    QUADRUPED("#quadruped"),
    SNAKE("#snake"),
    TROGLODYTE("#troglodyte");

    private final String dominionsCommand;

    DominionsBodyType(String dominionsCommand) {
        this.dominionsCommand = dominionsCommand;
    }

    public String toModCommand() {
        return this.dominionsCommand;
    }

    public static DominionsBodyType fromCommand(Command command) {
        return DominionsBodyType.fromCommand(command.command);
    }

    public static DominionsBodyType fromCommand(String command) {
        return Stream.of(DominionsBodyType.values())
            .filter(prop -> {
                return prop.dominionsCommand.equals(command);
            })
            .findFirst()
            .orElse(null);
    }

    /**
     * Decides on bodytype tags based on the itemslots of the unit. Note that some specific bodytypes like #lizard, #djinn or #bird
     * cannot easily be distinguish just based on itemslots. These should have the tags hardcoded into the relevant creatures.
     * @param slots - an integer describing the total bitmask of the unit's slots
     * @param isMounted - whether this is a mounted creature or not
     * @return
     */
    public static DominionsBodyType fromItemslots(int slots, boolean isMounted) {
        boolean hasFeet = Generic.containsBitmask(slots, DominionsItemSlot.FEET.bitmask);
        boolean hasArms = Generic.containsBitmask(slots, DominionsItemSlot.HAND.bitmask);
        boolean hasHead = Generic.containsBitmask(slots, DominionsItemSlot.HEAD.bitmask);
        DominionsBodyType result;

        // Has feet and an arm
        if (hasFeet && hasArms) {
            if (hasHead) {
                result = DominionsBodyType.HUMANOID;
            }

            else {
                result = DominionsBodyType.TROGLODYTE;
            }
        }

        // No feet, but arm
        else if (hasArms) {
            if (isMounted) {
                result = DominionsBodyType.MOUNTED_HUMANOID;
            }

            else {
                // This could also be a #djinn, but no way to distinguish
                result = DominionsBodyType.NAGA;
            }
        }

        // Feet, no arm
        else if (hasFeet) {
            // This could also be a #lizard, but no way to distinguish
            result = DominionsBodyType.QUADRUPED;
        }

        // No feet nor arm
        else {
            result = DominionsBodyType.MISC_SHAPE;
        }

        return result;
    }
}
