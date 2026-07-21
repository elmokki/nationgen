package nationGen.units;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import nationGen.items.DominionsItemSlot;

/** 
 * Utility abstract class to encode/decode Dominions' #itemslots bitmask. Can encode a HashMap of
 * DominionsItemSlot enum keys and integer values (i.e. { HAND: 2, BOW: 1, HEAD: 1, ... }) into a
 * valid bitmask as described in the #itemslots entry of the Dominions manual, as well as decode
 * such a bitmask into resulting unit slots.
 */
public abstract class DominionsItemSlots {
    public static final int MIN_HANDS = 0;
    public static final int MIN_BOWS = 0;
    public static final int MIN_HEADS = 0;
    public static final int MIN_BODIES = 0;
    public static final int MIN_FEET = 0;
    public static final int MIN_MISC = 0;

    public static final int MAX_HANDS = 6;
    public static final int MAX_BOWS = 1;
    public static final int MAX_HEADS = 2;
    public static final int MAX_BODIES = 1;
    public static final int MAX_FEET = 1;
    public static final int MAX_MISC = 5;

    public static HashMap<DominionsItemSlot, Integer> defaultSlots() {
        HashMap<DominionsItemSlot, Integer> itemslots = new HashMap<>();

        itemslots.put(DominionsItemSlot.HAND, 2);
        itemslots.put(DominionsItemSlot.BOW, 1);
        itemslots.put(DominionsItemSlot.HEAD, 1);
        itemslots.put(DominionsItemSlot.BODY, 1);
        itemslots.put(DominionsItemSlot.FEET, 1);
        itemslots.put(DominionsItemSlot.MISC, 2);
        itemslots.put(DominionsItemSlot.NO_SLOTS, 0);

        return itemslots;
    }

