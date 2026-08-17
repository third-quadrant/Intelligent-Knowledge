package com.third_quadrant.intelligentknowledge.item;

import com.third_quadrant.intelligentknowledge.registry.ModDataComponents;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class NoteItem extends Item {
    public static final int MAX_PAGES = 200;

    public NoteItem(Properties props) {
        super(props);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getPageCount(stack) == 0 ? 16 : 1;
    }

    // lore 표시: [페이지] 0/200
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int pages = getPageCount(stack);
        tooltip.add(Component.literal("§7[페이지] " + pages + "/" + MAX_PAGES));
    }

    public static int getPageCount(ItemStack stack) {
        Integer val = stack.get(ModDataComponents.NOTE_PAGES.get());
        return val != null ? val : 0;
    }

    public static void setPageCount(ItemStack stack, int pages) {
        stack.set(ModDataComponents.NOTE_PAGES.get(), Math.min(pages, MAX_PAGES));
    }

    public static void addPage(ItemStack stack) {
        setPageCount(stack, getPageCount(stack) + 1);
    }

    public static boolean canUse(ItemStack stack) {
        return getPageCount(stack) < MAX_PAGES;
    }
}
