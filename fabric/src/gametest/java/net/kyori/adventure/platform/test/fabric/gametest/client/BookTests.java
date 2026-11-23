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

import java.util.Collections;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.platform.test.fabric.gametest.mixin.BookViewScreenAccess;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static net.kyori.adventure.text.Component.text;

final class BookTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    // The book is only put in the held slot for the client, which then gets back what the player was holding
    session.test("opens with every page and gives back the held item", () -> {
      session.serverPlayer(player -> player.getInventory().setItem(player.getInventory().getSelectedSlot(), new ItemStack(Items.DIAMOND, 3)));
      try {
        // Otherwise the normal inventory sync would deliver it after the book, hiding a missing restore
        session.await("the held item to reach the client", () -> session.computeOnClient(client -> client.player.getInventory().getSelectedItem().is(Items.DIAMOND)));
        session.player(player -> player.openBook(Book.book(text("title"), text("author"), Collections.nCopies(3, text("a page")))));
        session.await("the book screen", () -> session.computeOnClient(client -> client.gui.screen() instanceof BookViewScreen));

        final int pages = session.computeOnClient(client -> ((BookViewScreenAccess) client.gui.screen()).gametest$bookAccess().getPageCount());
        if (pages != 3) {
          throw new AssertionError("The book screen showed " + pages + " pages");
        }
        session.sync();
        final ItemStack held = session.computeOnClient(client -> client.player.getInventory().getSelectedItem().copy());
        if (!ItemStack.matches(held, new ItemStack(Items.DIAMOND, 3))) {
          throw new AssertionError("The client was left holding " + held);
        }
      } finally {
        session.serverPlayer(player -> player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY));
      }
    });
  }
}