    public static int getHandSlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, DominionsItemSlot.NO_SLOTS).get(DominionsItemSlot.HAND);
    }

    public static int getBowSlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, DominionsItemSlot.HAND).get(DominionsItemSlot.BOW);
    }

    public static int getHeadSlots(int bitmask) {
        if (DominionsItemSlots.onlyCrownsAllowed(bitmask)) {
            return 0;
        }

        return DominionsItemSlots.decode(bitmask, DominionsItemSlot.BOW).get(DominionsItemSlot.HEAD);
    }

    public static int getCrownSlots(int bitmask) {
        if (!DominionsItemSlots.onlyCrownsAllowed(bitmask)) {
            return 0;
        }

        return DominionsItemSlots.decode(bitmask, DominionsItemSlot.BOW).get(DominionsItemSlot.HEAD);
    }

    public static int getBodySlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, DominionsItemSlot.HEAD).get(DominionsItemSlot.BODY);
    }

    public static int getFeetSlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, DominionsItemSlot.BODY).get(DominionsItemSlot.FEET);
    }

    public static int getMiscSlots(int bitmask) {
        return DominionsItemSlots.decode(bitmask, DominionsItemSlot.FEET).get(DominionsItemSlot.MISC);
    }

    public static boolean onlyCrownsAllowed(int bitmask) {
        return bitmask - DominionsItemSlot.HEADS_CAN_ONLY_HAVE_CROWNS.bitmask >= 0;
    }

    public static int encode(HashMap<DominionsItemSlot, Integer> slots) {
        int itemslots = 0;
        Iterator<Entry<DominionsItemSlot, Integer>> it = slots.entrySet().iterator();

        while (it.hasNext()) {
            Entry<DominionsItemSlot, Integer> entry = it.next();
            DominionsItemSlot slot = entry.getKey();
            int amountOfSlots = entry.getValue();
            int bitmask = DominionsItemSlots.getBitmaskForAmountOfSlots(slot.bitmask, amountOfSlots);
            itemslots += bitmask;
        }

        // No slots were encoded, so return the corresponding bitmask value
        if (itemslots == 0) {
            return DominionsItemSlot.NO_SLOTS.bitmask;
        }

        return itemslots;
    }

    public static int encode(int hands, int bow, int head, int body, int feet, int misc, boolean onlyCrowns) {
        HashMap<DominionsItemSlot, Integer> slots = new HashMap<>();

        slots.put(DominionsItemSlot.HAND, hands);
        slots.put(DominionsItemSlot.BOW, bow);
        slots.put(DominionsItemSlot.HEAD, head);
        slots.put(DominionsItemSlot.BODY, body);
        slots.put(DominionsItemSlot.FEET, feet);
        slots.put(DominionsItemSlot.MISC, misc);

        if (onlyCrowns) {
            slots.put(DominionsItemSlot.HEADS_CAN_ONLY_HAVE_CROWNS, 1);
        }

        return DominionsItemSlots.encode(slots);
    }
    
    public static int encode(int hands, int bow, int head, int body, int feet, int misc) {
        return DominionsItemSlots.encode(hands, bow, head, body, feet, misc, false);
    }

    public static HashMap<DominionsItemSlot, Integer> decode(int itemslots, DominionsItemSlot stopAt) {
        List<DominionsItemSlot> slotsEnum = Arrays.asList(DominionsItemSlot.values()).reversed();
        HashMap<DominionsItemSlot, Integer> decodedSlots = new HashMap<>();

        decodedSlots.put(DominionsItemSlot.HAND, 0);
        decodedSlots.put(DominionsItemSlot.BOW, 0);
        decodedSlots.put(DominionsItemSlot.HEAD, 0);
        decodedSlots.put(DominionsItemSlot.BODY, 0);
        decodedSlots.put(DominionsItemSlot.FEET, 0);
        decodedSlots.put(DominionsItemSlot.MISC, 0);
        decodedSlots.put(DominionsItemSlot.NO_SLOTS, 0);

        for (DominionsItemSlot slot : slotsEnum) {
            if (slot == stopAt) {
                break;
            }
            
            int i = 1;
            int bitmask = 0;

            while (bitmask < Math.abs(itemslots)) {
                int nextBitmask = DominionsItemSlots.getBitmaskForAmountOfSlots(slot.bitmask, i);

                if (nextBitmask > Math.abs(itemslots)) {
                    break;
                }

                else {
                    bitmask = nextBitmask;
                    i++;
                }
            }

            if (bitmask > Math.abs(itemslots) || bitmask == 0) {
                continue;
            }

            int sign = (itemslots < 0) ? -1 : 1;
            itemslots -= sign * bitmask;
            decodedSlots.put(slot, sign * (i-1));
        }

        return decodedSlots;
    }

    public static HashMap<DominionsItemSlot, Integer> decode(int itemslots) {
        return DominionsItemSlots.decode(itemslots, null);
    }

    public static void capSlot(HashMap<DominionsItemSlot, Integer> itemslots, DominionsItemSlot slot, int min, int max) {
        int currentValue = itemslots.get(slot);
        int newValue = Math.min(Math.max(currentValue, min), max);
        itemslots.put(slot, newValue);
    }

    public static void enforceMin(HashMap<DominionsItemSlot, Integer> itemslots) {
        itemslots.forEach((slot, value) -> {
            if (value < slot.min) {
                /*throw new IllegalStateException(
                    "Unit has less than " +
                    slot.min +
                    " " +
                    slot.toString() +
                    " slots"
                );*/
                itemslots.put(slot, slot.min);
            }
        });
    }

    public static void enforceMax(HashMap<DominionsItemSlot, Integer> itemslots) {
        itemslots.forEach((slot, value) -> {
            if (value > slot.max) {
                /*throw new IllegalStateException(
                    "Unit has more than " +
                    slot.max +
                    " " +
                    slot.toString() +
                    " slots"
                );*/
                itemslots.put(slot, slot.max);
            }
        });
    }

    /**
     * Merges the values of multiple HashMap<DominionsItemSlot, Integer> by adding them together. 
     * @param slotMaps - the maps to merge by addition
     * @return - the resulting map
     */
    public static HashMap<DominionsItemSlot, Integer> add(List<HashMap<DominionsItemSlot, Integer>> slotMaps) {
        HashMap<DominionsItemSlot, Integer> combined = new HashMap<>();

        for (HashMap<DominionsItemSlot, Integer> map : slotMaps) {
            map.forEach((slot, value) -> {
                combined.merge(slot, value, (oldValue, newValue) -> {
                    return oldValue + newValue;
                });
            });
        }

        return combined;
    }

    private static int getBitmaskForAmountOfSlots(int slotBitmask, int amountOfSlots) {
        int bitmask = slotBitmask * ((int)Math.pow(2, amountOfSlots) - 1);
        return bitmask;
    }
}
