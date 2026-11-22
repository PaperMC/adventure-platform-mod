/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2024-2026 KyoriPowered
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
package net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.server.network;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.LocaleHolderBridge;
import net.kyori.adventure.platform.modcommon.impl.VanillaDialog;
import net.kyori.adventure.platform.modcommon.impl.server.FriendlyByteBufBridge;
import net.kyori.adventure.platform.modcommon.impl.server.MinecraftServerAudiencesImpl;
import net.kyori.adventure.platform.modcommon.impl.server.ServerResourcePacks;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin extends ServerCommonPacketListenerImplMixin implements Audience {
  // @formatter:off
  @Shadow private ClientInformation clientInformation;
  // @formatter:on

  private final Pointers adventure$pointers = Pointers.builder()
    .withDynamic(Identity.NAME, () -> this.shadow$playerProfile().name())
    .withDynamic(Identity.UUID, () -> this.shadow$playerProfile().id())
    .withDynamic(Identity.LOCALE, () -> LocaleHolderBridge.toLocale(this.clientInformation.language()))
    .build();

  @Override
  public @NotNull Pointers pointers() {
    return this.adventure$pointers;
  }

  // there is no player yet, so resource pack prompts are converted with the server's default audiences instance
  @Override
  public void sendResourcePacks(final @NotNull ResourcePackRequest request) {
    final MinecraftServerAudiencesImpl controller = (MinecraftServerAudiencesImpl) MinecraftServerAudiences.of(this.server);
    ServerResourcePacks.push(request, controller, this).forEach(this::shadow$send);
  }

  @Override
  public void removeResourcePacks(final @NotNull UUID id, final @NotNull UUID@NotNull... others) {
    ServerResourcePacks.pop(id, others).forEach(this::shadow$send);
  }

  @Override
  public void clearResourcePacks() {
    this.shadow$send(ServerResourcePacks.clear());
  }

  @Override
  public void showDialog(final @NotNull DialogLike dialog) {
    this.shadow$send(new ClientboundShowDialogPacket(VanillaDialog.unwrap(dialog)));
  }

  @Override
  public void closeDialog() {
    this.shadow$send(ClientboundClearDialogPacket.INSTANCE);
  }

  @ModifyArg(method = "handleConfigurationFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/SimpleUnboundProtocol;bind(Ljava/util/function/Function;)Lnet/minecraft/network/ProtocolInfo;"))
  private Function<ByteBuf, RegistryFriendlyByteBuf> adventure$injectPointers(final Function<ByteBuf, RegistryFriendlyByteBuf> original) {
    return buf -> {
      final RegistryFriendlyByteBuf wrapped = original.apply(buf);
      if (this.adventure$connection().getPacketListener() instanceof ServerGamePacketListenerImpl game) {
        ((FriendlyByteBufBridge) wrapped).adventure$data(AdventureCommon.pointered(game.player));
      }
      return wrapped;
    };
  }

  // Only the server's own pack belongs to the vanilla pack task, responses to packs sent through this audience must not finish it
  @WrapWithCondition(
    method = "handleResourcePackResponse",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerConfigurationPacketListenerImpl;finishCurrentTask(Lnet/minecraft/server/network/ConfigurationTask$Type;)V")
  )
  private boolean adventure$finishOnlyServerPackTask(final ServerConfigurationPacketListenerImpl self, final ConfigurationTask.Type type, @Local(argsOnly = true) final ServerboundResourcePackPacket packet) {
    return packet.id().equals(this.server.getServerResourcePack().map(MinecraftServer.ServerResourcePackInfo::id).orElse(null));
  }
}
