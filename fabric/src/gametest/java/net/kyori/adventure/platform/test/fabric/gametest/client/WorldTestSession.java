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
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * A session connected to the shared dedicated server, with helpers for the player and what the client receives.
 */
public final class WorldTestSession extends ClientTestSession {
  private final TestDedicatedServerContext server;
  private final TestDedicatedServerConnection connection;
  private final ReceivedMessages received;
  private final UUID playerId;

  WorldTestSession(final ClientTestSession session, final TestDedicatedServerContext server, final TestDedicatedServerConnection connection, final ReceivedMessages received) {
    super(session);
    this.server = server;
    this.connection = connection;
    this.received = received;
    this.playerId = server.computeOnServer(s -> connection.getServerPlayer().getUUID());
  }

  /**
   * Run an action with the player's audience, on the server thread.
   *
   * @param action the action
   */
  public void player(final Consumer<Audience> action) {
    this.server.runOnServer(server -> action.accept(MinecraftServerAudiences.of(server).audience(this.serverPlayer(server))));
  }

  public <T> T computeWithPlayer(final Function<Audience, T> function) {
    return this.server.computeOnServer(server -> function.apply(MinecraftServerAudiences.of(server).audience(this.serverPlayer(server))));
  }

  /**
   * Wait until a condition on the player's audience holds, checked on the server thread.
   *
   * @param description what is being waited for
   * @param condition the condition
   */
  public void awaitPlayer(final String description, final Predicate<Audience> condition) {
    try {
      this.server.waitFor(server -> condition.test(MinecraftServerAudiences.of(server).audience(this.serverPlayer(server))));
    } catch (final AssertionError ex) {
      throw new AssertionError("Timed out waiting for " + description, ex);
    }
  }

  /**
   * Run an action with the current server player, on the server thread.
   *
   * @param action the action
   */
  public void serverPlayer(final Consumer<ServerPlayer> action) {
    this.server.runOnServer(server -> action.accept(this.serverPlayer(server)));
  }

  // Looked up each time, since respawning replaces the ServerPlayer
  private ServerPlayer serverPlayer(final MinecraftServer server) {
    return server.getPlayerList().getPlayer(this.playerId);
  }

  /**
   * Kill the player and wait until they've respawned, which replaces their ServerPlayer.
   */
  public void respawn() {
    final ServerPlayer before = this.server.computeOnServer(this::serverPlayer);
    this.serverCommand("kill " + before.getGameProfile().name());
    try {
      this.server.waitFor(server -> {
        final ServerPlayer now = this.serverPlayer(server);
        return now != null && now != before && now.isAlive();
      });
    } catch (final AssertionError ex) {
      throw new AssertionError("Timed out waiting for the player to respawn", ex);
    }
    this.sync();
  }

  public List<ReceivedSounds.Received> sounds() {
    return ReceivedSounds.snapshot();
  }

  /**
   * Run a command on the server console.
   *
   * @param command the command, without a slash
   */
  public void serverCommand(final String command) {
    this.server.runCommand(command);
  }

  /**
   * Send a command as the player.
   *
   * @param command the command, without a slash
   */
  public void command(final String command) {
    this.client(client -> client.getConnection().sendCommand(command));
  }

  public ReceivedMessages received() {
    return this.received;
  }

  public void awaitSystemMessage(final String text) {
    this.await("system message '" + text + "'", r -> r.contains(ReceivedMessages.Kind.SYSTEM, text));
  }

  public void awaitActionBar(final String text) {
    this.await("action bar '" + text + "'", r -> r.contains(ReceivedMessages.Kind.ACTION_BAR, text));
  }

  public void await(final String description, final Predicate<ReceivedMessages> predicate) {
    try {
      this.context().waitFor(client -> predicate.test(this.received));
    } catch (final AssertionError ex) {
      throw new AssertionError("Timed out waiting for " + description + ", received " + this.received.snapshot(), ex);
    }
  }

  /**
   * Wait until the client has handled everything the server sent so far.
   */
  public void sync() {
    this.connection.waitForClientboundPackets();
  }

  /**
   * Op the player, so tests can run commands as them, and turn on immediate respawn.
   */
  void prepareServer() {
    final String name = this.server.computeOnServer(server -> this.serverPlayer(server).getGameProfile().name());
    this.server.runCommand("op " + name);
    // Respawn straight away, without the death screen
    this.server.runCommand("gamerule immediate_respawn true");
  }

  @Override
  void reset() {
    this.sync();
    this.client(client -> {
      client.gui.setScreen(null);
      client.gui.hud.clearTitles();
      client.gui.hud.resetTitleTimes();
    });
    this.received.clear();
    ReceivedSounds.clear();
  }
}
