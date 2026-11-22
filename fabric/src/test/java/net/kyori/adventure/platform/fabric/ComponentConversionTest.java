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

import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.ClickCallbackRegistry;
import net.kyori.adventure.platform.modcommon.impl.NonWrappingComponentSerializer;
import net.kyori.adventure.platform.modcommon.impl.WrappedComponent;
import net.kyori.adventure.platform.modcommon.impl.nbt.ModDataComponentValue;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.kyori.adventure.text.format.Style.style;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Named.named;

class ComponentConversionTest extends BootstrappedTest {
  // TODO: ItemStack no longer available before data is loaded
  @SuppressWarnings("deprecation")
  static Stream<Object> testedComponents() {
    return Stream.<Component>of(
      Component.text("Hi"),
      Component.translatable("gameMode.creative", style(NamedTextColor.RED).font(Key.key("uniform"))),
      Component.text("Hello").append(Component.text(" friends", TextColor.color(0xaabbcc))),
      Component.keybind("key.jump"),
      //Component.text("Hello world", style(new ItemStack(Items.ACACIA_LOG, 1).asHoverEvent())),
      Component.text("Hello world", style(showItem(new ItemStackTemplate(Items.ACACIA_LOG, 1)))),
      //Component.text("I've got a hover!", style(new ItemStack(Items.ACACIA_LOG.builtInRegistryHolder(), 1, DataComponentPatch.builder().set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal("test")).build()).asHoverEvent()))
      Component.text("I've got a hover!", style(showItem(new ItemStackTemplate(Items.ACACIA_LOG.builtInRegistryHolder(), 1, DataComponentPatch.builder().set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal("test")).build()))))
    )
      .map(comp -> named(MiniMessage.miniMessage().serialize(comp), comp));
  }

  @ParameterizedTest
  @MethodSource("net.kyori.adventure.platform.fabric.ComponentConversionTest#testedComponents")
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface TestOnComponents {
  }

  @TestOnComponents
  void testComponentEqualSerializationWrapped(final Component input) {
    final JsonElement serialized = GsonComponentSerializer.gson().serializeToTree(input);
    final JsonElement serializedNative = this.componentToJson(this.asNativeWrapped(input));

    assertJsonTreesEqual(serializedNative, serialized);
  }

  @TestOnComponents
  void testNonWrappingSerializerComponentsEqual(final Component input) {
    final JsonElement serialized = GsonComponentSerializer.gson().serializeToTree(input);
    final JsonElement serializedNative = this.componentToJson(
      ComponentSerialization.CODEC.decode(lookup().createSerializationContext(JsonOps.INSTANCE), serialized).getOrThrow().getFirst()
    );

    assertJsonTreesEqual(serializedNative, serialized);
  }

  @TestOnComponents
  void testComponentEqualSerializationWrappedAfterDeepConversion(final Component input) {
    final JsonElement serialized = GsonComponentSerializer.gson().serializeToTree(input);
    final net.minecraft.network.chat.Component mc = this.asNativeWrapped(input);
    mc.getStyle(); // trigger deep conversion
    final JsonElement serializedNative = this.componentToJson(mc);

    assertJsonTreesEqual(serializedNative, serialized);
  }

  @TestOnComponents
  void testSerializationEqualWhenWrappedNested(final Component input) {
    final JsonElement serialized = GsonComponentSerializer.gson().serializeToTree(Component.text("Adventure says: ").append(input));
    final net.minecraft.network.chat.Component mc = net.minecraft.network.chat.Component.literal("Adventure says: ").append(this.asNativeWrapped(input));
    final JsonElement serializedNative = this.componentToJson(mc);

    assertJsonTreesEqual(serializedNative, serialized);
  }

  @TestOnComponents
  void testComponentEquality(final Component input) {
    final net.minecraft.network.chat.Component deepConverted = new NonWrappingComponentSerializer(BootstrappedTest::lookup).serialize(input);
    final net.minecraft.network.chat.Component wrapped = this.asNativeWrapped(input);
    final net.minecraft.network.chat.Component wrapped2 = this.asNativeWrapped(input);
    assertEquals(wrapped, wrapped2, "two wrapped components should be equal");
    assertEquals(deepConverted, wrapped, "deep should equal wrapped");
    assertEquals(wrapped, deepConverted, "wrapped should equal deep");
  }

  @Test
  void testClickCallbackConvertsToCustomClickAction() {
    final Component input = Component.text("Click me")
      .clickEvent(ClickCallbackRegistry.INSTANCE.register(audience -> {}, ClickCallback.Options.builder().build()));
    final net.minecraft.network.chat.Component converted = NonWrappingComponentSerializer.INSTANCE.serialize(input);

    final ClickEvent.Custom custom = assertInstanceOf(ClickEvent.Custom.class, converted.getStyle().getClickEvent());
    assertEquals(AdventureCommon.res("click_callback"), custom.id());
    final Tag payload = custom.payload().orElseThrow();
    assertTrue(payload.asCompound().flatMap(tag -> tag.read("id", UUIDUtil.CODEC)).isPresent(), () -> "payload missing callback id: " + payload);
  }

  @Test
  void testCustomClickPayloadRoundTrip() throws Exception {
    final Component input = Component.text("Click me")
      .clickEvent(net.kyori.adventure.text.event.ClickEvent.custom(Key.key("adventure", "test"), BinaryTagHolder.binaryTagHolder("{a:1b,b:[I;1,2],c:\"str\",d:{e:[1.5d,2.5d]}}")));
    final net.minecraft.network.chat.Component converted = NonWrappingComponentSerializer.INSTANCE.serialize(input);
    final ClickEvent.Custom custom = assertInstanceOf(ClickEvent.Custom.class, converted.getStyle().getClickEvent());
    assertEquals(TagParser.parseCompoundFully("{a:1b,b:[I;1,2],c:\"str\",d:{e:[1.5d,2.5d]}}"), custom.payload().orElseThrow());

    final Component roundTripped = NonWrappingComponentSerializer.INSTANCE.deserialize(converted);
    assertEquals(NonWrappingComponentSerializer.INSTANCE.serialize(roundTripped), converted);
  }

  // enchantments reference a datapack registry, so they can only be converted with registry context
  @Test
  void testRegistryDependentDataComponentRoundTrip() {
    final NonWrappingComponentSerializer serializer = new NonWrappingComponentSerializer(BootstrappedTest::lookup);
    final DataComponentValue.TagSerializable enchantments = () -> BinaryTagHolder.binaryTagHolder("{\"minecraft:sharpness\":3}");
    final Component input = Component.text("Hover").hoverEvent(HoverEvent.showItem(Key.key("diamond_sword"), 1, Map.of(Key.key("enchantments"), enchantments)));

    final net.minecraft.network.chat.Component converted = serializer.serialize(input);
    final net.minecraft.network.chat.HoverEvent.ShowItem nativeHover = assertInstanceOf(net.minecraft.network.chat.HoverEvent.ShowItem.class, converted.getStyle().getHoverEvent());
    // item prototypes aren't bound in tests, so read the patch directly
    final ItemEnchantments nativeEnchantments = nativeHover.item().components().split().added().get(DataComponents.ENCHANTMENTS);
    assertEquals(3, nativeEnchantments.getLevel(lookup().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS)));

    final Component roundTripped = serializer.deserialize(converted);
    final HoverEvent.ShowItem hover = assertInstanceOf(HoverEvent.ShowItem.class, roundTripped.hoverEvent().value());
    final DataComponentValue value = hover.dataComponents().get(Key.key("enchantments"));
    final DataComponentValue.TagSerializable tag = assertInstanceOf(DataComponentValue.TagSerializable.class, value);
    assertEquals("{\"minecraft:sharpness\":3}", tag.asBinaryTag().string());
    assertTrue(GsonComponentSerializer.gson().serialize(roundTripped).contains("\"minecraft:sharpness\":3"));
    assertEquals(converted, serializer.serialize(roundTripped));
  }

  private static HoverEvent<HoverEvent.ShowItem> showItem(final ItemStackTemplate template) {
    return ModDataComponentValue.showItem(template.typeHolder(), template.count(), template.components(), lookup().createSerializationContext(JsonOps.INSTANCE));
  }

  private static void assertJsonTreesEqual(final JsonElement expected, final JsonElement actual) {
    assertEquals(toStableString(expected), toStableString(actual));
  }

  private static String toStableString(final JsonElement stringable) {
    final StringWriter writer = new StringWriter();
    final JsonWriter jw = new JsonWriter(writer);
    jw.setIndent("  ");
    jw.setHtmlSafe(false);
    try {
      GsonHelper.writeValue(jw, stringable, Comparator.naturalOrder());
    } catch (final IOException ex) {
      fail("Could not write json", ex);
    }

    return writer.toString();
  }

  private WrappedComponent asNativeWrapped(final Component component) {
    final Function<Pointered, Locale> partition = AdventureCommon.localePartition();
    return new WrappedComponent(component, partition, GlobalTranslator.renderer().mapContext(partition), NonWrappingComponentSerializer.INSTANCE);
  }
}
