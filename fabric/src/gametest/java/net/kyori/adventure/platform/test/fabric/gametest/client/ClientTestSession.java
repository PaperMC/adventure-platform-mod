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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs client test cases and the client-side helpers they share.
 *
 * <p>Each case runs through {@link #test(String, Runnable)}, which resets afterwards and records failures,
 * so every case runs and all failures are reported at the end.</p>
 */
public class ClientTestSession {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientTestSession.class);

  private final ClientGameTestContext context;
  private final List<String> failures;
  private String group = "";

  ClientTestSession(final ClientGameTestContext context) {
    this.context = context;
    this.failures = new ArrayList<>();
  }

  // Continues with the same client and failures
  ClientTestSession(final ClientTestSession session) {
    this.context = session.context;
    this.failures = session.failures;
  }

  void group(final String name) {
    this.group = name;
  }

  public void test(final String name, final Runnable testCase) {
    final String fullName = this.group + " > " + name;
    try {
      testCase.run();
      LOGGER.info("PASSED {}", fullName);
    } catch (final AssertionError | RuntimeException ex) {
      LOGGER.error("FAILED {}", fullName, ex);
      this.failures.add(fullName + ": " + ex.getMessage());
    } finally {
      this.reset();
    }
  }

  /**
   * Run an action on the client thread.
   *
   * @param action the action
   */
  public void client(final Consumer<Minecraft> action) {
    this.context.runOnClient(action::accept);
  }

  public <T> T computeOnClient(final Function<Minecraft, T> function) {
    return this.context.computeOnClient(function::apply);
  }

  public void await(final String description, final Supplier<Boolean> condition) {
    try {
      this.context.waitFor(client -> condition.get());
    } catch (final AssertionError ex) {
      throw new AssertionError("Timed out waiting for " + description, ex);
    }
  }

  ClientGameTestContext context() {
    return this.context;
  }

  // Clean up after a test case
  void reset() {
  }

  void assertNoFailures() {
    if (!this.failures.isEmpty()) {
      throw new AssertionError(this.failures.size() + " client test(s) failed:\n" + String.join("\n", this.failures));
    }
  }
}
