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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.test.fabric.gametest.mixin.BossHealthOverlayAccess;

import static net.kyori.adventure.text.Component.text;

final class BossBarTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    session.test("shown, updated and hidden", () -> {
      final BossBar bar = bar();
      try {
        session.player(player -> player.showBossBar(bar));
        this.awaitBars(session, List.of("a boss bar"));
        bar.name(text("a renamed boss bar"));
        this.awaitBars(session, List.of("a renamed boss bar"));
        session.player(player -> player.hideBossBar(bar));
        this.awaitBars(session, List.of());
      } finally {
        session.player(player -> player.hideBossBar(bar));
      }
    });
  }

  private static BossBar bar() {
    return BossBar.bossBar(text("a boss bar"), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
  }

  private void awaitBars(final WorldTestSession session, final List<String> names) {
    session.await("boss bars " + names, () -> session.computeOnClient(client -> ((BossHealthOverlayAccess) client.gui.hud.getBossOverlay()).gametest$events().values().stream()
      .map(event -> event.getName().getString())
      .toList()
      .equals(names)));
  }
}
