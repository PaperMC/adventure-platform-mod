/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2026 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.network.codec;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.DynamicOps;
import net.kyori.adventure.platform.modcommon.impl.PointeredOps;
import net.kyori.adventure.platform.modcommon.impl.server.FriendlyByteBufBridge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// The stream codec created by ByteBufCodecs.fromCodecWithRegistries(Codec, Supplier)
@Mixin(targets = "net.minecraft.network.codec.ByteBufCodecs$20")
public abstract class ByteBufCodecs_FromCodecWithRegistriesMixin {
  @ModifyArg(method = "encode", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;encodeStart(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"))
  private DynamicOps<?> adventure$attachRecipient(final DynamicOps<?> ops, @Local(argsOnly = true) final RegistryFriendlyByteBuf output) {
    return output instanceof FriendlyByteBufBridge bridge ? PointeredOps.withPointered(ops, bridge.adventure$recipient()) : ops;
  }
}
