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
package net.kyori.adventure.platform.modcommon.impl;

import net.kyori.adventure.dialog.DialogLike;
import net.minecraft.core.Holder;
import net.minecraft.server.dialog.Dialog;

/**
 * An opaque {@link DialogLike} wrapping a Minecraft dialog, so dialog click events survive conversion.
 *
 * @param holder the Minecraft dialog
 */
public record VanillaDialog(Holder<Dialog> holder) implements DialogLike {
  /**
   * Get the Minecraft dialog of a dialog-like.
   *
   * @param dialog the dialog-like
   * @return the Minecraft dialog
   * @throws IllegalArgumentException if the dialog is not a {@link VanillaDialog}
   */
  public static Holder<Dialog> unwrap(final DialogLike dialog) {
    if (dialog instanceof VanillaDialog(final Holder<Dialog> holder)) {
      return holder;
    }
    throw new IllegalArgumentException("Only dialogs converted from Minecraft are supported, got " + dialog);
  }
}
