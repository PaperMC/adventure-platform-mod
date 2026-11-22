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
package net.kyori.adventure.platform.fabric.impl.compat.permissions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.CollectPointersCallback;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.util.TriState;

public class PermissionsApiIntegration implements ModInitializer {
  private static final ComponentLogger LOGGER = ComponentLogger.logger();
  private static final String PERMISSIONS_API_MOD_NAME = "fabric-permissions-api-v0";

  @Override
  public void onInitialize() {
    if (FabricLoader.getInstance().isModLoaded(PERMISSIONS_API_MOD_NAME)) {
      this.registerPermissionHook();
      LOGGER.debug("Registered fabric-permissions-api hook");
    } else {
      LOGGER.debug("fabric-permissions-api not detected, the PermissionChecker pointer will not be present");
    }
  }

  private void registerPermissionHook() {
    CollectPointersCallback.EVENT.register((pointered, consumer) -> {
      // TODO(26.4): restore command source permission checks once fabric-permissions-api supports 26.4
      //  (SharedSuggestionProvider no longer exists)
    });
  }

  private static TriState adaptTristate(final net.fabricmc.fabric.api.util.TriState fabricState) {
    return switch (fabricState) {
      case FALSE -> TriState.FALSE;
      case DEFAULT -> TriState.NOT_SET;
      case TRUE -> TriState.TRUE;
    };
  }
}
