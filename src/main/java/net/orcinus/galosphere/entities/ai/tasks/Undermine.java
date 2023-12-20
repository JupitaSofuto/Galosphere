package net.orcinus.galosphere.entities.ai.tasks;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.orcinus.galosphere.entities.Berserker;
import net.orcinus.galosphere.entities.PinkSaltPillar;
import net.orcinus.galosphere.init.GEntityTypes;
import net.orcinus.galosphere.init.GMemoryModuleTypes;
import net.orcinus.galosphere.init.GSoundEvents;

import java.util.List;
import java.util.Optional;

public class Undermine extends Behavior<Berserker> {
    private static final int DURATION = Mth.ceil(22.4F);
    private static final int MAX_DURATION = 50;

    public Undermine() {
        super(ImmutableMap.of(
                GMemoryModuleTypes.RAMPAGE_TICKS.get(), MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
                GMemoryModuleTypes.IMPALING_COOLDOWN.get(), MemoryStatus.VALUE_ABSENT,
                GMemoryModuleTypes.IS_SMASHING.get(), MemoryStatus.VALUE_ABSENT,
                GMemoryModuleTypes.IS_IMPALING.get(), MemoryStatus.REGISTERED,
                GMemoryModuleTypes.IS_SUMMONING.get(), MemoryStatus.VALUE_ABSENT
        ), MAX_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverLevel, Berserker mob) {
        List<LivingEntity> list = serverLevel.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox());
        for (LivingEntity nearby : list) {
            if (nearby.isAlive() && nearby.getType() != GEntityTypes.BERSERKER.get()) {
                return false;
            }
        }
        return mob.hasPose(Pose.STANDING) && mob.shouldAttack() && mob.closerThan(mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get(), 15.0, 20.0);
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Berserker mob, long l) {
        return true;
    }

    @Override
    protected void start(ServerLevel serverLevel, Berserker mob, long l) {
        Brain<Berserker> brain = mob.getBrain();
        brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, DURATION);
        brain.setMemoryWithExpiry(GMemoryModuleTypes.IS_IMPALING.get(), Unit.INSTANCE, MAX_DURATION);
        brain.setMemory(GMemoryModuleTypes.IMPALING_COUNT.get(), brain.getMemory(GMemoryModuleTypes.IMPALING_COUNT.get()).orElse(0) + 1);
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        mob.setPhase(Berserker.Phase.UNDERMINE);
        mob.playSound(GSoundEvents.BERSERKER_SMASH.get(), 3.0f, 1.0f);
    }

    @Override
    protected void tick(ServerLevel serverLevel, Berserker mob, long l) {
        Optional<LivingEntity> memory = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        memory.ifPresent(target -> mob.getLookControl().setLookAt(target.position()));
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN)) {
            return;
        }
        if (memory.isPresent() && mob.canTargetEntity(memory.get())) {
            LivingEntity target = memory.get();
            double d = Math.min(target.getY(), mob.getY());
            double e = Math.max(target.getY(), mob.getY()) + 1.0;
            float f = (float)Mth.atan2(target.getZ() - mob.getZ(), target.getX() - mob.getX());
            if (mob.distanceTo(target) < 4.0D) {
                float g;
                int i;
                for (i = 0; i < 5; ++i) {
                    g = f + (float)i * (float)Math.PI * 0.4f;
                    this.createPillar(mob, mob.getX() + (double)Mth.cos(g) * 1.5, mob.getZ() + (double)Mth.sin(g) * 1.5, d, e, g, 0);
                }
                for (i = 0; i < 8; ++i) {
                    g = f + (float)i * (float)Math.PI * 2.0f / 8.0f + 1.2566371f;
                    this.createPillar(mob, mob.getX() + (double)Mth.cos(g) * 2.5, mob.getZ() + (double)Mth.sin(g) * 2.5, d, e, g, 3);
                }
            } else {
                for (int i = 0; i < 16; ++i) {
                    double h = 1.25 * (double)(i + 1);
                    this.createPillar(mob, mob.getX() + (double)Mth.cos(f) * h + ((mob.getRandom().nextFloat() - 0.5F) * 0.4F), mob.getZ() + (double)Mth.sin(f) * h + ((mob.getRandom().nextFloat() - 0.5F) * 0.4F), d, e, f, i);
                }
            }
        }
        mob.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, MAX_DURATION- DURATION);
    }

    private void createPillar(Berserker blighted, double d, double e, double f, double g, float h, int i) {
        BlockPos blockPos = BlockPos.containing(d, g, e);
        boolean bl = false;
        double j = 0.0;
        do {
            VoxelShape voxelShape;
            BlockPos blockPos2 = blockPos.below();
            BlockState blockState = blighted.level().getBlockState(blockPos2);
            if (!blockState.isFaceSturdy(blighted.level(), blockPos2, Direction.UP)) continue;
            if (!blighted.level().isEmptyBlock(blockPos) && !(voxelShape = blighted.level().getBlockState(blockPos).getCollisionShape(blighted.level(), blockPos)).isEmpty()) {
                j = voxelShape.max(Direction.Axis.Y);
            }
            bl = true;
            break;
        } while ((blockPos = blockPos.below()).getY() >= Mth.floor(f) - 1);
        if (bl) {
            blighted.level().addFreshEntity(new PinkSaltPillar(blighted.level(), d, (double)blockPos.getY() + j, e, h, i, blighted));
        }
    }

    @Override
    protected void stop(ServerLevel serverLevel, Berserker mob, long l) {
        mob.setPhase(Berserker.Phase.IDLING);
        Optional<LivingEntity> memory = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        Optional<Integer> undermine = mob.getBrain().getMemory(GMemoryModuleTypes.IMPALING_COUNT.get());
        if (undermine.isPresent() && undermine.get() >= 3) {
            mob.getBrain().setMemoryWithExpiry(GMemoryModuleTypes.IMPALING_COOLDOWN.get(), Unit.INSTANCE, MAX_DURATION);
            mob.getBrain().setMemory(GMemoryModuleTypes.IMPALING_COUNT.get(), 0);
        } else if (memory.isPresent() && memory.get().distanceTo(mob) < 4.0D) {
            mob.getBrain().setMemoryWithExpiry(GMemoryModuleTypes.IMPALING_COOLDOWN.get(), Unit.INSTANCE, 400);
        }
    }
}