package com.example.nexuscore.categorization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GpcCategoryResolverTest {

    private final GpcCategoryResolver resolver = new GpcCategoryResolver();

    @Test
    void resolvesEveryInternalCategory() {
        assertEquals("Açúcares e Adoçantes", resolver.resolve("10000043"));
        assertEquals("Bebidas", resolver.resolve("10008042"));
        assertEquals("Biscoitos e Snacks", resolver.resolve("10000160"));
        assertEquals("Carnes, Aves e Pescados", resolver.resolve("10000146"));
        assertEquals("Cereais e Grãos", resolver.resolve("10000601"));
        assertEquals("Congelados", resolver.resolve("10000307"));
        assertEquals("Conservas e Enlatados", resolver.resolve("10000147"));
        assertEquals("Doces e Sobremesas", resolver.resolve("10006390"));
        assertEquals("Farinhas, Fermentos e Misturas", resolver.resolve("10000068"));
        assertEquals("Frutas, Verduras e Legumes", resolver.resolve("10008039"));
        assertEquals("Laticínios e Ovos", resolver.resolve("10000168"));
        assertEquals("Massas", resolver.resolve("10000317"));
        assertEquals("Molhos, Condimentos e Temperos", resolver.resolve("10008044"));
        assertEquals("Óleos e Gorduras", resolver.resolve("10000041"));
        assertEquals("Padaria", resolver.resolve("10000164"));
        assertEquals("Pratos Prontos", resolver.resolve("10005826"));
        assertEquals("Outros", resolver.resolve("10008449"));
    }

    @Test
    void usesFallbackForMissingOrUnknownCodes() {
        assertEquals("Outros", resolver.resolve(null));
        assertEquals("Outros", resolver.resolve(""));
        assertEquals("Outros", resolver.resolve("   "));
        assertEquals("Outros", resolver.resolve("99999999"));
    }

    @Test
    void ignoresSurroundingWhitespace() {
        assertEquals("Açúcares e Adoçantes", resolver.resolve(" 10000043 "));
    }
}
