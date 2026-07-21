package nationGen.items;

public enum DominionsItemSlot {
    NO_SLOTS(1, 0, 1),
    HAND(2, 0, 6),
    BOW(512, 0, 1),
    HEAD(8192, 0, 1),
    BODY(65536, 0, 1),
    FEET(131072, 0, 1),
    MISC(262144, 0, 5),
    HEADS_CAN_ONLY_HAVE_CROWNS(16777216, 0, 1);

    public final int bitmask;
    public final int min;
    public final int max;

    DominionsItemSlot(int bitmask, int min, int max) {
        this.bitmask = bitmask;
        this.min = min;
        this.max = max;
    }

    public static DominionsItemSlot fromString(String slotName) {
        switch(slotName) {
            case "head":
                return DominionsItemSlot.HEAD;
            case "misc":
                return DominionsItemSlot.MISC;
            case "body":
                return DominionsItemSlot.BODY;
            case "hand":
                return DominionsItemSlot.HAND;
            case "feet":
                return DominionsItemSlot.FEET;
            case "bow":
                return DominionsItemSlot.BOW;
            default:
                return DominionsItemSlot.NO_SLOTS;
        }
    }
}
