package nationGen.units;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import nationGen.items.DominionsItemSlot;

/** Utility static class to encode/decode Dominions' Item Slots bitmask */
public abstract class DominionsItemSlots {
    public static int getHandSlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, SlotBitmask.NO_SLOTS).get(DominionsItemSlot.HAND);
    }

    public static int getBowSlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, SlotBitmask.SIX_HANDS).get(DominionsItemSlot.BOW);
    }

    public static int getHeadSlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, SlotBitmask.ONE_BOW).get(DominionsItemSlot.HEAD);
    }

    public static int getCrownSlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, SlotBitmask.ONE_BOW).get(DominionsItemSlot.CROWN);
    }

    public static int getBodySlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, SlotBitmask.TWO_HEADS).get(DominionsItemSlot.BODY);
    }

    public static int getFeetSlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, SlotBitmask.ONE_BODY).get(DominionsItemSlot.FEET);
    }

    public static int getMiscSlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, SlotBitmask.ONE_FEET).get(DominionsItemSlot.MISC);
    }

    public static boolean onlyCrownsAllowed(int bitmask) {
        return bitmask - SlotBitmask.HEADS_CAN_ONLY_HAVE_CROWNS.value >= 0;
    }

    public static int encode(HashMap<DominionsItemSlot, Integer> slots) {
        int itemslots = 0;
        Iterator<Entry<DominionsItemSlot, Integer>> it = slots.entrySet().iterator();

        while (it.hasNext()) {
            Entry<DominionsItemSlot, Integer> entry = it.next();
            DominionsItemSlot slot = entry.getKey();
            int amountOfSlots = entry.getValue();
            SlotBitmask slotBitmask = SlotBitmask.fromItemSlot(slot, amountOfSlots);
            itemslots += slotBitmask.value;
        }

        return itemslots;
    }

    public static HashMap<DominionsItemSlot, Integer> decode(int itemslots) {
        return DominionsItemSlots.decode(itemslots, null);
    }

    public static HashMap<DominionsItemSlot, Integer> decode(int itemslots, SlotBitmask stopAt) {
        List<SlotBitmask> maskValues = Arrays.asList(SlotBitmask.values()).reversed();
        HashMap<DominionsItemSlot, Integer> slots = new HashMap<>();

        slots.put(DominionsItemSlot.HAND, 0);
        slots.put(DominionsItemSlot.BOW, 0);
        slots.put(DominionsItemSlot.HEAD, 0);
        slots.put(DominionsItemSlot.BODY, 0);
        slots.put(DominionsItemSlot.FEET, 0);
        slots.put(DominionsItemSlot.MISC, 0);
        slots.put(DominionsItemSlot.NO_SLOTS, 0);

        boolean onlyCrowns = false;
        for (SlotBitmask slotBitmask : maskValues) {
            if (itemslots - slotBitmask.value < 0 || slotBitmask == stopAt) {
                break;
            }
            
            itemslots -= slotBitmask.value;

            if (slotBitmask == SlotBitmask.HEADS_CAN_ONLY_HAVE_CROWNS) {
                onlyCrowns = true;
            }

            else if (slotBitmask.slot == DominionsItemSlot.HEAD) {
                if (onlyCrowns) {
                    slots.remove(DominionsItemSlot.HEAD);
                    slots.put(DominionsItemSlot.CROWN, slotBitmask.amount);
                }

                else {
                    slots.put(DominionsItemSlot.HEAD, slotBitmask.amount);
                }
            }

            else {
                slots.put(slotBitmask.slot, slotBitmask.amount);
            }
        }

        return slots;
    }
    
    private enum SlotBitmask {
        NO_SLOTS(1, DominionsItemSlot.NO_SLOTS, 1),
        ONE_HAND(2, DominionsItemSlot.HAND, 1),
        TWO_HANDS(6, DominionsItemSlot.HAND, 2),
        THREE_HANDS(14, DominionsItemSlot.HAND, 3),
        FOUR_HANDS(30, DominionsItemSlot.HAND, 4),
        SIX_HANDS(126, DominionsItemSlot.HAND, 6),
        ONE_BOW(512, DominionsItemSlot.BOW, 1),
        ONE_HEAD(8912, DominionsItemSlot.HEAD, 1),
        TWO_HEADS(24576, DominionsItemSlot.HEAD, 2),
        ONE_BODY(65536, DominionsItemSlot.BODY, 1),
        ONE_FEET(131072, DominionsItemSlot.FEET, 1),
        ONE_MISC(262144, DominionsItemSlot.MISC, 1),
        TWO_MISCS(786432, DominionsItemSlot.MISC, 2),
        THREE_MISCS(1835008, DominionsItemSlot.MISC, 3),
        FOUR_MISCS(3932160, DominionsItemSlot.MISC, 4),
        HEADS_CAN_ONLY_HAVE_CROWNS(16777216, null, 0);

        public final int value;
        public final DominionsItemSlot slot;
        public final int amount;

        SlotBitmask(int value, DominionsItemSlot slot, int amount) {
            this.value = value;
            this.slot = slot;
            this.amount = amount;
        }

        static public SlotBitmask fromItemSlot(DominionsItemSlot slot, int amount) {
            return Arrays.asList(SlotBitmask.values())
                .stream()
                .filter(slotBitmask -> {
                    return slotBitmask.slot == slot && slotBitmask.amount == amount;
                })
                .findFirst()
                .orElse(SlotBitmask.NO_SLOTS);
        }
    }
}
