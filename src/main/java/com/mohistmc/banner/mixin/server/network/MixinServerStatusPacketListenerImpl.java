package com.mohistmc.banner.mixin.server.network;

import com.mohistmc.banner.bukkit.BannerServerListPingEvent;
import com.mohistmc.banner.bukkit.BukkitExtraConstants;
import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Mixin(ServerStatusPacketListenerImpl.class)
public class MixinServerStatusPacketListenerImpl {

    @Redirect(method = "handleStatusRequest", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void banner$handleServerPing(Connection networkManager, Packet<?> packetIn) {
        MinecraftServer server = BukkitExtraConstants.getServer();
        Object[] players = server.getPlayerList().players.toArray();
        BannerServerListPingEvent event = new BannerServerListPingEvent(networkManager, server);
        List<GameProfile> profiles = new ArrayList<>(players.length);
        Object[] array;
        for (int length = (array = players).length, i = 0; i < length; ++i) {
            ServerPlayer player = (ServerPlayer) array[i];
            if (player != null) {
                if (player.allowsListing()) {
                    profiles.add(player.getGameProfile());
                } else {
                    profiles.add(MinecraftServer.ANONYMOUS_PLAYER_PROFILE);
                }
            }
        }
        if (!server.hidesOnlinePlayers() && !profiles.isEmpty()) {
            Collections.shuffle(profiles);
            profiles = profiles.subList(0, Math.min(profiles.size(), SpigotConfig.playerSample));
        }
        ServerStatus.Players playerSample = new ServerStatus.Players(event.getMaxPlayers(), profiles.size(), (server.hidesOnlinePlayers()) ? Collections.emptyList() : profiles);
        ServerStatus ping = new ServerStatus(
                CraftChatMessage.fromString(event.getMotd(), true)[0],
                Optional.of(playerSample),
                Optional.of(new ServerStatus.Version(server.getServerModName() + " " + server.getServerVersion(), SharedConstants.getCurrentVersion().getProtocolVersion())),
                (event.icon.value != null) ? Optional.of(new ServerStatus.Favicon(event.icon.value)) : Optional.empty(),
                server.enforceSecureProfile());
        networkManager.send(new ClientboundStatusResponsePacket(ping));
    }
}
