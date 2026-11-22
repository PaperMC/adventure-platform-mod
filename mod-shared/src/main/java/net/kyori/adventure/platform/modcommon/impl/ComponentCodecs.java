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
package net.kyori.adventure.platform.modcommon.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Locale;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

/**
 * Common codec storage place.
 *
 * <p>These should not be referenced directly.</p>
 */
public final class ComponentCodecs {

  private ComponentCodecs() {
  }

  public static Codec<Component> unwrappingToNative(final Codec<Component> original) {
    return new Codec<Component>() {
      @Override
      public <T> DataResult<T> encode(final Component input, final DynamicOps<T> ops, final T prefix) {
        final @Nullable Pointered recipient = PacketRecipient.renderingDisabled() ? null : PointeredOps.pointered(ops);
        if (input instanceof WrappedComponent wrapped) {
          final WrappedComponent rendered = recipient == null ? wrapped : wrapped.rendered(recipient);
          if (rendered.deepConvertedIfPresent() != null) { // already converted
            return original.encode(rendered.deepConvertedIfPresent(), ops, prefix);
          }

          return AdventureCodecs.COMPONENT_CODEC.encode(rendered.wrapped(), ops, prefix);
        }

        if (recipient != null && input.getContents() instanceof TranslatableContents && AdventureCommon.hasAnyTranslations()) {
          final Locale locale = AdventureCommon.localePartition().apply(recipient);
          return original.encodeStart(ops, input)
            .flatMap(encoded -> AdventureCodecs.COMPONENT_CODEC.parse(ops, encoded))
            .flatMap(component -> AdventureCodecs.COMPONENT_CODEC.encode(GlobalTranslator.render(component, locale), ops, prefix));
        }

        return original.encode(input, ops, prefix);
      }

      @Override
      public <T> DataResult<Pair<Component, T>> decode(final DynamicOps<T> ops, final T input) {
        return original.decode(ops, input);
      }
    }.withLifecycle(Lifecycle.stable());
  }
}
