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

import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.platform.test.fabric.gametest.AdventureGameTestMod;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

final class RenderingTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    // Server-side translations are rendered when the packet is encoded for the receiving player
    session.test("server-side translation", () -> {
      session.player(player -> player.sendMessage(translatable(AdventureGameTestMod.TRANSLATION_KEY)));
      session.awaitSystemMessage(AdventureGameTestMod.TRANSLATED);
    });

    session.test("custom renderer", () -> {
      session.serverPlayer(player -> MinecraftServerAudiences.builder(player.level().getServer())
        .componentRenderer((component, viewer) -> text("rendered by a custom renderer"))
        .build()
        .player(player.getUUID())
        .sendMessage(text("not rendered")));
      session.awaitSystemMessage("rendered by a custom renderer");
    });
  }
}
