package net.mineclick.game.util.nms;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerData;

public class NMS {
    public final static WrappedDataWatcher.Serializer BOOLEAN_TYPE = WrappedDataWatcher.Registry.get(Boolean.class);
    public final static WrappedDataWatcher.Serializer VILLAGER_DATA_TYPE = WrappedDataWatcher.Registry.get(VillagerData.class);
    public final static WrappedDataWatcher.Serializer CHAT_COMPONENT_TYPE_OPT = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
    public final static WrappedDataWatcher.Serializer CHAT_COMPONENT_TYPE = WrappedDataWatcher.Registry.getChatComponentSerializer();
    public final static WrappedDataWatcher.Serializer BYTE_TYPE = WrappedDataWatcher.Registry.get(Byte.class);
    public final static WrappedDataWatcher.Serializer INT_TYPE = WrappedDataWatcher.Registry.get(Integer.class);
    public final static WrappedDataWatcher.Serializer BLOCK_POS = WrappedDataWatcher.Registry.getBlockPositionSerializer(false);
    public final static WrappedDataWatcher.Serializer ITEM = WrappedDataWatcher.Registry.getItemStackSerializer(false);

    public static int createId() {
        return Entity.nextEntityId();
    }
}
