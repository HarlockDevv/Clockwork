package org.valkyrienskies.clockwork.mixin.content.fan;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.forces.EncasedFanController;
import org.valkyrienskies.clockwork.content.generic.IForceApplierBE;
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanCreateData;
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanData;
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanUpdateData;
import org.valkyrienskies.clockwork.util.ClockworkConstants;

@Mixin(EncasedFanBlockEntity.class)
public abstract class MixinEncasedFanTileEntity extends KineticBlockEntity implements IForceApplierBE<EncasedFanUpdateData, EncasedFanData, EncasedFanCreateData, EncasedFanController> {

//    @Shadow(remap = false)
//    LerpedFloat visualSpeed;

    public MixinEncasedFanTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    // VS2 ship controller tick removed - EncasedFanController requires Valkyrien Skies 2
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void vs_clockwork$injectTick(CallbackInfo ci) {
        // no-op without VS2
    }

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void vs_clockwork$injectWrite(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        if (!isVirtual()) {
            if (getPhysID() != -1.0) {
                compound.putInt(ClockworkConstants.Nbt.FAN_ID, getPhysID());
            }
        }
    }

    @Inject(method = "read", at = @At("TAIL"), remap = false)
    private void vs_clockwork$injectRead(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        if (!isVirtual()) {
            if (compound.contains(ClockworkConstants.Nbt.FAN_ID)) {
                setPhysID(compound.getInt(ClockworkConstants.Nbt.FAN_ID));
            }
        }
    }


    // VS2 ship controller remove logic commented out - requires Valkyrien Skies 2
    // @Inject(method = "remove", at = @At("HEAD"), remap = false)
    // private void vs_clockwork$injectRemove(CallbackInfo ci) { ... }
}
