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

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;

final class ResourcePackTests implements ClientTestGroup {
  @Override
  public void run(final WorldTestSession session) {
    // The client rejects a URL it can't download from straight away, which answers the request without any network
    session.test("request is answered", () -> {
      final CompletableFuture<ResourcePackStatus> status = new CompletableFuture<>();
      session.player(player -> player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
        .packs(ResourcePackInfo.resourcePackInfo(UUID.randomUUID(), URI.create("ftp://adventure.invalid/pack.zip"), ""))
        .callback((id, packStatus, audience) -> status.complete(packStatus))));
      session.await("the client's answer", status::isDone);
      if (status.join() != ResourcePackStatus.INVALID_URL) {
        throw new AssertionError("The client answered " + status.join());
      }
    });
  }
}
