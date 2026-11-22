/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2023-2026 KyoriPowered
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

import com.google.common.collect.Iterables;
import java.util.function.Supplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.sound.Sound;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * The players of a server or a world.
 *
 * <p>Sounds are played to them together like vanilla's level sounds: with one seed, and only to players in range when played at a position or entity.</p>
 */
final class BroadcastAudience implements ForwardingAudience {
  private final MinecraftServerAudiencesImpl controller;
  private final Supplier<? extends Iterable<ServerPlayer>> players;
  private final Supplier<? extends Iterable<ServerLevel>> levels;
  private final ServerLevel seedSource;

  /**
   * Create a new audience.
   *
   * @param controller the controller
   * @param players the players
   * @param levels the levels sounds at a position or entity are played in
   * @param seedSource the level to generate sound seeds with
   */
  BroadcastAudience(final MinecraftServerAudiencesImpl controller, final Supplier<? extends Iterable<ServerPlayer>> players, final Supplier<? extends Iterable<ServerLevel>> levels, final ServerLevel seedSource) {
    this.controller = controller;
    this.players = players;
    this.levels = levels;
    this.seedSource = seedSource;
  }

  @Override
  public @NotNull Iterable<? extends Audience> audiences() {
    return Iterables.transform(this.players.get(), this.controller::audience);
  }

  @Override
  public void playSound(final @NotNull Sound sound) {
    final Sound seeded = ServerSounds.seeded(this.seedSource, sound);
    for (final Audience audience : this.audiences()) {
      audience.playSound(seeded);
    }
  }

  @Override
  public void playSound(final @NotNull Sound sound, final double x, final double y, final double z) {
    final Holder<SoundEvent> event = ServerSounds.event(this.controller, sound);
    final Packet<?> packet = ServerSounds.at(event, sound, x, y, z, ServerSounds.seed(this.seedSource, sound));
    for (final ServerLevel level : this.levels.get()) {
      this.broadcast(level, x, y, z, event.value().getRange(sound.volume()), packet);
    }
  }

  @Override
  public void playSound(final @NotNull Sound sound, final Sound.@NotNull Emitter emitter) {
    if (emitter == Sound.Emitter.self()) {
      final Sound seeded = ServerSounds.seeded(this.seedSource, sound);
      for (final Audience audience : this.audiences()) {
        audience.playSound(seeded, Sound.Emitter.self());
      }
    } else if (emitter instanceof final Entity entity) {
      if (entity.level() instanceof final ServerLevel level && Iterables.contains(this.levels.get(), level)) {
        final Holder<SoundEvent> event = ServerSounds.event(this.controller, sound);
        this.broadcast(level, entity.getX(), entity.getY(), entity.getZ(), event.value().getRange(sound.volume()), ServerSounds.from(event, sound, entity, ServerSounds.seed(level, sound)));
      }
    } else {
      throw new IllegalArgumentException("Provided emitter '" + emitter + "' was not Sound.Emitter.self() or an Entity");
    }
  }

  private void broadcast(final ServerLevel level, final double x, final double y, final double z, final double range, final Packet<?> packet) {
    level.getServer().getPlayerList().broadcast(null, x, y, z, range, level.dimension(), packet);
  }
}
