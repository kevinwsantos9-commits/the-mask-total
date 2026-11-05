package com.nivek.themaskedmorte;

import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final RegistryEntry<ArmorMaterial> MASK_MATERIAL = Registry.registerReference(
            Registries.ARMOR_MATERIAL,
            Identifier.of(TheMaskedMorte.MOD_ID, "mask"),
            new ArmorMaterial(
                    Map.of(ArmorItem.Type.HELMET, 1),
                    0,
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(Identifier.of(TheMaskedMorte.MOD_ID, "mask"))),
                    0.0f,
                    0.0f));

    public static final Item MASK = Registry.register(
            Registries.ITEM,
            Identifier.of(TheMaskedMorte.MOD_ID, "mask"),
            new MaskItem(MASK_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()));

    private ModItems() {}

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(MASK));
    }
}
