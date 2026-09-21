package com.example.nexuscore.categorization;

public enum FoodCategory {
    SUGARS_AND_SWEETENERS("Açúcares e Adoçantes"),
    BEVERAGES("Bebidas"),
    BISCUITS_AND_SNACKS("Biscoitos e Snacks"),
    MEAT_POULTRY_AND_FISH("Carnes, Aves e Pescados"),
    CEREALS_AND_GRAINS("Cereais e Grãos"),
    FROZEN("Congelados"),
    PRESERVES_AND_CANNED_GOODS("Conservas e Enlatados"),
    SWEETS_AND_DESSERTS("Doces e Sobremesas"),
    FLOURS_YEAST_AND_MIXES("Farinhas, Fermentos e Misturas"),
    FRUITS_AND_VEGETABLES("Frutas, Verduras e Legumes"),
    DAIRY_AND_EGGS("Laticínios e Ovos"),
    PASTA("Massas"),
    SAUCES_CONDIMENTS_AND_SEASONINGS("Molhos, Condimentos e Temperos"),
    OILS_AND_FATS("Óleos e Gorduras"),
    BAKERY("Padaria"),
    READY_MEALS("Pratos Prontos"),
    OTHER("Outros");

    private final String displayName;

    FoodCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
