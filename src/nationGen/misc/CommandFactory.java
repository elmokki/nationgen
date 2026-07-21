package nationGen.misc;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import nationGen.misc.commands.RpCostCommand;

public abstract class CommandFactory {
    public static Command create(CommandType type, String raw, Args args, String comment) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Command must have valid raw String input");
        }

        if (args == null) {
            args = new Args();
        }

        if (type == CommandType.RPCOST) {
            return new RpCostCommand(raw, args, comment);
        }
        
        else {
            return new Command(raw, args, comment);
        }
    }

    public static Command copy(Command commandToClone) {
        Optional<CommandType> type = commandToClone.getType();
        
        if (type.isPresent() && type.get() == CommandType.RPCOST) {
            return new RpCostCommand(commandToClone);
        }
        
        else {
            return new Command(commandToClone);
        }
    }

    public static Command create(String raw, Args args, String comment) {
        CommandType type = CommandType.fromRaw(raw);
        return CommandFactory.create(type, raw, args, comment);
    }

    public static Command create(CommandType type, String raw, String... args) {
        return CommandFactory.create(
            type,
            raw,
            Arrays.stream(args).map(Arg::new).collect(Collectors.toCollection(Args::new)),
            null
        );
    }

    public static Command create(String raw, String... args) {
        CommandType type = CommandType.fromRaw(raw);
        return CommandFactory.create(type, raw, args);
    }

    public static Command create(CommandType type, String raw, Arg... args) {
        return CommandFactory.create(type, raw, Args.of(args), null);
    }

    public static Command create(String raw, Arg... args) {
        CommandType type = CommandType.fromRaw(raw);
        return CommandFactory.create(type, raw, args);
    }

    public static Command create(CommandType type, String raw, Args args) {
        return CommandFactory.create(type, raw, args, null);
    }

    public static Command create(String raw, Args args) {
        CommandType type = CommandType.fromRaw(raw);
        return CommandFactory.create(type, raw, args, null);
    }

    public static Command create(CommandType type, String raw) {
        return CommandFactory.create(type, raw, null, null);
    }

    public static Command create(String raw) {
        CommandType type = CommandType.fromRaw(raw);
        return CommandFactory.create(type, raw, null, null);
    }

    public static Command parse(String raw) {
        ArgParser allArgs = Args.parse(raw);

        if (allArgs.isEmpty()) {
            throw new IllegalArgumentException("Command line is empty!");
        }

        String commandName = allArgs.nextString();
        CommandType type = CommandType.fromRaw(commandName);
        Args args = new Args();
        String comment = null;

        while (!allArgs.isEmpty()) {
            Arg arg = allArgs.next("argument");

            if (arg.get().startsWith("--")) {
                comment = arg.get().replaceAll("^-*", "") +
                    allArgs
                    .remaining()
                    .stream()
                    .map(Arg::get)
                    .collect(Collectors.joining(" "));

                break;
            } else {
                args.add(arg);
            }
        }

        return CommandFactory.create(type, commandName, args, comment);
    }
}
