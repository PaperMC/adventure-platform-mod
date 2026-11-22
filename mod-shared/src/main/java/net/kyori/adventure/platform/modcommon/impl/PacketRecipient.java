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
package net.kyori.adventure.platform.modcommon.impl;

import io.netty.util.AttributeKey;
import net.kyori.adventure.pointer.Pointered;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks the receiver of the packet currently being encoded on this thread.
 */
public final class PacketRecipient {
  public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("adventure:connection");
  private static final Pointered UNIDENTIFIED = new Pointered() {
  };
  private static final ThreadLocal<@Nullable Pointered> CURRENT = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> DONT_RENDER = ThreadLocal.withInitial(() -> false);

  private PacketRecipient() {
  }

  /**
   * Get the receiver of the packet currently being encoded.
   *
   * @return the receiver, or {@code null} if unknown
   */
  public static @Nullable Pointered current() {
    return CURRENT.get();
  }

  /**
   * Run {@code action} with {@code recipient} as the current receiver.
   *
   * @param recipient the receiver, or {@code null} if unknown
   * @param action the action
   */
  public static void encodeFor(final @Nullable Pointered recipient, final Runnable action) {
    final @Nullable Pointered previous = CURRENT.get();
    CURRENT.set(recipient);
    try {
      action.run();
    } finally {
      if (previous == null) {
        CURRENT.remove();
      } else {
        CURRENT.set(previous);
      }
    }
  }

  /**
   * Get whether rendering for the receiver is disabled on this thread.
   *
   * @return whether rendering is disabled
   */
  public static boolean renderingDisabled() {
    return DONT_RENDER.get();
  }

  /**
   * Wrap {@code codec} so nothing it encodes is rendered for the receiver.
   *
   * @param codec the codec
   * @param <B> the buffer type
   * @param <V> the value type
   * @return the wrapped codec
   */
  public static <B, V> StreamCodec<B, V> withoutRendering(final StreamCodec<B, V> codec) {
    return new StreamCodec<>() {
      @Override
      public V decode(final B input) {
        return codec.decode(input);
      }

      @Override
      public void encode(final B output, final V value) {
        final boolean previous = DONT_RENDER.get();
        DONT_RENDER.set(true);
        try {
          codec.encode(output, value);
        } finally {
          DONT_RENDER.set(previous);
        }
      }
    };
  }

  /**
   * Get the receiver of packets sent over {@code connection}.
   *
   * <p>Connections to clients that have not identified themselves as a player yet get a receiver without pointers.</p>
   *
   * @param connection the connection
   * @return the receiver, or {@code null} if the connection is not to a client
   */
  public static @Nullable Pointered of(final @Nullable Connection connection) {
    final @Nullable PacketListener listener = connection == null ? null : connection.getPacketListener();
    if (listener == null || listener.flow() != PacketFlow.SERVERBOUND) {
      return null;
    }
    return switch (listener) {
      case final ServerGamePacketListenerImpl game -> AdventureCommon.pointered(game.player);
      case final ServerConfigurationPacketListenerImpl configuration -> (Pointered) configuration;
      default -> UNIDENTIFIED;
    };
  }
}
