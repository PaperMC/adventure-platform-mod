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

import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;

import static net.kyori.adventure.text.Component.text;

final class MessageTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    session.test("system message", () -> {
      session.player(player -> player.sendMessage(text("a system message")));
      session.awaitSystemMessage("a system message");
    });

    session.test("action bar", () -> {
      session.player(player -> player.sendActionBar(text("an action bar")));
      session.awaitActionBar("an action bar");
    });

    session.test("chat with a chat type", () -> {
      session.player(player -> player.sendMessage(text("a chat message"), ChatType.CHAT.bind(text("a sender"))));
      session.await("chat message", r -> r.any(m -> m.kind() == ReceivedMessages.Kind.CHAT && m.text().contains("a chat message")));
    });

    session.test("broadcast to all players", () -> {
      session.serverPlayer(player -> MinecraftServerAudiences.of(player.level().getServer()).all().sendMessage(text("a broadcast")));
      session.awaitSystemMessage("a broadcast");
    });
  }
}
