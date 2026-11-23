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
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.identity.Identity;

final class PointerTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    session.test("player pointers", () -> {
      final List<Object> expected = session.computeOnClient(client -> List.of(client.getUser().getName(), client.player.getUUID(), Locale.US));
      final List<Object> actual = session.computeWithPlayer(player -> List.of(
        player.get(Identity.NAME).orElseThrow(),
        player.get(Identity.UUID).orElseThrow(),
        player.get(Identity.LOCALE).orElseThrow()
      ));
      if (!actual.equals(expected)) {
        throw new AssertionError("Expected name, UUID and locale " + expected + ", got " + actual);
      }
    });

    session.test("locale follows the client's language", () -> {
      try {
        this.changeLanguage(session, "de_de");
        session.awaitPlayer("a German locale", player -> player.get(Identity.LOCALE).equals(Optional.of(Locale.GERMANY)));
      } finally {
        this.changeLanguage(session, "en_us");
        session.awaitPlayer("the locale to be reset", player -> player.get(Identity.LOCALE).equals(Optional.of(Locale.US)));
      }
    });
  }

  // Only tells the server, without reloading the client's resources
  private void changeLanguage(final WorldTestSession session, final String language) {
    session.client(client -> {
      client.options.languageCode = language;
      client.options.broadcastOptions();
    });
  }
}
