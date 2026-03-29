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

            while (bitmask < itemslots) {
                int nextBitmask = DominionsItemSlots.getBitmaskForAmountOfSlots(slot.bitmask, i);

                if (nextBitmask > itemslots) {
                    break;
                }

                else {
                    bitmask = nextBitmask;
                    i++;
                }
            }

            if (bitmask > itemslots || bitmask == 0) {
                continue;
            }

            itemslots -= bitmask;
            decodedSlots.put(slot, i-1);
        }

        return decodedSlots;
    }

    public static HashMap<DominionsItemSlot, Integer> decode(int itemslots) {
        return DominionsItemSlots.decode(itemslots, null);
    }

    private static int getBitmaskForAmountOfSlots(int slotBitmask, int amountOfSlots) {
        int bitmask = slotBitmask * ((int)Math.pow(2, amountOfSlots) - 1);
        return bitmask;
    }
}
