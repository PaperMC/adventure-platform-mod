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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.resource.ResourcePackCallback;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.util.MonkeyBars;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;

/**
 * Resource pack packets shared by every server-side audience backed by a connection.
 */
public final class ServerResourcePacks {
  private ServerResourcePacks() {
  }

  /**
   * Create the packets for a resource pack request, registering its callback on the connection.
   *
   * @param request the request
   * @param controller the audiences instance converting the prompt and owning the callback
   * @param listener the connection's packet listener
   * @return the packets to send
   */
  public static List<Packet<ClientCommonPacketListener>> push(final ResourcePackRequest request, final MinecraftServerAudiencesImpl controller, final ServerCommonPacketListenerImplBridge listener) {
    final List<Packet<ClientCommonPacketListener>> packets = new ArrayList<>(request.packs().size() + 1);
    if (request.replace()) {
      packets.add(new ClientboundResourcePackPopPacket(Optional.empty()));
    }

    final Optional<net.minecraft.network.chat.Component> prompt = Optional.ofNullable(request.prompt()).map(controller::asNative);
    for (final Iterator<ResourcePackInfo> it = request.packs().iterator(); it.hasNext();) {
      final ResourcePackInfo pack = it.next();
      packets.add(new ClientboundResourcePackPushPacket(
        pack.id(),
        pack.uri().toASCIIString(),
        pack.hash(),
        request.required(),
        it.hasNext() ? Optional.empty() : prompt
      ));

      if (request.callback() != ResourcePackCallback.noOp()) {
        listener.adventure$bridge$registerPackCallback(pack.id(), controller, request.callback());
      }
    }
    return packets;
  }

  /**
   * Create the packets removing resource packs.
   *
   * @param id the first pack to remove
   * @param others other packs to remove
   * @return the packets to send
   */
  public static List<Packet<ClientCommonPacketListener>> pop(final UUID id, final UUID... others) {
    return MonkeyBars.nonEmptyArrayToList(pack -> new ClientboundResourcePackPopPacket(Optional.of(pack)), id, others);
  }

  /**
   * Create the packet removing all resource packs.
   *
   * @return the packet to send
   */
  public static Packet<ClientCommonPacketListener> clear() {
    return new ClientboundResourcePackPopPacket(Optional.empty());
  }
}
