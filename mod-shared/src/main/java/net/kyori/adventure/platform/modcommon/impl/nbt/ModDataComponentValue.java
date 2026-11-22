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
package net.kyori.adventure.platform.modcommon.impl.nbt;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public sealed interface ModDataComponentValue extends DataComponentValue {
  net.kyori.adventure.util.Codec<Tag, String, CommandSyntaxException, RuntimeException> SNBT_CODEC = net.kyori.adventure.util.Codec.codec(
    TagParser.create(NbtOps.INSTANCE)::parseFully,
    Tag::toString
  );

  /**
   * Create a show item hover event for an item.
   *
   * @param item the item type
   * @param count the item count
   * @param patch the item's data component patch
   * @param registries the registries the item's data components belong to
   * @return the hover event
   */
  static HoverEvent<HoverEvent.ShowItem> showItem(final Holder<Item> item, final int count, final DataComponentPatch patch, final RegistryOps<?> registries) {
    final Key itemType = MinecraftAudiences.asAdventure(item.unwrapKey().orElseThrow().identifier());
    return HoverEvent.showItem(itemType, count, asAdventure(patch, registries));
  }

  /**
   * Convert a Minecraft component patch to Adventure data component values.
   *
   * @param patch the patch
   * @param registries the registries the patch's values belong to
   * @return the data component values
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  static Map<Key, DataComponentValue> asAdventure(final DataComponentPatch patch, final RegistryOps<?> registries) {
    if (patch.isEmpty()) {
      return Map.of();
    }
    final Map<Key, DataComponentValue> components = new HashMap<>();
    final DataComponentPatch.SplitResult split = patch.split();
    for (final DataComponentType<?> type : split.removed()) {
      if (type.isTransient()) continue;

      components.put(MinecraftAudiences.asAdventure(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)), Removed.INSTANCE);
    }
    for (final TypedDataComponent<?> entry : split.added()) {
      if (entry.type().isTransient()) continue;

      components.put(MinecraftAudiences.asAdventure(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.type())), new Present(entry.value(), entry.type().codecOrThrow(), registries));
    }
    return components;
  }

  /**
   * Convert Adventure data component values to a Minecraft component patch.
   *
   * @param map the data component values
   * @param registries the registries to resolve values against
   * @return the patch
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  static DataResult<DataComponentPatch> asPatch(final Map<? extends Key, ? extends DataComponentValue> map, final RegistryOps<?> registries) {
    final DataComponentPatch.Builder builder = DataComponentPatch.builder();
    for (final Map.Entry<? extends Key, ? extends DataComponentValue> entry : map.entrySet()) {
      final Key key = entry.getKey();
      final DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(MinecraftAudiences.asNative(key));
      if (type == null) {
        return DataResult.error(() -> "Unknown data component type " + key.asString());
      }

      final DataResult<?> value = switch (entry.getValue()) {
        case final DataComponentValue.Removed removed -> null;
        case final Present<?> present -> DataResult.success(present.value());
        case final GsonDataComponentValue gson -> type.codecOrThrow().parse(registries.withParent(JsonOps.INSTANCE), gson.element());
        case final DataComponentValue.TagSerializable tag -> parseSnbt(tag.asBinaryTag()).flatMap(nbt -> type.codecOrThrow().parse(registries.withParent(NbtOps.INSTANCE), nbt));
        default -> DataResult.error(() -> "Unsupported data component value " + entry.getValue());
      };
      if (value == null) {
        builder.remove(type);
      } else if (value.error().isPresent()) {
        return DataResult.error(() -> "Failed to convert data component " + key.asString() + ": " + value.error().get().message());
      } else {
        builder.set(type, value.getOrThrow());
      }
    }
    return DataResult.success(builder.build());
  }

  private static DataResult<Tag> parseSnbt(final BinaryTagHolder holder) {
    try {
      return DataResult.success(holder.get(SNBT_CODEC));
    } catch (final CommandSyntaxException ex) {
      return DataResult.error(ex::getMessage);
    }
  }

  /**
   * A data component value from Minecraft, along with the registries it belongs to.
   *
   * @param value the value
   * @param codec the value's codec
   * @param registries the registries the value belongs to
   * @param <T> the value type
   */
  record Present<T>(@NotNull T value, @NotNull Codec<T> codec, @NotNull RegistryOps<?> registries) implements ModDataComponentValue, DataComponentValue.TagSerializable {
    @Override
    public @NotNull BinaryTagHolder asBinaryTag() {
      return BinaryTagHolder.encode(
        this.codec.encodeStart(this.registries.withParent(NbtOps.INSTANCE), this.value).getOrThrow(IllegalArgumentException::new),
        SNBT_CODEC
      );
    }

    /**
     * Encode this value as JSON.
     *
     * @return the JSON value
     */
    public @NotNull JsonElement asJson() {
      return this.codec.encodeStart(this.registries.withParent(JsonOps.INSTANCE), this.value).getOrThrow(IllegalArgumentException::new);
    }
  }

  enum Removed implements ModDataComponentValue, DataComponentValue.Removed {
    INSTANCE;
  }
}
