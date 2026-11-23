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
package net.kyori.adventure.platform.test.fabric.gametest;

import java.text.MessageFormat;
import java.util.Locale;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;

import static net.kyori.adventure.platform.modcommon.ComponentArgumentType.component;
import static net.kyori.adventure.platform.modcommon.ComponentArgumentType.miniMessage;
import static net.kyori.adventure.text.Component.text;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Server-side setup shared by the game tests.
 */
public class AdventureGameTestMod implements ModInitializer {
  public static final String TRANSLATION_KEY = "adventure.gametest.greeting";
  public static final String TRANSLATED = "Hello from a server-side translation";
  public static final String ACTION_BAR = "action bar from a command";

  @Override
  public void onInitialize() {
    final TranslationStore.StringBased<MessageFormat> translations = TranslationStore.messageFormat(Key.key("adventure", "gametest"));
    translations.register(TRANSLATION_KEY, Locale.US, new MessageFormat(TRANSLATED, Locale.US));
    GlobalTranslator.translator().addSource(translations);

    CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> {
      dispatcher.register(literal("adventure_gametest")
        .then(literal("action_bar").executes(ctx -> {
          MinecraftServerAudiences.of(ctx.getSource().getServer()).audience(ctx.getSource()).sendActionBar(text(ACTION_BAR));
          return 1;
        }))
        .then(literal("echo").then(argument("message", miniMessage()).executes(ctx -> {
          MinecraftServerAudiences.of(ctx.getSource().getServer()).audience(ctx.getSource()).sendMessage(component(ctx, "message"));
          return 1;
        }))));
    });
  }
}
