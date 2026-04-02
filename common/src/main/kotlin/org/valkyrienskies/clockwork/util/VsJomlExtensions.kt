package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.primitives.AABBd
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i

// Local replacements for VS2 JOML extension functions (org.valkyrienskies.mod.common.util.toJOML / toJOMLD)
// These were previously provided by Valkyrien Skies 2 as extension functions on Minecraft types.

fun BlockPos.toJOML(): Vector3i = Vector3i(x, y, z)
fun Vec3i.toJOMLD(): Vector3d = Vector3d(x.toDouble(), y.toDouble(), z.toDouble())
fun Vec3i.toJOML(): Vector3i = Vector3i(x, y, z)
fun BlockPos.toJOMLD(): Vector3d = Vector3d(x.toDouble(), y.toDouble(), z.toDouble())
fun Vec3.toJOML(): Vector3d = Vector3d(x, y, z)
fun Vec3.toJOMLD(): Vector3d = Vector3d(x, y, z)
fun AABB.toJOML(): AABBd = AABBd(minX, minY, minZ, maxX, maxY, maxZ)
fun Vector3d.toMinecraft(): Vec3 = Vec3(x, y, z)
fun Vector3i.toBlockPos(): BlockPos = BlockPos(x, y, z)

// Replacements for org.valkyrienskies.mod.util.putVector3d / getVector3d (VS2 NBT helpers)
fun CompoundTag.putVector3d(key: String, vec: Vector3dc) {
    putDouble("${key}X", vec.x())
    putDouble("${key}Y", vec.y())
    putDouble("${key}Z", vec.z())
}

fun CompoundTag.getVector3dVS(key: String): Vector3d? {
    if (!contains("${key}X") || !contains("${key}Y") || !contains("${key}Z")) return null
    return Vector3d(getDouble("${key}X"), getDouble("${key}Y"), getDouble("${key}Z"))
}
