package com.mohistmc.banner.mixin.world.level.spawner;

import com.mohistmc.banner.injection.world.level.spawner.InjectionSpawnState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelData;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftSpawnCategory;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;

@Mixin(NaturalSpawner.class)
public abstract class MixinNaturalSpawner {

    // @formatter:off
    @Shadow @Final private static MobCategory[] SPAWNING_CATEGORIES;
    // @formatter:on

    @Shadow
    public static void spawnCategoryForChunk(MobCategory category, ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnPredicate filter, NaturalSpawner.AfterSpawnCallback callback) {
    }

    @Shadow
    private static boolean isRightDistanceToPlayerAndSpawnPoint(ServerLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos pos, double distance) {
        return false;
    }

    @Shadow
    private static Optional<MobSpawnSettings.SpawnerData> getRandomSpawnMobAt(ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory category, RandomSource random, BlockPos pos) {
        return Optional.empty();
    }

    @Shadow
    @Nullable
    private static Mob getMobForSpawn(ServerLevel level, EntityType<?> entityType) {
        return null;
    }

    @Shadow
    private static boolean isValidSpawnPostitionForType(ServerLevel level, MobCategory category, StructureManager structureManager, ChunkGenerator generator, MobSpawnSettings.SpawnerData data, BlockPos.MutableBlockPos pos, double distance) {
        return false;
    }

    @Shadow
    private static boolean isValidPositionForMob(ServerLevel level, Mob mob, double distance) {
        return false;
    }

    /**
     * @author wdog5
     * @reason bukkit things
     */
    @Overwrite
    public static void spawnForChunk(ServerLevel worldserver, LevelChunk chunk, NaturalSpawner.SpawnState spawnercreature_d, boolean flag, boolean flag1, boolean flag2) {
        worldserver.getProfiler().push("spawner");
        worldserver.bridge$timings().mobSpawn.startTiming(); // Spigot
        MobCategory[] aenumcreaturetype = SPAWNING_CATEGORIES;
        int i = aenumcreaturetype.length;

        LevelData worlddata = worldserver.getLevelData(); // CraftBukkit - Other mob type spawn tick rate

        for (int j = 0; j < i; ++j) {
            MobCategory enumcreaturetype = aenumcreaturetype[j];
            // CraftBukkit start - Use per-world spawn limits
            boolean spawnThisTick = true;
            int limit = enumcreaturetype.getMaxInstancesPerChunk();
            SpawnCategory spawnCategory = CraftSpawnCategory.toBukkit(enumcreaturetype);
            if (CraftSpawnCategory.isValidForLimits(spawnCategory)) {
                spawnThisTick = worldserver.bridge$ticksPerSpawnCategory().getLong(spawnCategory) != 0 && worlddata.getGameTime() % worldserver.bridge$ticksPerSpawnCategory().getLong(spawnCategory) == 0;
                limit = worldserver.getWorld().getSpawnLimit(spawnCategory);
            }

            if (!spawnThisTick || limit == 0) {
                continue;
            }

            if ((flag || !enumcreaturetype.isFriendly()) && (flag1 || enumcreaturetype.isFriendly()) && (flag2 || !enumcreaturetype.isPersistent()) &&  ((InjectionSpawnState) spawnercreature_d).canSpawnForCategory(enumcreaturetype, chunk.getPos(), limit)) {
                // CraftBukkit end
                Objects.requireNonNull(spawnercreature_d);
                NaturalSpawner.SpawnPredicate spawnercreature_c = spawnercreature_d::canSpawn;

                Objects.requireNonNull(spawnercreature_d);
                spawnCategoryForChunk(enumcreaturetype, worldserver, chunk, spawnercreature_c, spawnercreature_d::afterSpawn);
            }
        }

        worldserver.bridge$timings().mobSpawn.stopTiming(); // Spigot
        worldserver.getProfiler().pop();
    }

