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

import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.client.gui.screens.TitleScreen;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

final class TitleScreenTests implements TitleScreenTestGroup {
  @Override
  public void run(final ClientTestSession session) {
    // Mods use the client audiences before any world exists, such as for title screen buttons
    session.test("client audiences convert components", () -> {
      final String converted = session.computeOnClient(client -> {
        if (!(client.gui.screen() instanceof TitleScreen)) {
          throw new AssertionError("Expected to be on the title screen, was on " + client.gui.screen());
        }
        return MinecraftClientAudiences.of().asNative(translatable("menu.singleplayer", NamedTextColor.LIGHT_PURPLE)).getString();
      });
      if (!converted.equals("Singleplayer")) {
        throw new AssertionError("Converted to '" + converted + "'");
      }
    });

    // Open-file click events never go over the wire, so they are only converted locally
    session.test("open-file click event converts", () -> {
      final var clickEvent = session.computeOnClient(client -> MinecraftClientAudiences.of().asNative(text("a file").clickEvent(ClickEvent.openFile("some/file.txt"))).getStyle().getClickEvent());
      if (!(clickEvent instanceof net.minecraft.network.chat.ClickEvent.OpenFile(final String path) && path.equals("some/file.txt"))) {
        throw new AssertionError("Converted to " + clickEvent);
      }
    });
  }
}
