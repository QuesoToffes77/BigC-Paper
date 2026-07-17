package dev.linqfy.bigCasares.items.catalog;

@FunctionalInterface
public interface ItemMaterialValidator {

    boolean isItem(String materialName);
}
