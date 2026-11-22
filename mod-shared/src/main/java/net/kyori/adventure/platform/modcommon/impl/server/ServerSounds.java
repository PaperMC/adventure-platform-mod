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

import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
import net.kyori.adventure.platform.modcommon.impl.GameEnums;
import net.kyori.adventure.platform.modcommon.impl.accessor.minecraft.world.level.LevelAccess;
import net.kyori.adventure.sound.Sound;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Sound packets for server audiences.
 */
final class ServerSounds {
  private ServerSounds() {
  }

  static Holder<SoundEvent> event(final MinecraftServerAudiencesImpl controller, final Sound sound) {
    final var soundKey = MinecraftAudiences.asNative(sound.name());
    final var event = controller.registryAccess().lookupOrThrow(Registries.SOUND_EVENT).get(ResourceKey.create(Registries.SOUND_EVENT, soundKey));
    return event.isPresent() ? event.get() : Holder.direct(SoundEvent.createVariableRangeEvent(soundKey));
  }

  static long seed(final Level level, final Sound sound) {
    return sound.seed().isPresent() ? sound.seed().getAsLong() : ((LevelAccess) level).accessor$soundSeedGenerator().nextLong();
  }

  static Sound seeded(final Level level, final Sound sound) {
    return sound.seed().isPresent() ? sound : Sound.sound(sound).seed(seed(level, sound)).build();
  }

  static ClientboundSoundPacket at(final Holder<SoundEvent> event, final Sound sound, final double x, final double y, final double z, final long seed) {
    return new ClientboundSoundPacket(event, GameEnums.SOUND_SOURCE.toMinecraft(sound.source()), x, y, z, sound.volume(), sound.pitch(), seed);
  }

  static ClientboundSoundEntityPacket from(final Holder<SoundEvent> event, final Sound sound, final Entity emitter, final long seed) {
    return new ClientboundSoundEntityPacket(event, GameEnums.SOUND_SOURCE.toMinecraft(sound.source()), emitter, sound.volume(), sound.pitch(), seed);
  }
}
