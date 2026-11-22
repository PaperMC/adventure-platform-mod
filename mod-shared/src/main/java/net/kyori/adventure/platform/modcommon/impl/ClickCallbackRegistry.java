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
package net.kyori.adventure.platform.modcommon.impl;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
import net.kyori.adventure.platform.modcommon.impl.nbt.ModDataComponentValue;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

public final class ClickCallbackRegistry {
  public static final ClickCallbackRegistry INSTANCE = new ClickCallbackRegistry();

  public static final int CLEAN_UP_RATE = 30 /* seconds */;

  private static final Identifier CLICK_CALLBACK_ID = AdventureCommon.res("click_callback");
  private static final String ID_KEY = "id";

  private final Map<UUID, StoredCallback> callbacks = new ConcurrentHashMap<>();

  private ClickCallbackRegistry() {
  }

  /**
   * Register a callback, returning the click event that will trigger it.
   *
   * <p>Callbacks use custom click actions, which vanilla clients send to the server without any confirmation.</p>
   *
   * @param callback the callback to register
   * @param options options
   * @return a new click event
   */
  public ClickEvent<?> register(final ClickCallback<Audience> callback, final ClickCallback.Options options) {
    final UUID id = UUID.randomUUID();
    this.callbacks.put(id, new StoredCallback(callback, options));

    final CompoundTag payload = new CompoundTag();
    payload.store(ID_KEY, UUIDUtil.CODEC, id);
    return ClickEvent.custom(MinecraftAudiences.asAdventure(CLICK_CALLBACK_ID), BinaryTagHolder.encode(payload, ModDataComponentValue.SNBT_CODEC));
  }

  /**
   * Handle a custom click action, running the callback it refers to if there is a valid one.
   *
   * @param id the custom click action id
   * @param payload the custom click action payload
   * @param audience the audience that clicked
   */
  public void handleClick(final Identifier id, final Optional<Tag> payload, final Audience audience) {
    if (!CLICK_CALLBACK_ID.equals(id)) {
      return;
    }

    payload.flatMap(Tag::asCompound)
      .flatMap(tag -> tag.read(ID_KEY, UUIDUtil.CODEC))
      .map(this.callbacks::get)
      .filter(StoredCallback::valid)
      .ifPresent(callback -> {
        callback.takeUse();
        callback.callback.accept(audience);
      });
  }

  /**
   * Remove callbacks that have expired or have no remaining uses.
   */
  public void cleanUp() {
    this.callbacks.values().removeIf(callback -> !callback.valid());
  }

  private static final class StoredCallback {
    private final long startedAt = System.nanoTime();
    private final ClickCallback<Audience> callback;
    private final long lifetime;
    private volatile int remainingUses; // only written on the server thread, read during clean up

    StoredCallback(final ClickCallback<Audience> callback, final ClickCallback.Options options) {
      this.callback = callback;
      this.lifetime = saturatedNanos(options.lifetime());
      this.remainingUses = options.uses();
    }

    private static long saturatedNanos(final Duration duration) {
      try {
        return duration.toNanos();
      } catch (final ArithmeticException ex) {
        return Long.MAX_VALUE;
      }
    }

    void takeUse() {
      if (this.remainingUses != ClickCallback.UNLIMITED_USES) {
        this.remainingUses--;
      }
    }

    boolean hasRemainingUses() {
      return this.remainingUses == ClickCallback.UNLIMITED_USES || this.remainingUses > 0;
    }

    boolean expired() {
      if (this.lifetime == Long.MAX_VALUE) return false;
      return System.nanoTime() - this.startedAt >= this.lifetime;
    }

    boolean valid() {
      return this.hasRemainingUses() && !this.expired();
    }
  }
}
