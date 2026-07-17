package dev.linqfy.bigCasares.items.catalog;

public interface CatalogRecipeGateway {

    void remove(String recipeKey);

    boolean register(
        String recipeKey,
        CustomItemDefinition result,
        ItemRecipeDefinition recipe,
        String catalogRevision
    );
}
