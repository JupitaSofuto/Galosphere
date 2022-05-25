package net.orcinus.galosphere.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.orcinus.galosphere.blocks.LumiereBlock;
import net.orcinus.galosphere.init.GBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(LightningBolt.class)
public class LightningBoltMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LightningBolt;clearCopperOnLightningStrike(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"), method = "tick", cancellable = true)
    private void tick(CallbackInfo ci) {
        LightningBolt $this = (LightningBolt)(Object)this;
        LightningBoltAccessor accessor = (LightningBoltAccessor) $this;
        clearCopperOnLightningStrike(((LightningBolt)(Object)this).level, accessor.callGetStrikePosition());
    }

    private static void clearCopperOnLightningStrike(Level world, BlockPos pos) {
        BlockState blockstate = world.getBlockState(pos);
        BlockPos blockpos;
        BlockState blockstate1;
        if (blockstate.is(Blocks.LIGHTNING_ROD)) {
            blockpos = pos.relative(blockstate.getValue(LightningRodBlock.FACING).getOpposite());
            blockstate1 = world.getBlockState(blockpos);
        } else {
            blockpos = pos;
            blockstate1 = blockstate;
        }

        if (blockstate1.getBlock() instanceof LumiereBlock) {
            world.setBlockAndUpdate(blockpos, GBlocks.CHARGED_LUMIERE_BLOCK.get().defaultBlockState());
            BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();
            int i = world.random.nextInt(3) + 3;

            for(int j = 0; j < i; ++j) {
                int k = world.random.nextInt(8) + 1;
                randomWalkCleaningCopper(world, blockpos, blockpos$mutableblockpos, k);
            }

        }
    }

    private static void randomWalkCleaningCopper(Level world, BlockPos pos, BlockPos.MutableBlockPos mut, int tries) {
        mut.set(pos);

        for(int i = 0; i < tries; ++i) {
            Optional<BlockPos> optional = randomStepCleaningCopper(world, mut);
            if (optional.isEmpty()) {
                break;
            }

            mut.set(optional.get());
        }
    }

    private static Optional<BlockPos> randomStepCleaningCopper(Level world, BlockPos pos) {
        for(BlockPos blockpos : BlockPos.randomInCube(world.random, 10, pos, 1)) {
            BlockState blockstate = world.getBlockState(blockpos);
            if (blockstate.getBlock() instanceof LumiereBlock) {
                world.setBlockAndUpdate(blockpos, GBlocks.CHARGED_LUMIERE_BLOCK.get().defaultBlockState());
                world.levelEvent(3002, blockpos, -1);
                return Optional.of(blockpos);
            }
        }

        return Optional.empty();
    }


}