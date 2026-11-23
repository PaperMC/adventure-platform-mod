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

import net.kyori.adventure.platform.test.fabric.gametest.AdventureGameTestMod;

final class CommandTests implements ClientTestGroup {
  private static final String TAG = "adventure_gametest";

  @Override
  public void run(final WorldTestSession session) {
    session.test("player command", () -> {
      session.command("adventure_gametest action_bar");
      session.awaitActionBar(AdventureGameTestMod.ACTION_BAR);
    });

    // execute as keeps the player's command source, which must still resolve to the player's audience
    session.test("command run as another entity", () -> {
      session.serverCommand("summon minecraft:armor_stand 0 200 0 {NoGravity:1b,Tags:[\"" + TAG + "\"]}");
      try {
        session.command("execute as @e[tag=" + TAG + ",limit=1] run adventure_gametest action_bar");
        session.awaitActionBar(AdventureGameTestMod.ACTION_BAR);
      } finally {
        session.serverCommand("kill @e[tag=" + TAG + "]");
      }
    });

    // The argument type also has to reach the client, or the client can't parse the command
    session.test("MiniMessage argument", () -> {
      session.command("adventure_gametest echo <red>an echoed message");
      session.awaitSystemMessage("an echoed message");
    });
  }
}
