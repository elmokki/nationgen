package nationGen.rostergeneration.statupgradepatterns;

public enum StatUpgradePatternEnum {
    LowerFirstStatUpgradePattern(1),
    BalancedStatUpgradePattern(2),
    RandomStatUpgradePattern(3),
    AnyStatUpgradePattern(4);

    private final int numVal;

    StatUpgradePatternEnum(final int numVal) {
        this.numVal = numVal;
    }

    public int getNumVal() {
        return this.numVal;
    }

    public String toString() {
        return this.getClass().getName();
    }
}
