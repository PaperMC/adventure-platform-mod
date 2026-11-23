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

import net.kyori.adventure.platform.test.fabric.gametest.mixin.PlayerTabOverlayAccess;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

final class TabListTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    session.test("header and footer", () -> {
      try {
        session.player(player -> player.sendPlayerListHeaderAndFooter(text("a header"), text("a footer")));
        this.awaitTabList(session, "a header", "a footer");
      } finally {
        session.player(player -> player.sendPlayerListHeaderAndFooter(empty(), empty()));
      }
    });

    // Respawning replaces the ServerPlayer, but the client keeps showing the header it was sent
    session.test("header is kept when only the footer changes after respawning", () -> {
      try {
        session.player(player -> player.sendPlayerListHeader(text("a header")));
        session.respawn();
        session.player(player -> player.sendPlayerListFooter(text("a footer")));
        this.awaitTabList(session, "a header", "a footer");
      } finally {
        session.player(player -> player.sendPlayerListHeaderAndFooter(empty(), empty()));
      }
    });
  }

  private void awaitTabList(final WorldTestSession session, final String header, final String footer) {
    session.await("tab list header '" + header + "' and footer '" + footer + "'", () -> session.computeOnClient(client -> {
      final PlayerTabOverlayAccess tabList = (PlayerTabOverlayAccess) client.gui.hud.getTabList();
      return tabList.gametest$header() != null && tabList.gametest$header().getString().equals(header)
        && tabList.gametest$footer() != null && tabList.gametest$footer().getString().equals(footer);
    }));
  }
}
