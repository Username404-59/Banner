package com.mohistmc.banner.mixin.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.CampfireStartEvent;
import org.bukkit.inventory.CampfireRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(CampfireBlockEntity.class)
public abstract class MixinCampfireBlockEntity extends BlockEntity {

    @Shadow @Final private RecipeManager.CachedCheck<Container, CampfireCookingRecipe> quickCheck;
    @Shadow
    public abstract Optional<CampfireCookingRecipe> getCookableRecipe(ItemStack p_59052_);

    @Shadow @Final public int[] cookingTime;

    public MixinCampfireBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void cookTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity entity) {
        boolean flag = false;

        for (int i = 0; i < entity.getItems().size(); ++i) {
            ItemStack itemstack = entity.getItems().get(i);
            if (!itemstack.isEmpty()) {
                flag = true;
                entity.cookingProgress[i]++;
                if (entity.cookingProgress[i] >= entity.cookingTime[i]) {
                    Container container = new SimpleContainer(itemstack);
                    ItemStack itemstack1 = ((MixinCampfireBlockEntity) (Object) entity).quickCheck.getRecipeFor(container, level).map((p_155305_) -> {
                        return p_155305_.assemble(container, level.registryAccess());
                    }).orElse(itemstack);

                    if (!itemstack1.isItemEnabled(level.enabledFeatures())) continue;
                    CraftItemStack source = CraftItemStack.asCraftMirror(itemstack);
                    org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(itemstack1);

                    BlockCookEvent blockCookEvent = new BlockCookEvent(CraftBlock.at(level, pos), source, result);
                    Bukkit.getPluginManager().callEvent(blockCookEvent);

                    if (blockCookEvent.isCancelled()) {
                        return;
                    }

                    result = blockCookEvent.getResult();
                    itemstack1 = CraftItemStack.asNMSCopy(result);

                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemstack1);
                    entity.getItems().set(i, ItemStack.EMPTY);
                    level.sendBlockUpdated(pos, state, state, 3);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
                }
            }
        }

        if (flag) {
            setChanged(level, pos, state);
        }

    }

    @Inject(method = "placeFood", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;cookingProgress:[I"))
    private void banner$cookStart(Entity p_238285_, ItemStack stack, int p_238287_, CallbackInfoReturnable<Boolean> cir, int i) {
        var event = new CampfireStartEvent(CraftBlock.at(this.level, this.worldPosition), CraftItemStack.asCraftMirror(stack), (CampfireRecipe)  getCookableRecipe(stack).get().toBukkitRecipe());
        Bukkit.getPluginManager().callEvent(event);
        this.cookingTime[i] = event.getTotalCookTime();
    }
}
