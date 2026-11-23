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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Records the sound packets the client receives, whether or not it can play them.
 */
public final class ReceivedSounds {
  private static final List<Received> RECEIVED = new CopyOnWriteArrayList<>();

  public enum Kind {
    AT_POSITION,
    FROM_ENTITY,
    STOP
  }

  /**
   * A received sound packet.
   *
   * @param kind the kind of packet
   * @param sound the sound's id, or an empty string when stopping every sound
   * @param position where the sound plays, only for {@link Kind#AT_POSITION}
   */
  public record Received(Kind kind, String sound, @Nullable Vec3 position) {
  }

  private ReceivedSounds() {
  }

  public static void record(final ClientboundSoundPacket packet) {
    RECEIVED.add(new Received(Kind.AT_POSITION, id(packet.getSound()), new Vec3(packet.getX(), packet.getY(), packet.getZ())));
  }

  public static void record(final ClientboundSoundEntityPacket packet) {
    RECEIVED.add(new Received(Kind.FROM_ENTITY, id(packet.getSound()), null));
  }

  public static void record(final ClientboundStopSoundPacket packet) {
    RECEIVED.add(new Received(Kind.STOP, packet.getName() == null ? "" : packet.getName().toString(), null));
  }

  private static String id(final Holder<SoundEvent> sound) {
    return sound.unwrapKey().map(key -> key.identifier().toString()).orElse("direct");
  }

  static List<Received> snapshot() {
    return List.copyOf(RECEIVED);
  }

  static void clear() {
    RECEIVED.clear();
  }
}
