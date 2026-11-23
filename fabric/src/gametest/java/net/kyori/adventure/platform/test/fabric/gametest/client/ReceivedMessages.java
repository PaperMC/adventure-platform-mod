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
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

/**
 * Records the messages and titles the client receives.
 */
public final class ReceivedMessages {
  private final List<Received> received = new CopyOnWriteArrayList<>();

  public enum Kind {
    SYSTEM,
    CHAT,
    ACTION_BAR,
    TITLE,
    SUBTITLE,
    CLEAR_TITLES,
    RESET_TITLES
  }

  public record Received(Kind kind, String text, Component message) {
  }

  private ReceivedMessages() {
  }

  // Fabric events can't be unregistered, so this is registered once for the whole run
  static ReceivedMessages register() {
    final ReceivedMessages messages = new ReceivedMessages();
    ClientReceiveMessageEvents.GAME.register((message, overlay) -> messages.add(overlay ? Kind.ACTION_BAR : Kind.SYSTEM, message));
    ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> messages.add(Kind.CHAT, message));
    ClientReceiveMessageEvents.TITLE.register(title -> messages.add(Kind.TITLE, title));
    ClientReceiveMessageEvents.SUBTITLE.register(subtitle -> messages.add(Kind.SUBTITLE, subtitle));
    ClientReceiveMessageEvents.CLEAR_TITLES.register(reset -> messages.add(reset ? Kind.RESET_TITLES : Kind.CLEAR_TITLES, Component.empty()));
    return messages;
  }

  private void add(final Kind kind, final Component message) {
    this.received.add(new Received(kind, message.getString(), message));
  }

  public boolean contains(final Kind kind, final String text) {
    return this.find(kind, text).isPresent();
  }

  public Optional<Received> find(final Kind kind, final String text) {
    return this.received.stream().filter(r -> r.kind() == kind && r.text().equals(text)).findFirst();
  }

  public boolean any(final Predicate<Received> predicate) {
    return this.received.stream().anyMatch(predicate);
  }

  public List<Received> snapshot() {
    return List.copyOf(this.received);
  }

  void clear() {
    this.received.clear();
  }
}
