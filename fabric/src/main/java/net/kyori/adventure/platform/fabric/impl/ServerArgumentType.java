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
package net.kyori.adventure.platform.fabric.impl;

import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Function;
import net.minecraft.commands.arguments.SerializableArgument;
import net.minecraft.commands.suggestions.CustomSuggestion;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * An argument type that only needs to be known on the server.
 *
 * @param id argument id
 * @param type serializable argument type
 * @param fallbackProvider fallback type provider
 * @param fallbackSuggestions fallback suggestions provider
 * @param <A> argument type
 */
public record ServerArgumentType<A extends SerializableArgument<?>>(
  Identifier id,
  SerializableArgument.Type<A> type,
  Function<A, ArgumentType<?>> fallbackProvider,
  @Nullable CustomSuggestion fallbackSuggestions
) {
}
