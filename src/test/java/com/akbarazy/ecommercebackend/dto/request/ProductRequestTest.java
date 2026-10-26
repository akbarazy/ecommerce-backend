package com.akbarazy.ecommercebackend.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private ProductRequest buildValidProductRequest() {
        ProductOptionRequest colorOption = ProductOptionRequest.builder()
                .name("Color")
                .values(List.of("Black", "White"))
                .build();

        ProductOptionRequest sizeOption = ProductOptionRequest.builder()
                .name("Size")
                .values(List.of("M", "L"))
                .build();

        VariantOptionRequest var1Color = VariantOptionRequest.builder()
                .optionName("Color")
                .value("Black")
                .build();

        VariantOptionRequest var1Size = VariantOptionRequest.builder()
                .optionName("Size")
                .value("M")
                .build();

        ProductVariantRequest variant1 = ProductVariantRequest.builder()
                .sku("TSHIRT-BLK-M")
                .price(new BigDecimal("150000.00"))
                .stock(10)
                .optionValues(List.of(var1Color, var1Size))
                .build();

        return ProductRequest.builder()
                .name("Basic T-Shirt")
                .description("Comfortable cotton t-shirt")
                .categoryId(1L)
                .options(List.of(colorOption, sizeOption))
                .variants(List.of(variant1))
                .build();
    }

    @Test
    @DisplayName("Valid product request should pass validation with no violations")
    void validProductRequest_ShouldPassValidation() {
        ProductRequest request = buildValidProductRequest();
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Expected no violations, but found: " + violations);
    }

    @Test
    @DisplayName("Empty options should trigger @NotEmpty violation")
    void emptyOptions_ShouldFailValidation() {
        ProductRequest request = buildValidProductRequest();
        request.setOptions(Collections.emptyList());

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("options")
                && v.getMessage().equals("Product options are required")));
    }

    @Test
    @DisplayName("Null options should trigger @NotEmpty violation")
    void nullOptions_ShouldFailValidation() {
        ProductRequest request = buildValidProductRequest();
        request.setOptions(null);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("options")));
    }

    @Test
    @DisplayName("Empty variants should trigger @NotEmpty violation")
    void emptyVariants_ShouldFailValidation() {
        ProductRequest request = buildValidProductRequest();
        request.setVariants(Collections.emptyList());

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("variants")
                && v.getMessage().equals("Product variants are required")));
    }

    @Test
    @DisplayName("Null variants should trigger @NotEmpty violation")
    void nullVariants_ShouldFailValidation() {
        ProductRequest request = buildValidProductRequest();
        request.setVariants(null);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("variants")));
    }

    @Test
    @DisplayName("Variant missing one of registered options should fail cross-field validation")
    void variantMissingOption_ShouldFailValidation() {
        ProductRequest request = buildValidProductRequest();
        // Variant only defines "Color", missing "Size"
        VariantOptionRequest var1Color = VariantOptionRequest.builder()
                .optionName("Color")
                .value("Black")
                .build();

        request.getVariants().get(0).setOptionValues(List.of(var1Color));

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Each variant must define values for all registered options")));
    }

    @Test
    @DisplayName("Variant with unregistered option should fail cross-field validation")
    void variantWithUnregisteredOption_ShouldFailValidation() {
        ProductRequest request = buildValidProductRequest();
        VariantOptionRequest var1Color = VariantOptionRequest.builder()
                .optionName("Color")
                .value("Black")
                .build();

        VariantOptionRequest var1Size = VariantOptionRequest.builder()
                .optionName("Size")
                .value("M")
                .build();

        VariantOptionRequest unregisteredMaterial = VariantOptionRequest.builder()
                .optionName("Material")
                .value("Cotton")
                .build();

        request.getVariants().get(0).setOptionValues(List.of(var1Color, var1Size, unregisteredMaterial));

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Each variant must define values for all registered options")));
    }

    @Test
    @DisplayName("Variant with duplicate options in same variant should fail validation")
    void variantWithDuplicateOption_ShouldFailValidation() {
        ProductRequest request = buildValidProductRequest();
        VariantOptionRequest var1Color = VariantOptionRequest.builder()
                .optionName("Color")
                .value("Black")
                .build();

        VariantOptionRequest var1Size1 = VariantOptionRequest.builder()
                .optionName("Size")
                .value("M")
                .build();

        VariantOptionRequest var1Size2 = VariantOptionRequest.builder()
                .optionName("Size")
                .value("L")
                .build();

        request.getVariants().get(0).setOptionValues(List.of(var1Color, var1Size1, var1Size2));

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Each variant must define values for all registered options")));
    }

    @Test
    @DisplayName("Variant with value outside registered allowed values should fail validation")
    void variantWithValueOutsideAllowedValues_ShouldFailValidation() {
        ProductRequest request = buildValidProductRequest();
        // Registered sizes are "M", "L". Variant specifies "XL".
        VariantOptionRequest var1Color = VariantOptionRequest.builder()
                .optionName("Color")
                .value("Black")
                .build();

        VariantOptionRequest var1Size = VariantOptionRequest.builder()
                .optionName("Size")
                .value("XL")
                .build();

        request.getVariants().get(0).setOptionValues(List.of(var1Color, var1Size));

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Each variant must define values for all registered options")));
    }

    @Test
    @DisplayName("Options without predefined values list should accept any non-blank value")
    void optionsWithoutPredefinedValues_ShouldAcceptAnyValue() {
        ProductOptionRequest colorOption = ProductOptionRequest.builder()
                .name("Color")
                .build(); // values list is empty

        VariantOptionRequest var1Color = VariantOptionRequest.builder()
                .optionName("Color")
                .value("Navy Blue")
                .build();

        ProductVariantRequest variant1 = ProductVariantRequest.builder()
                .sku("TSHIRT-NVY")
                .price(new BigDecimal("150000.00"))
                .stock(10)
                .optionValues(List.of(var1Color))
                .build();

        ProductRequest request = ProductRequest.builder()
                .name("Custom T-Shirt")
                .categoryId(1L)
                .options(List.of(colorOption))
                .variants(List.of(variant1))
                .build();

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Expected no violations, but found: " + violations);
    }
}