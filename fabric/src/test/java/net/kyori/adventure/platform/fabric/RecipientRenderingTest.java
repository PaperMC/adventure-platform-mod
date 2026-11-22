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
package net.kyori.adventure.platform.fabric;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.function.Function;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.NonWrappingComponentSerializer;
import net.kyori.adventure.platform.modcommon.impl.PacketRecipient;
import net.kyori.adventure.platform.modcommon.impl.WrappedComponent;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RecipientRenderingTest extends BootstrappedTest {
  private static final String KEY = "adventure.test.greeting";
  private static final Pointered GERMAN = new Pointered() {
    @Override
    public Pointers pointers() {
      return Pointers.builder().withStatic(Identity.LOCALE, Locale.GERMAN).build();
    }
  };
  private static TranslationStore.StringBased<MessageFormat> translations;

  @BeforeAll
  static void registerTranslations() {
    translations = TranslationStore.messageFormat(Key.key("adventure", "test"));
    translations.register(KEY, Locale.GERMAN, new MessageFormat("Hallo"));
    GlobalTranslator.translator().addSource(translations);
  }

  @AfterAll
  static void unregisterTranslations() {
    GlobalTranslator.translator().removeSource(translations);
  }

  @Test
  void testWrappedComponentRenderedForRecipient() {
    final Component sent = roundTrip(ComponentSerialization.STREAM_CODEC, wrapped(net.kyori.adventure.text.Component.translatable(KEY)), GERMAN);
    assertEquals("Hallo", sent.getString());
  }

  @Test
  void testNestedWrappedComponentRenderedForRecipient() {
    final Component input = Component.literal("> ").append(wrapped(net.kyori.adventure.text.Component.translatable(KEY)));
    assertEquals("> Hallo", roundTrip(ComponentSerialization.STREAM_CODEC, input, GERMAN).getString());
  }

  @Test
  void testNativeTranslatableRenderedForRecipient() {
    assertEquals("Hallo", roundTrip(ComponentSerialization.STREAM_CODEC, Component.translatable(KEY), GERMAN).getString());
  }

  @Test
  void testNotRenderedWithoutRecipient() {
    final Component sent = roundTrip(ComponentSerialization.STREAM_CODEC, Component.translatable(KEY), null);
    assertEquals(KEY, assertInstanceOf(TranslatableContents.class, sent.getContents()).getKey());
  }

  @Test
  void testItemComponentsNotRendered() {
    final ItemStack stack = new ItemStack(Items.DIAMOND);
    stack.set(DataComponents.CUSTOM_NAME, Component.translatable(KEY));
    final ItemStack sent = roundTrip(ItemStack.OPTIONAL_STREAM_CODEC, stack, GERMAN);
    assertEquals(KEY, assertInstanceOf(TranslatableContents.class, sent.get(DataComponents.CUSTOM_NAME).getContents()).getKey());
  }

  // Like PacketEncoder, create the buffer while encoding for the receiver
  private static <V> V roundTrip(final StreamCodec<RegistryFriendlyByteBuf, V> codec, final V value, final @Nullable Pointered recipient) {
    final ByteBuf raw = Unpooled.buffer();
    final RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    PacketRecipient.encodeFor(recipient, () -> codec.encode(new RegistryFriendlyByteBuf(raw, registries), value));
    return codec.decode(new RegistryFriendlyByteBuf(raw, registries));
  }

  private static WrappedComponent wrapped(final net.kyori.adventure.text.Component component) {
    final Function<Pointered, Locale> partition = AdventureCommon.localePartition();
    return new WrappedComponent(component, partition, GlobalTranslator.renderer().mapContext(partition), NonWrappingComponentSerializer.INSTANCE);
  }
}
