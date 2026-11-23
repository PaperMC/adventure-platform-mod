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
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Dialogs;

final class DialogTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    // Dialogs can only come from Minecraft, here through a click event converted to Adventure
    session.test("dialog opens", () -> {
      session.serverPlayer(player -> {
        final var server = player.level().getServer();
        final var dialog = server.registryAccess().lookupOrThrow(Registries.DIALOG).getOrThrow(Dialogs.SERVER_LINKS);
        final var clickEvent = MinecraftServerAudiences.of(server)
          .asAdventure(Component.empty().withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent.ShowDialog(dialog))))
          .clickEvent();
        MinecraftServerAudiences.of(server).audience(player).showDialog(((ClickEvent.Payload.Dialog) clickEvent.payload()).dialog());
      });
      session.await("the dialog screen", () -> session.computeOnClient(client -> client.gui.screen() instanceof DialogScreen<?>));
    });
  }
}
