package pw.komarov.annotations.tests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import pw.komarov.annotations.Property;

class PropertyTest {
    @Property
    private int fieldName = 10;

    @Test
    void propertyTest() {
        setFieldName(getFieldName() + 5);
        assertEquals(15, fieldName);
    }
}
