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

import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Runs every client test group against one dedicated server and one connection.
 */
public final class AdventureClientGameTest implements FabricClientGameTest {
  private static final List<TitleScreenTestGroup> TITLE_SCREEN_GROUPS = List.of(
    new TitleScreenTests()
  );
  private static final List<ClientTestGroup> GROUPS = List.of(
    new MessageTests(),
    new TitleTests(),
    new RenderingTests(),
    new CommandTests(),
    new PointerTests(),
    new BookTests(),
    new TabListTests(),
    new BossBarTests(),
    new SoundTests(),
    new ClientAudienceTests(),
    new ResourcePackTests(),
    new DialogTests(),
    new ClickCallbackTests()
  );

  @Override
  public void runTest(final ClientGameTestContext context) {
    // Mixins only apply when their target is loaded, so load every target to catch ones that fail to apply
    context.runOnClient(client -> MixinEnvironment.getCurrentEnvironment().audit());

    final ClientTestSession session = new ClientTestSession(context);
    for (final TitleScreenTestGroup group : TITLE_SCREEN_GROUPS) {
      session.group(group.getClass().getSimpleName());
      group.run(session);
    }

    final ReceivedMessages received = ReceivedMessages.register();
    try (
      final TestDedicatedServerContext server = context.worldBuilder().createServer();
      final TestDedicatedServerConnection connection = server.connect()
    ) {
      connection.waitForChunksRender();
      final WorldTestSession world = new WorldTestSession(session, server, connection, received);
      world.prepareServer();
      for (final ClientTestGroup group : GROUPS) {
        world.group(group.getClass().getSimpleName());
        group.run(world);
      }
    }
    session.assertNoFailures();
  }
}
