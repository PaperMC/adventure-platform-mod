/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 KyoriPowered
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
package net.kyori.adventure.platform.modcommon.impl.server;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps a player's command source, so commands run as another entity still resolve to the player's audience.
 */
public final class PlayerCommandSource implements CommandSource, RenderableAudience, ForwardingAudience.Single {
  private final ServerPlayer player;
  private final CommandSource delegate;

  public PlayerCommandSource(final ServerPlayer player, final CommandSource delegate) {
    this.player = player;
    this.delegate = delegate;
  }

  @Override
  public void sendSystemMessage(final Component message) {
    this.delegate.sendSystemMessage(message);
  }

  @Override
  public boolean acceptsSuccess() {
    return this.delegate.acceptsSuccess();
  }

  @Override
  public boolean acceptsFailure() {
    return this.delegate.acceptsFailure();
  }

  @Override
  public boolean shouldInformAdmins() {
    return this.delegate.shouldInformAdmins();
  }

  @Override
  public boolean alwaysAccepts() {
    return this.delegate.alwaysAccepts();
  }

  @Override
  public @NotNull Audience audience() {
    return (Audience) this.player;
  }

  @Override
  public Audience renderUsing(final MinecraftServerAudiencesImpl controller) {
    return ((RenderableAudience) this.player).renderUsing(controller);
  }

  @Override
  public void refresh() {
    ((RenderableAudience) this.player).refresh();
  }
}
