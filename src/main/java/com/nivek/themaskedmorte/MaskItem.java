package com.nivek.themaskedmorte;

import java.util.List;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MaskItem extends ArmorItem {
    public MaskItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(material, type, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.themaskedmorte.mask.tip1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.themaskedmorte.mask.tip2").formatted(Formatting.DARK_GRAY));
    }
}
