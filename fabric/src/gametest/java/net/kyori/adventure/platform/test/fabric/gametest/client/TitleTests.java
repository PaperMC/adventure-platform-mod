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

import net.kyori.adventure.title.Title;

import static net.kyori.adventure.platform.test.fabric.gametest.client.ReceivedMessages.Kind.CLEAR_TITLES;
import static net.kyori.adventure.platform.test.fabric.gametest.client.ReceivedMessages.Kind.RESET_TITLES;
import static net.kyori.adventure.platform.test.fabric.gametest.client.ReceivedMessages.Kind.SUBTITLE;
import static net.kyori.adventure.platform.test.fabric.gametest.client.ReceivedMessages.Kind.TITLE;
import static net.kyori.adventure.text.Component.text;

final class TitleTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    session.test("title and subtitle", () -> {
      session.player(player -> player.showTitle(Title.title(text("a title"), text("a subtitle"))));
      session.await("title and subtitle", r -> r.contains(TITLE, "a title") && r.contains(SUBTITLE, "a subtitle"));
    });

    session.test("clear", () -> {
      session.player(player -> player.clearTitle());
      session.await("cleared titles", r -> r.contains(CLEAR_TITLES, ""));
    });

    session.test("reset", () -> {
      session.player(player -> player.resetTitle());
      session.await("reset titles", r -> r.contains(RESET_TITLES, ""));
    });
  }
}
