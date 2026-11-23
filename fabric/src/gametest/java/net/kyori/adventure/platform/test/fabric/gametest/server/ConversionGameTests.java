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

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.ComponentMessageThrowable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;

import static net.kyori.adventure.text.Component.text;

public class ConversionGameTests {
  // cloud-minecraft-modded converts command exceptions this way, and casts the result back to an Exception
  @GameTest
  public void commandExceptionsExposeComponentMessages(final GameTestHelper helper) {
    final Component message = Component.literal("a command error").withStyle(ChatFormatting.RED);
    final CommandSyntaxException exception = new SimpleCommandExceptionType(message).create();
    final ComponentMessageThrowable converted = MinecraftServerAudiences.of(helper.getLevel().getServer()).asComponentThrowable(exception);

    helper.assertTrue(converted instanceof Exception, "The converted exception is not an Exception");
    helper.assertValueEqual(converted.componentMessage(), text("a command error", NamedTextColor.RED), "converted message");
    helper.succeed();
  }

  @GameTest
  public void entitiesAreHoverEventSources(final GameTestHelper helper) {
    final Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(1, 1, 1));
    final HoverEvent.ShowEntity shown = ((HoverEventSource<?>) pig).asHoverEvent().value() instanceof final HoverEvent.ShowEntity entity ? entity : null;

    helper.assertTrue(shown != null, "The hover event does not show an entity");
    helper.assertValueEqual(shown.type(), Key.key("minecraft", "pig"), "entity type");
    helper.assertValueEqual(shown.id(), pig.getUUID(), "entity UUID");
    helper.succeed();
  }
}
