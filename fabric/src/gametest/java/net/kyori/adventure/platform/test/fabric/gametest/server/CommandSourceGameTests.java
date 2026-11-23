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
package net.kyori.adventure.platform.test.fabric.gametest.server;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

import static net.kyori.adventure.text.Component.text;

public class CommandSourceGameTests {
  @GameTest
  public void rconReceivesMessages(final GameTestHelper helper) {
    final RconConsoleSource rcon = new RconConsoleSource(helper.getLevel().getServer());
    MinecraftServerAudiences.of(helper.getLevel().getServer()).audience(rcon).sendMessage(text("a message"));

    helper.assertValueEqual(rcon.getCommandResponse(), "a message", "rcon output");
    helper.succeed();
  }

  // Commands reply through the command block's source, which keeps the last output
  @GameTest
  public void commandBlockReceivesMessages(final GameTestHelper helper) {
    final BlockPos pos = new BlockPos(0, 1, 0);
    helper.setBlock(pos, Blocks.COMMAND_BLOCK);
    final BaseCommandBlock commandBlock = helper.<CommandBlockEntity>getBlockEntity(pos, CommandBlockEntity.class).getCommandBlock();
    commandBlock.setCommand("adventure_gametest echo a message");
    commandBlock.performCommand(helper.getLevel());

    final String output = commandBlock.getLastOutput().getString();
    helper.assertTrue(output.endsWith("a message"), "Expected the command block's output to end with 'a message', was '" + output + "'");
    helper.succeed();
  }
}
