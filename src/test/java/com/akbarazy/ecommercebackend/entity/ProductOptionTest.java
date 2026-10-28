package com.akbarazy.ecommercebackend.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductOptionTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("ProductOption with empty values should fail validation")
    void emptyValues_ShouldFailValidation() {
        ProductOption option = ProductOption.builder()
                .name("Color")
                .values(Collections.emptyList())
                .build();

        Set<ConstraintViolation<ProductOption>> violations = validator.validate(option);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("values")
                && v.getMessage().equals("Option values are required")));
    }

    @Test
    @DisplayName("ProductOption with null values should fail validation")
    void nullValues_ShouldFailValidation() {
        ProductOption option = ProductOption.builder()
                .name("Color")
                .values(null)
                .build();

        Set<ConstraintViolation<ProductOption>> violations = validator.validate(option);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("values")
                && v.getMessage().equals("Option values are required")));
    }

    @Test
    @DisplayName("ProductOption with non-empty values should pass validation")
    void nonEmptyValues_ShouldPassValidation() {
        VariantOption variantOption = VariantOption.builder()
                .value("Red")
                .build();

        ProductOption option = ProductOption.builder()
                .name("Color")
                .values(List.of(variantOption))
                .build();

        Set<ConstraintViolation<ProductOption>> violations = validator.validate(option);
        assertTrue(violations.isEmpty());
    }
}
