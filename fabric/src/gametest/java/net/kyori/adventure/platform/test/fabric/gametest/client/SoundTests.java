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
package net.kyori.adventure.platform.test.fabric.gametest.client;

import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.minecraft.world.phys.Vec3;

import static net.kyori.adventure.key.Key.key;

final class SoundTests implements ClientTestGroup {
  private static final Sound SOUND = Sound.sound(key("entity.experience_orb.pickup"), Sound.Source.MASTER, 1f, 1f);

  @Override
  public void run(final WorldTestSession session) {
    session.test("sound played to the player", () -> {
      session.player(player -> player.playSound(SOUND));
      session.await("the sound", () -> session.sounds().stream().anyMatch(sound -> sound.sound().equals(SOUND.name().asString())));
    });

    session.test("sound from an entity", () -> {
      session.player(player -> player.playSound(SOUND, Sound.Emitter.self()));
      session.await("the sound", () -> session.sounds().stream().anyMatch(sound -> sound.kind() == ReceivedSounds.Kind.FROM_ENTITY && sound.sound().equals(SOUND.name().asString())));
    });

    session.test("stop sound", () -> {
      session.player(player -> player.stopSound(SoundStop.named(SOUND.name())));
      session.await("the sound to be stopped", () -> session.sounds().stream().anyMatch(sound -> sound.kind() == ReceivedSounds.Kind.STOP && sound.sound().equals(SOUND.name().asString())));
    });

    // A sound played at a position in a world only reaches players within its range, like vanilla's level sounds
    session.test("world sound only reaches players in range", () -> {
      final Vec3[] positions = new Vec3[2];
      session.serverPlayer(player -> {
        positions[0] = player.position();
        positions[1] = player.position().add(1000, 0, 0);
        final var world = MinecraftServerAudiences.of(player.level().getServer()).world(player.level().dimension().identifier());
        world.playSound(SOUND, positions[1].x, positions[1].y, positions[1].z);
        world.playSound(SOUND, positions[0].x, positions[0].y, positions[0].z);
      });
      session.await("the nearby sound", () -> session.sounds().stream().anyMatch(sound -> sound.kind() == ReceivedSounds.Kind.AT_POSITION && sound.position().closerThan(positions[0], 1)));
      session.sync();
      if (session.sounds().stream().anyMatch(sound -> sound.kind() == ReceivedSounds.Kind.AT_POSITION && sound.position().closerThan(positions[1], 1))) {
        throw new AssertionError("The player heard a sound out of range: " + session.sounds());
      }
    });
  }
}