    @Inject(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void banner$naturalSpawn(MobCategory category, ServerLevel level, ChunkAccess chunk, BlockPos pos, NaturalSpawner.SpawnPredicate filter, NaturalSpawner.AfterSpawnCallback callback, CallbackInfo ci) {
         level.pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }

    /**
     * @author wdog5
     * @reason bukkit things
     */
    @Overwrite
    public static void spawnCategoryForPosition(MobCategory enumcreaturetype, ServerLevel worldserver, ChunkAccess ichunkaccess, BlockPos blockposition, NaturalSpawner.SpawnPredicate spawnercreature_c, NaturalSpawner.AfterSpawnCallback spawnercreature_a) {
        StructureManager structuremanager = worldserver.structureManager();
        ChunkGenerator chunkgenerator = worldserver.getChunkSource().getGenerator();
        int i = blockposition.getY();
        BlockState iblockdata = ichunkaccess.getBlockState(blockposition);

        if (!iblockdata.isRedstoneConductor(ichunkaccess, blockposition)) {
            BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
            int j = 0;
            int k = 0;

            while (k < 3) {
                int l = blockposition.getX();
                int i1 = blockposition.getZ();
                boolean flag = true;
                MobSpawnSettings.SpawnerData biomesettingsmobs_c = null;
                SpawnGroupData groupdataentity = null;
                int j1 = Mth.ceil(worldserver.random.nextFloat() * 4.0F);
                int k1 = 0;
                int l1 = 0;

                while (true) {
                    if (l1 < j1) {
                        label53:
                        {
                            l += worldserver.random.nextInt(6) - worldserver.random.nextInt(6);
                            i1 += worldserver.random.nextInt(6) - worldserver.random.nextInt(6);
                            blockposition_mutableblockposition.set(l, i, i1);
                            double d0 = (double) l + 0.5D;
                            double d1 = (double) i1 + 0.5D;
                            Player entityhuman = worldserver.getNearestPlayer(d0, (double) i, d1, -1.0D, false);

                            if (entityhuman != null) {
                                double d2 = entityhuman.distanceToSqr(d0, (double) i, d1);

                                if (isRightDistanceToPlayerAndSpawnPoint(worldserver, ichunkaccess, blockposition_mutableblockposition, d2)) {
                                    if (biomesettingsmobs_c == null) {
                                        Optional<MobSpawnSettings.SpawnerData> optional = getRandomSpawnMobAt(worldserver, structuremanager, chunkgenerator, enumcreaturetype, worldserver.random, blockposition_mutableblockposition);

                                        if (optional.isEmpty()) {
                                            break label53;
                                        }

                                        biomesettingsmobs_c = (MobSpawnSettings.SpawnerData) optional.get();
                                        j1 = biomesettingsmobs_c.minCount + worldserver.random.nextInt(1 + biomesettingsmobs_c.maxCount - biomesettingsmobs_c.minCount);
                                    }

                                    if (isValidSpawnPostitionForType(worldserver, enumcreaturetype, structuremanager, chunkgenerator, biomesettingsmobs_c, blockposition_mutableblockposition, d2) && spawnercreature_c.test(biomesettingsmobs_c.type, blockposition_mutableblockposition, ichunkaccess)) {
                                        Mob entityinsentient = getMobForSpawn(worldserver, biomesettingsmobs_c.type);

                                        if (entityinsentient == null) {
                                            return;
                                        }

                                        entityinsentient.moveTo(d0, (double) i, d1, worldserver.random.nextFloat() * 360.0F, 0.0F);
                                        if (isValidPositionForMob(worldserver, entityinsentient, d2)) {
                                            groupdataentity = entityinsentient.finalizeSpawn(worldserver, worldserver.getCurrentDifficultyAt(entityinsentient.blockPosition()), MobSpawnType.NATURAL, groupdataentity, (CompoundTag) null);
                                            // CraftBukkit start
                                            // SPIGOT-7045: Give ocelot babies back their special spawn reason. Note: This is the only modification required as ocelots count as monsters which means they only spawn during normal chunk ticking and do not spawn during chunk generation as starter mobs.
                                            if (entityinsentient instanceof Ocelot && !((org.bukkit.entity.Ageable) entityinsentient.getBukkitEntity()).isAdult()) {
                                                worldserver.pushAddEntityReason(CreatureSpawnEvent.SpawnReason.OCELOT_BABY);
                                            }else {
                                                worldserver.pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
                                            }

                                            worldserver.addFreshEntityWithPassengers(entityinsentient);
                                            if (!entityinsentient.isRemoved()) {
                                                ++j;
                                                ++k1;
                                                spawnercreature_a.run(entityinsentient, ichunkaccess);
                                            }
                                            // CraftBukkit end
                                            if (j >= entityinsentient.getMaxSpawnClusterSize()) {
                                                return;
                                            }

                                            if (entityinsentient.isMaxGroupSizeReached(k1)) {
                                                break label53;
                                            }
                                        }
                                    }
                                }
                            }

                            ++l1;
                            continue;
                        }
                    }

                    ++k;
                    break;
                }
            }

        }
    }

    @Inject(method = "spawnMobsForChunkGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void banner$worldGenSpawn(ServerLevelAccessor levelAccessor, Holder<Biome> biome, ChunkPos chunkPos, RandomSource random, CallbackInfo ci) {
        levelAccessor.pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }
}
