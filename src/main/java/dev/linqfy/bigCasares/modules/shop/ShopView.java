package dev.linqfy.bigCasares.modules.shop;

import java.util.List;

public record ShopView(
    String id,
    String title,
    String description,
    String balance,
    List<ShopViewItem> items
) {
    public ShopView {
        items = List.copyOf(items);
    }
}
