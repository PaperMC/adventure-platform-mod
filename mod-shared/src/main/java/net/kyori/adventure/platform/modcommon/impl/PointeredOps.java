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

import com.mojang.serialization.DynamicOps;
import net.kyori.adventure.pointer.Pointered;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.DelegatingOps;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

/**
 * Ops carrying the {@link Pointered} that the encoded data is meant for.
 *
 * <p>Codecs can use this to render content for the receiver.</p>
 */
public interface PointeredOps {
  /**
   * Get the receiver of the encoded data.
   *
   * @return the receiver
   */
  Pointered pointered();

  /**
   * Get the receiver carried by {@code ops}, if any.
   *
   * @param ops the ops
   * @return the receiver, or {@code null} if the ops carry none
   */
  static @Nullable Pointered pointered(final DynamicOps<?> ops) {
    return ops instanceof PointeredOps pointeredOps ? pointeredOps.pointered() : null;
  }

  /**
   * Attach a receiver to {@code ops}, keeping registry access if present.
   *
   * @param ops the ops
   * @param pointered the receiver, or {@code null} to return {@code ops} unchanged
   * @param <T> the data type
   * @return ops carrying the receiver
   */
  static <T> DynamicOps<T> withPointered(final DynamicOps<T> ops, final @Nullable Pointered pointered) {
    if (pointered == null) {
      return ops;
    } else if (ops instanceof RegistryOps<T> registries) {
      return new Registry<>(registries, registries.getterProvider, pointered);
    } else {
      return new Delegating<>(ops, pointered);
    }
  }

  final class Delegating<T> extends DelegatingOps<T> implements PointeredOps {
    private final Pointered pointered;

    Delegating(final DynamicOps<T> delegate, final Pointered pointered) {
      super(delegate);
      this.pointered = pointered;
    }

    @Override
    public Pointered pointered() {
      return this.pointered;
    }
  }

  final class Registry<T> extends RegistryOps<T> implements PointeredOps {
    private final Pointered pointered;

    Registry(final DynamicOps<T> parent, final HolderGetter.Provider getters, final Pointered pointered) {
      super(parent, getters);
      this.pointered = pointered;
    }

    @Override
    public Pointered pointered() {
      return this.pointered;
    }

    @Override
    public <U> RegistryOps<U> withParent(final DynamicOps<U> parent) {
      return new Registry<>(parent, this.getterProvider, this.pointered);
    }
  }
}
