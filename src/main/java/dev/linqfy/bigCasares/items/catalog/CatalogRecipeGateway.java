package dev.linqfy.bigCasares.items.catalog;

public interface CatalogRecipeGateway {

    void remove(String recipeKey);

    /**
     * Registers one catalog recipe. {@code catalog} is the full candidate
     * catalog: it provides the revision, the owning result definition (when the
     * recipe produces a custom item) and the definitions behind custom-item
     * ingredient references such as {@code bigcasares:potassium_nitrate}.
     */
    boolean register(String recipeKey, ItemRecipeDefinition recipe, CustomItemCatalog catalog);
}
