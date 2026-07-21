package nationGen.misc;

public enum CommandType {
    CUSTOMMAGIC("#custommagic", true),
    MAGICBOOST("#magicboost", true),
    MAGICSKILL("#magicskill", true),
    RPCOST("#rpcost", false),
    WEAPON("#weapon", true);

    private final String raw;
    public final boolean canMultipleExist;

    CommandType(String raw, boolean canMultipleExist) {
        this.raw = raw;
        this.canMultipleExist = canMultipleExist;
    }

    public String toString() {
        return this.raw;
    }

    public static CommandType fromRaw(String raw) {
        for (CommandType type : values()) {
            if (type.raw.equals(raw)) {
                return type;
            }
        }

        return null;
    }
}
