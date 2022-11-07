package de.maxhenkel.pipez.blocks.tileentity.types;

import de.maxhenkel.pipez.DirectionalPosition;
import de.maxhenkel.pipez.filters.Filter;
import de.maxhenkel.pipez.Upgrade;
import de.maxhenkel.pipez.blocks.tileentity.PipeLogicTileEntity;
import de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity;
import de.maxhenkel.pipez.blocks.tileentity.UpgradeTileEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public abstract class PipeType<T> {

    public abstract String getKey();
    public abstract Component getKeyText();

    public abstract void tick(PipeLogicTileEntity tileEntity);

    public abstract int getRate(@Nullable Upgrade upgrade);

    public abstract boolean canInsert(BlockEntity tileEntity, Direction direction);

    public abstract Filter<T> createFilter();

    public abstract String getTranslationKey();

    public abstract ItemStack getIcon();

    public abstract Component getTransferText(@Nullable Upgrade upgrade);

    public boolean hasFilter() {
        return true;
    }

    public UpgradeTileEntity.Distribution getDefaultDistribution() {
        return UpgradeTileEntity.Distribution.ROUND_ROBIN;
    }

    public UpgradeTileEntity.RedstoneMode getDefaultRedstoneMode() {
        return UpgradeTileEntity.RedstoneMode.IGNORED;
    }

    public UpgradeTileEntity.FilterMode getDefaultFilterMode() {
        return UpgradeTileEntity.FilterMode.WHITELIST;
    }

    public int getRate(PipeLogicTileEntity tileEntity, Direction direction) {
        return getRate(tileEntity.getUpgrade(direction));
    }

    public boolean matchesConnection(PipeTileEntity.Connection connection, Filter<T> filter) {
        if (filter.getDestination() == null) {
            //System.out.println("no destination set, skipping");
            return true;
        }
//        System.out.println(filter.getDestination().getPos() + " == " + connection.getPos());
//        System.out.println(filter.getDestination().getDirection().getName() + " == " + connection.getDirection().getName());
//        System.out.println(filter.getDestination().getDirection().getStepX() + " == " + connection.getDirection().getStepX());
//        System.out.println(filter.getDestination().getDirection().getStepY() + " == " + connection.getDirection().getStepY());
//        System.out.println(filter.getDestination().getDirection().getStepZ() + " == " + connection.getDirection().getStepZ());
        boolean destination = filter.getDestination().equals(new DirectionalPosition(connection.getPos(), connection.getDirection()));
//        System.out.println("matched destination: " + destination);
        return destination;
    }

    public boolean deepExactCompare(Tag meta, Tag item) {
        if (meta instanceof CompoundTag) {
            if (!(item instanceof CompoundTag)) {
                return false;
            }
            CompoundTag c = (CompoundTag) meta;
            CompoundTag i = (CompoundTag) item;
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(c.getAllKeys());
            allKeys.addAll(i.getAllKeys());
            for (String key : allKeys) {
                if (c.contains(key)) {
                    if (i.contains(key)) {
                        Tag nbt = c.get(key);
                        if (!deepExactCompare(nbt, i.get(key))) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        } else if (meta instanceof ListTag) {
            ListTag l = (ListTag) meta;
            if (!(item instanceof ListTag)) {
                return false;
            }
            ListTag il = (ListTag) item;
            if (!l.stream().allMatch(inbt -> il.stream().anyMatch(inbt1 -> deepExactCompare(inbt, inbt1)))) {
                return false;
            }
            if (!il.stream().allMatch(inbt -> l.stream().anyMatch(inbt1 -> deepExactCompare(inbt, inbt1)))) {
                return false;
            }
            return true;
        } else {
            return meta != null && meta.equals(item);
        }
    }


    boolean stringListFuncCompare(String comparisonString, ListTag tag) {

        char comparisonFunc = comparisonString.charAt(0);
        int comparisonVal = 0;
        String comparisonStringVal = comparisonString.substring(1);
        if(comparisonStringVal.length()>0 && !comparisonStringVal.matches("([0-9.]+)"))
            return false;
        if (comparisonStringVal.length() > 0) {
            comparisonVal = Integer.valueOf(comparisonStringVal).intValue();
        }
        switch (comparisonFunc) {
            case '>':
                return tag.size() > comparisonVal;
            case '=':
                return tag.size() == comparisonVal;
            case '<':
                return tag.size() < comparisonVal;
            case '~':
                return tag.size() != comparisonVal;
            case '*':
                return tag.size() > 0;
        }
        return false;
    }

    boolean stringNumericFuncCompare(String comparisonString, double tag) {
        char comparisonFunc = comparisonString.charAt(0);
        float comparisonVal = 0;
        String comparisonStringVal = comparisonString.substring(1);
        if(comparisonStringVal.length()>0 && !comparisonStringVal.matches("([0-9.]+)"))
            return false;
        if (comparisonStringVal.length() > 0) {
            comparisonVal = Float.valueOf(comparisonStringVal).floatValue();
        }
        switch (comparisonFunc) {
            case '>':
                return tag > comparisonVal;
            case '=':
                return tag == comparisonVal;
            case '<':
                return tag < comparisonVal;
            case '~':
                return tag != comparisonVal;
            case '*':
                return tag > 0;
        }
        return false;
    }

    public boolean deepFuzzyCompare(Tag filterTag, Tag stackTag) {
        if (filterTag instanceof CompoundTag) {
            CompoundTag filterCompound = (CompoundTag) filterTag;
            return filterCompound.getAllKeys().stream().allMatch(key -> {
                Tag filterMatchOnKey = filterCompound.get(key);
                switch (key) {
                    case "pze":
                        if (stackTag == null)
                            return false;
                        if (!(stackTag instanceof CompoundTag))
                            return false;
                        CompoundTag itemCompound = (CompoundTag) stackTag;
                        if (filterMatchOnKey instanceof ListTag) {
                            ListTag filterMatchOnKeyList = (ListTag) filterMatchOnKey;
                            return filterMatchOnKeyList.stream().allMatch(filterMatchOnKeyListItem -> {
                                if (filterMatchOnKeyListItem instanceof StringTag)
                                    return itemCompound.contains(filterMatchOnKeyListItem.getAsString());
                                return false;
                            });
                        } else if (filterMatchOnKey instanceof StringTag) {
                            StringTag filterMatchOnKeyString = (StringTag) filterMatchOnKey;
                            return itemCompound.contains(filterMatchOnKeyString.getAsString());
                        }
                        break;
                    case "pzne":
                        if (stackTag == null)
                            return true;
                        if (!(stackTag instanceof CompoundTag))
                            return false;
                        itemCompound = (CompoundTag) stackTag;
                        if (filterMatchOnKey instanceof ListTag) {
                            ListTag filterMatchOnKeyList = (ListTag) filterMatchOnKey;
                            return !filterMatchOnKeyList.stream().anyMatch(filterMatchOnKeyListItem -> {
                                if (filterMatchOnKeyListItem instanceof StringTag)
                                    return itemCompound.contains(filterMatchOnKeyListItem.getAsString());
                                return false;
                            });
                        } else if (filterMatchOnKey instanceof StringTag) {
                            StringTag filterMatchOnKeyString = (StringTag) filterMatchOnKey;
                            return !itemCompound.contains(filterMatchOnKeyString.getAsString());
                        }
                        break;
                    default:
                        if (!(stackTag instanceof CompoundTag))
                            return false;
                        itemCompound = (CompoundTag) stackTag;
                        if (itemCompound.contains(key)) {
                            return deepFuzzyCompare(filterMatchOnKey, itemCompound.get(key));
                        }
                }
                return false;
            });
        } else if (stackTag instanceof ListTag && filterTag instanceof NumericTag) {
            ListTag stackList = (ListTag) stackTag;
            NumericTag filterInt = (NumericTag) filterTag;
            return stackList.size() == filterInt.getAsInt();
        } else if (stackTag instanceof ListTag && filterTag instanceof StringTag) {
            ListTag stackList = (ListTag) stackTag;
            return stringListFuncCompare(filterTag.getAsString(), stackList);
        } else if (filterTag instanceof ListTag && stackTag instanceof ListTag) {
            ListTag filterList = (ListTag) filterTag;
            ListTag stackList = (ListTag) stackTag;
            if (filterList.size() == 0)
                return false;
            return filterList.stream().allMatch(inbt -> stackList.stream().anyMatch(inbt1 -> deepFuzzyCompare(inbt, inbt1)));
        } else if (stackTag instanceof NumericTag && filterTag instanceof StringTag) {
            return stringNumericFuncCompare(filterTag.getAsString(), ((NumericTag) stackTag).getAsFloat());
        } else if ((stackTag instanceof NumericTag || stackTag instanceof StringTag) && filterTag instanceof ListTag) { // match any
            ListTag tags = (ListTag) stackTag;
            return tags.stream().anyMatch(c -> deepFuzzyCompare(stackTag, c));
        } else { // both are the same type
            return filterTag.equals(stackTag);
        }
    }

    public static int getConnectionsNotFullCount(boolean[] connections) {
        int count = 0;
        for (boolean connection : connections) {
            if (!connection) {
                count++;
            }
        }
        return count;
    }

}
