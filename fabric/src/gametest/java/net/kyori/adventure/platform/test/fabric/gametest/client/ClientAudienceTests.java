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

import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.platform.test.fabric.gametest.mixin.BossHealthOverlayAccess;
import net.kyori.adventure.platform.test.fabric.gametest.mixin.ChatComponentAccess;
import net.kyori.adventure.platform.test.fabric.gametest.mixin.HudAccess;
import net.kyori.adventure.platform.test.fabric.gametest.mixin.PlayerTabOverlayAccess;
import net.kyori.adventure.title.Title;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

// The client's own audience, shown directly on the client without the server
final class ClientAudienceTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    session.test("message", () -> {
      clientAudience(session, audience -> audience.sendMessage(text("a client-side message")));
      session.await("the message in chat", () -> session.computeOnClient(client -> ((ChatComponentAccess) client.gui.hud.getChat()).gametest$allMessages().stream()
        .anyMatch(message -> message.content().getString().equals("a client-side message"))));
    });

    session.test("title", () -> {
      clientAudience(session, audience -> audience.showTitle(Title.title(text("a client-side title"), empty())));
      session.await("the title", () -> session.computeOnClient(client -> {
        final var title = ((HudAccess) client.gui.hud).gametest$title();
        return title != null && title.getString().equals("a client-side title");
      }));
    });

    session.test("boss bar", () -> {
      final BossBar bar = BossBar.bossBar(text("a client-side boss bar"), 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
      try {
        clientAudience(session, audience -> audience.showBossBar(bar));
        session.await("the boss bar", () -> bossBars(session).equals(List.of("a client-side boss bar")));
      } finally {
        clientAudience(session, audience -> audience.hideBossBar(bar));
      }
      session.await("the boss bar to be hidden", () -> bossBars(session).isEmpty());
    });

    session.test("tab list header and footer", () -> {
      try {
        clientAudience(session, audience -> audience.sendPlayerListHeaderAndFooter(text("a client-side header"), text("a client-side footer")));
        session.await("the tab list", () -> session.computeOnClient(client -> {
          final PlayerTabOverlayAccess tabList = (PlayerTabOverlayAccess) client.gui.hud.getTabList();
          return tabList.gametest$header() != null && tabList.gametest$header().getString().equals("a client-side header")
            && tabList.gametest$footer() != null && tabList.gametest$footer().getString().equals("a client-side footer");
        }));
      } finally {
        clientAudience(session, audience -> audience.sendPlayerListHeaderAndFooter(empty(), empty()));
      }
    });

    session.test("book", () -> {
      clientAudience(session, audience -> audience.openBook(Book.book(text("title"), text("author"), List.of(text("a page")))));
      session.await("the book screen", () -> session.computeOnClient(client -> client.gui.screen() instanceof BookViewScreen));
    });
  }

  private static void clientAudience(final WorldTestSession session, final Consumer<Audience> action) {
    session.client(client -> action.accept(MinecraftClientAudiences.of().audience()));
  }

  private static List<String> bossBars(final WorldTestSession session) {
    return session.computeOnClient(client -> ((BossHealthOverlayAccess) client.gui.hud.getBossOverlay()).gametest$events().values().stream()
      .map(event -> event.getName().getString())
      .toList());
  }
}
