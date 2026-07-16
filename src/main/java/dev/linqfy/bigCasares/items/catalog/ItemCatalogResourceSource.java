package dev.linqfy.bigCasares.items.catalog;

import java.io.InputStream;

@FunctionalInterface
public interface ItemCatalogResourceSource {

    InputStream open(String resourcePath);
}
