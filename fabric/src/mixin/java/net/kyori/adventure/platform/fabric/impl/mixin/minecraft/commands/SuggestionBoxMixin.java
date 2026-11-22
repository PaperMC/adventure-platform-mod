/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2023-2026 KyoriPowered
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
package net.kyori.adventure.platform.fabric.impl.mixin.minecraft.commands;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Set;
import net.kyori.adventure.platform.fabric.impl.ServerArgumentType;
import net.kyori.adventure.platform.fabric.impl.ServerArgumentTypes;
import net.kyori.adventure.platform.fabric.impl.accessor.brigadier.builder.RequiredArgumentBuilderAccess;
import net.minecraft.commands.ServerPlayerSuggestionContext;
import net.minecraft.commands.SuggestionBox;
import net.minecraft.commands.arguments.SerializableArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SuggestionBox.class)
public abstract class SuggestionBoxMixin {
  @WrapOperation(
    method = "fillUsableCommands",
    at = @At(
      value = "INVOKE",
      target = "Lcom/mojang/brigadier/builder/ArgumentBuilder;build()Lcom/mojang/brigadier/tree/CommandNode;",
      remap = false
    )
  )
  private static <S> CommandNode<S> adventure$replaceArgumentType(
    final ArgumentBuilder<S, ?> instance,
    final Operation<CommandNode<S>> original,
    final @Local(argsOnly = true) Object commandFilter
  ) {
    if (!(instance instanceof RequiredArgumentBuilder<?, ?> builder)
      || !(commandFilter instanceof ServerPlayerSuggestionContext context)
      || !(context.entity() instanceof ServerPlayer player)) {
      return original.call(instance); // only replace argument types for required arguments being sent to players
    }

    final Set<Identifier> knownTypes = ServerArgumentTypes.knownArgumentTypes(player);
    // If we have a replacement and the arg type isn't known to the client, change the argument type
    // This is super un-typesafe, but as long as the returned CommandNode is only used for serialization we are fine.
    // Repeat as long as a type is replaceable -- that way you can have a hierarchy of argument types.
    ServerArgumentType<SerializableArgument<?>> type = adventure$serverType(builder.getType());
    while (type != null && !knownTypes.contains(type.id())) {
      final ArgumentType<?> fallback = type.fallbackProvider().apply(SerializableArgument.unwrapOrThrow(builder.getType()));
      ((RequiredArgumentBuilderAccess) builder).accessor$type(fallback);
      if (type.fallbackSuggestions() != null) {
        builder.suggests(type.fallbackSuggestions().toBrigadier());
      }
      type = adventure$serverType(fallback);
    }
    return original.call(instance);
  }

  private static ServerArgumentType<SerializableArgument<?>> adventure$serverType(final ArgumentType<?> type) {
    return type instanceof SerializableArgument.BrigadierWrapper<?> wrapper ? ServerArgumentTypes.serverType(wrapper.argument()) : null;
  }
}
