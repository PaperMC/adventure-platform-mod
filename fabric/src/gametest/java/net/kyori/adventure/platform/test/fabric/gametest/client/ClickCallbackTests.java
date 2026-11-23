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

import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;

import static net.kyori.adventure.text.Component.text;

final class ClickCallbackTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    session.test("callback runs when clicked", () -> {
      final AtomicBoolean clicked = new AtomicBoolean();
      session.player(player -> player.sendMessage(text("click me").clickEvent(ClickEvent.callback(audience -> clicked.set(true)))));
      session.awaitSystemMessage("click me");

      // Send what the client sends when the message is clicked
      final var received = session.received().find(ReceivedMessages.Kind.SYSTEM, "click me").orElseThrow();
      if (!(received.message().getStyle().getClickEvent() instanceof net.minecraft.network.chat.ClickEvent.Custom(final var id, final var payload))) {
        throw new AssertionError("The message arrived without a custom click action: " + received.message());
      }
      session.client(client -> client.getConnection().send(new ServerboundCustomClickActionPacket(id, payload)));
      session.await("the callback to run", clicked::get);
    });
  }
}
