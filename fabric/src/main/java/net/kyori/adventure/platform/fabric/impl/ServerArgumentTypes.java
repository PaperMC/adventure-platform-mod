/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2022-2026 KyoriPowered
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
package net.kyori.adventure.platform.fabric.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.kyori.adventure.platform.fabric.impl.server.ServerPlayerBridge;
import net.minecraft.commands.arguments.SerializableArgument;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ServerArgumentTypes {
  private static final Map<Identifier, ServerArgumentType<?>> BY_LOCATION = new ConcurrentHashMap<>();
  private static final Map<SerializableArgument.Type<?>, ServerArgumentType<?>> BY_TYPE = new IdentityHashMap<>();
  private static final Int2ObjectMap<ServerArgumentType<?>> BY_ID = new Int2ObjectArrayMap<>();
  private static final Reference2IntMap<SerializableArgument.Type<?>> IDS_BY_TYPE = new Reference2IntOpenHashMap<>();
  private static final AtomicInteger ID_COUNTER = new AtomicInteger(100000); // start at 100,000. hopefully nobody registers that many types.
  private static @Nullable Int2ObjectMap<ServerArgumentType<?>> BY_ID_ACTIVE_SERVER = null;

  static {
    IDS_BY_TYPE.defaultReturnValue(-1);
  }

  private ServerArgumentTypes() {
  }

  public static void register(final ServerArgumentType<?> type) {
    final int id = ID_COUNTER.getAndIncrement();
    BY_ID.put(id, type);
    IDS_BY_TYPE.put(type.type(), id);
    BY_TYPE.put(type.type(), type);
    BY_LOCATION.put(type.id(), type);
  }

  public static Set<Identifier> ids() {
    return Collections.unmodifiableSet(BY_LOCATION.keySet());
  }

  @SuppressWarnings("unchecked")
  public static <A extends SerializableArgument<?>> @Nullable ServerArgumentType<A> serverType(final A argument) {
    return (ServerArgumentType<A>) BY_TYPE.get(argument.type());
  }

  private static @Nullable ServerArgumentType<?> byId(final int id) {
    return Objects.requireNonNullElse(BY_ID_ACTIVE_SERVER, BY_ID).get(id);
  }

  /**
   * Wrap the registry name codec for argument types to also handle server argument types.
   *
   * @param original the registry codec
   * @return the wrapped codec
   */
  public static Codec<SerializableArgument.Type<?>> wrapNameCodec(final Codec<SerializableArgument.Type<?>> original) {
    return new Codec<>() {
      @Override
      public <T> DataResult<T> encode(final SerializableArgument.Type<?> input, final DynamicOps<T> ops, final T prefix) {
        final ServerArgumentType<?> serverType = BY_TYPE.get(input);
        if (serverType != null) {
          return Identifier.CODEC.encode(serverType.id(), ops, prefix);
        }
        return original.encode(input, ops, prefix);
      }

      @Override
      public <T> DataResult<Pair<SerializableArgument.Type<?>, T>> decode(final DynamicOps<T> ops, final T input) {
        final DataResult<Pair<Identifier, T>> id = Identifier.CODEC.decode(ops, input);
        final ServerArgumentType<?> serverType = id.result().map(pair -> BY_LOCATION.get(pair.getFirst())).orElse(null);
        if (serverType != null) {
          return id.map(pair -> pair.mapFirst(ignored -> serverType.type()));
        }
        return original.decode(ops, input);
      }

      @Override
      public String toString() {
        return "ServerArgumentTypes[" + original + "]";
      }
    };
  }

  /**
   * Wrap the registry stream codec for argument types to write server argument types with their own network ids.
   *
   * @param original the registry stream codec
   * @return the wrapped stream codec
   */
  public static StreamCodec<RegistryFriendlyByteBuf, SerializableArgument.Type<?>> wrapStreamCodec(final StreamCodec<RegistryFriendlyByteBuf, SerializableArgument.Type<?>> original) {
    return new StreamCodec<>() {
      @Override
      public SerializableArgument.Type<?> decode(final RegistryFriendlyByteBuf buf) {
        final int readerIndex = buf.readerIndex();
        final ServerArgumentType<?> serverType = byId(VarInt.read(buf));
        if (serverType != null) {
          return serverType.type();
        }
        buf.readerIndex(readerIndex);
        return original.decode(buf);
      }

      @Override
      public void encode(final RegistryFriendlyByteBuf buf, final SerializableArgument.Type<?> value) {
        final int id = IDS_BY_TYPE.getInt(value);
        if (id != -1) {
          VarInt.write(buf, id);
        } else {
          original.encode(buf, value);
        }
      }
    };
  }

  public static void knownArgumentTypes(final ServerPlayer player, final Set<Identifier> ids, final PacketSender responder) {
    ((ServerPlayerBridge) player).bridge$knownArguments(ids);
    sendMappings(player, responder);
    if (!ids.isEmpty()) { // TODO: Avoid resending the whole command tree, find a way to receive the packet before sending?
      player.level().getServer().getCommands().sendCommands(player);
    }
  }

  public static Set<Identifier> knownArgumentTypes(final ServerPlayer player) {
    return ((ServerPlayerBridge) player).bridge$knownArguments();
  }

  private static void sendMappings(final ServerPlayer player, final PacketSender responder) {
    final Set<Identifier> known = knownArgumentTypes(player);
    final Int2ObjectMap<Identifier> map = new Int2ObjectArrayMap<>();
    for (final Identifier resourceLocation : known) {
      final ServerArgumentType<?> type = BY_LOCATION.get(resourceLocation);
      if (type != null) {
        map.put(IDS_BY_TYPE.getInt(type.type()), type.id());
      }
    }
    new ClientboundArgumentTypeMappingsPacket(map).sendTo(responder);
  }

  public static void receiveMappings(final ClientboundArgumentTypeMappingsPacket packet) {
    final Int2ObjectMap<ServerArgumentType<?>> map = new Int2ObjectArrayMap<>();
    for (final Int2ObjectMap.Entry<Identifier> entry : packet.mappings().int2ObjectEntrySet()) {
      map.put(entry.getIntKey(), BY_LOCATION.get(entry.getValue()));
    }
    BY_ID_ACTIVE_SERVER = map;
  }
}
