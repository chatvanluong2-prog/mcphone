package com.november.mcphone.feature.flashlight;

import com.november.mcphone.feature.flashlight.net.FlashlightPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Đèn pin phía server: mỗi tick bắn tia từ mắt người chơi theo hướng nhìn,
 * đặt một khối ánh sáng (minecraft:light) vào ô trống cuối cùng trước khi chạm vật cản.
 * Khối cũ được dọn khi tia dời đi, khi tắt đèn, khi thoát game hoặc đổi chiều không gian.
 * Chỉ ghi đè ô là không khí nên không phá bất kỳ khối thật nào.
 */
public final class Flashlight {

    private static final int RANGE = 24;
    private static final int LEVEL = 15;

    private record Placed(ServerLevel level, BlockPos pos) {}

    private static final Map<UUID, Placed> PLACED = new HashMap<>();
    private static final Map<UUID, Boolean> ON = new HashMap<>();

    private Flashlight() {}

    public static void handlePacket(FlashlightPacket packet, ServerPlayer player) {
        if (packet.on()) ON.put(player.getUUID(), true);
        else {
            ON.remove(player.getUUID());
            clear(player.getUUID());
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        if (!ON.containsKey(id)) return;
        if (player.isSpectator() || !player.isAlive()) {
            clear(id);
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(RANGE));
        var hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));

        BlockPos target;
        if (hit.getType() == HitResult.Type.BLOCK) {
            target = hit.getBlockPos().relative(hit.getDirection());
        } else {
            target = BlockPos.containing(end);
        }

        Placed old = PLACED.get(id);
        if (old != null && old.level() == level && old.pos().equals(target)) return;

        BlockState at = level.getBlockState(target);
        if (!at.isAir()) return; // không ghi đè khối thật; giữ đèn ở chỗ cũ

        clear(id);
        level.setBlock(target, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LEVEL), 3);
        PLACED.put(id, new Placed(level, target));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        ON.remove(id);
        clear(id);
    }

    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clear(event.getEntity().getUUID());
    }

    private static void clear(UUID id) {
        Placed p = PLACED.remove(id);
        if (p == null) return;
        if (p.level().getBlockState(p.pos()).is(Blocks.LIGHT)) {
            p.level().setBlock(p.pos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
