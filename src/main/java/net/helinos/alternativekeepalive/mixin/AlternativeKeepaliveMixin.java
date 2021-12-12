package net.helinos.alternativekeepalive.mixin;

import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class AlternativeKeepaliveMixin {

    private boolean processedDisconnect;

    @Inject(
            method = "onDisconnected",
            at = @At("HEAD"),
            cancellable = true
    )
    private void disconnected(Text reason, CallbackInfo ci) {
        if(processedDisconnect) {
            ci.cancel();
        } else {
            processedDisconnect = true;
        }
    }

    @Shadow
    private long lastKeepAliveTime;

    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    @Final
    static Logger LOGGER;

    @Shadow
    public abstract void disconnect(Text reason);

    @Shadow public abstract void sendPacket(Packet<?> packet);

    private final List<Long> keepAlives = new ArrayList<>();

    @Inject(
            method = "tick()V",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/server/MinecraftServer;getProfiler()Lnet/minecraft/util/profiler/Profiler;",
                ordinal = 0,
                shift = At.Shift.AFTER
            )
    )
    private void alternativeKeepalive(CallbackInfo ci) {
        long currentTime = Util.getMeasuringTimeMs();
        long elapsedTime = currentTime - lastKeepAliveTime;

        if (elapsedTime >= 1000L) { // 1 second
            if(!processedDisconnect && keepAlives.size() > 30000) {
                LOGGER.warn("{} was kicked due to keepalive timeout!", player.getName());
                disconnect(new TranslatableText("disconnect.timeout"));
            } else {
                lastKeepAliveTime = currentTime; // hijack this field for 1 second intervals
                keepAlives.add(currentTime); // currentTime is ID
                sendPacket(new KeepAliveS2CPacket(currentTime));
            }
        }
    }

    // Prevent vanilla behavior from ever running
    @Redirect(
            method = "tick()V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;lastKeepAliveTime:J",
                    opcode = Opcodes.GETFIELD
            )
    )
    private long prevent(ServerPlayNetworkHandler instance) {
        return Util.getMeasuringTimeMs();
    }

    /**
     * @author Helinos
     * @reason Honor patched packets
     */
    @Overwrite
    public void onKeepAlive(KeepAliveC2SPacket packet) {
        long id = packet.getId();
        if (keepAlives.size() > 0 && keepAlives.contains(id)) {
            int ping = (int) (Util.getMeasuringTimeMs() - id);
            player.pingMilliseconds = (player.pingMilliseconds * 3 + ping) / 4;
            keepAlives.clear(); // we got a valid response, lets roll with it and forget the rest
        }
    }
}
