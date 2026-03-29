package nationGen.items;

public enum DominionsItemSlot {
    NO_SLOTS(1),
    HAND(2),
    BOW(512),
    HEAD(8192),
    BODY(65536),
    FEET(131072),
    MISC(262144),
    HEADS_CAN_ONLY_HAVE_CROWNS(16777216);

    public final int bitmask;

    DominionsItemSlot(int bitmask) {
        this.bitmask = bitmask;
    }
}
