package io.github.mortuusars.exposure_catalogue.network.packet;

import io.github.mortuusars.exposure_catalogue.network.PacketDirection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface IPacket {
    default FriendlyByteBuf toBuffer(FriendlyByteBuf buffer) { return buffer; }
    /**
     * @param player will be null when on the client.
     */
    boolean handle(PacketDirection direction, @Nullable Player player);
    ResourceLocation getId();
}