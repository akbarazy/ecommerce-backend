package com.akbarazy.ecommercebackend.integration;

import com.akbarazy.ecommercebackend.dto.request.CartItemRequest;
import com.akbarazy.ecommercebackend.dto.request.CheckoutRequest;
import com.akbarazy.ecommercebackend.dto.request.ProductOptionRequest;
import com.akbarazy.ecommercebackend.dto.request.ProductRequest;
import com.akbarazy.ecommercebackend.dto.request.ProductVariantRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateCartItemRequest;
import com.akbarazy.ecommercebackend.dto.request.VariantOptionRequest;
import com.akbarazy.ecommercebackend.entity.Category;
import com.akbarazy.ecommercebackend.entity.User;
import com.akbarazy.ecommercebackend.entity.enums.UserRole;
import com.akbarazy.ecommercebackend.repository.CategoryRepository;
import com.akbarazy.ecommercebackend.repository.ProductVariantRepository;
import com.akbarazy.ecommercebackend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductVariantOrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    private Category savedCategory;
    private User customerUser;

    @BeforeEach
    void setUp() {
        savedCategory = categoryRepository.save(Category.builder()
                .name("Apparel & Fashion")
                .description("Clothing and fashion items")
                .build());

        customerUser = userRepository.save(User.builder()
                .name("Budi Santoso")
                .email("budi@example.com")
                .password("encoded_password")
                .role(UserRole.CUSTOMER)
                .build());
    }

    private ProductRequest buildCreateProductRequest() {
        ProductOptionRequest colorOption = ProductOptionRequest.builder()
                .name("Color")
                .values(List.of("Black", "White"))
                .build();

        ProductOptionRequest sizeOption = ProductOptionRequest.builder()
                .name("Size")
                .values(List.of("M", "L"))
                .build();

        ProductVariantRequest initialVariant = ProductVariantRequest.builder()
                .sku("HOODIE-BLK-M")
                .price(new BigDecimal("250000.00"))
                .stock(5)
                .weight(new BigDecimal("0.60"))
                .imageUrl("https://example.com/hoodie-blk-m.jpg")
                .variantOptions(List.of(
                        VariantOptionRequest.builder().optionName("Color").value("Black").build(),
                        VariantOptionRequest.builder().optionName("Size").value("M").build()
                ))
                .build();

        return ProductRequest.builder()
                .name("Premium Fleece Hoodie")
                .description("Warm heavyweight fleece hoodie")
                .categoryId(savedCategory.getId())
                .options(List.of(colorOption, sizeOption))
                .variants(List.of(initialVariant))
                .build();
    }

    @Test
    @DisplayName("Integration Flow: Create Product with Variants -> Add to Cart -> Validate Stock -> Checkout Order")
    void fullProductVariantToOrderCheckoutFlow() throws Exception {
        // 1. ADMIN creates a product with options and initial variant (stock = 5)
        ProductRequest createProductReq = buildCreateProductRequest();

        MvcResult createProductResult = mockMvc.perform(post("/api/admin/products")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createProductReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Premium Fleece Hoodie"))
                .andExpect(jsonPath("$.data.options.length()").value(2))
                .andExpect(jsonPath("$.data.variants.length()").value(1))
                .andExpect(jsonPath("$.data.variants[0].sku").value("HOODIE-BLK-M"))
                .andExpect(jsonPath("$.data.variants[0].stock").value(5))
                .andReturn();

        JsonNode productData = objectMapper.readTree(createProductResult.getResponse().getContentAsString()).get("data");
        Long productId = productData.get("id").asLong();
        Long firstVariantId = productData.get("variants").get(0).get("id").asLong();

        // 2. ADMIN adds a second variant (stock = 3)
        ProductVariantRequest secondVariantReq = ProductVariantRequest.builder()
                .sku("HOODIE-WHT-L")
                .price(new BigDecimal("275000.00"))
                .stock(3)
                .weight(new BigDecimal("0.65"))
                .imageUrl("https://example.com/hoodie-wht-l.jpg")
                .variantOptions(List.of(
                        VariantOptionRequest.builder().optionName("Color").value("White").build(),
                        VariantOptionRequest.builder().optionName("Size").value("L").build()
                ))
                .build();

        MvcResult addVariantResult = mockMvc.perform(post("/api/admin/products/" + productId + "/variants")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondVariantReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("HOODIE-WHT-L"))
                .andExpect(jsonPath("$.data.stock").value(3))
                .andReturn();

        Long secondVariantId = objectMapper.readTree(addVariantResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 3. PUBLIC (unauthenticated) user reads product variants
        mockMvc.perform(get("/api/products/" + productId + "/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        // 4. CUSTOMER attempts to add quantity exceeding available stock (10 > 5) -> 400 Bad Request
        CartItemRequest excessiveAddReq = CartItemRequest.builder()
                .variantId(firstVariantId)
                .quantity(10)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .with(user(customerUser.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveAddReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Stok tidak mencukupi"));

        // 5. CUSTOMER adds valid quantities of both variants to cart (2 of firstVariant, 1 of secondVariant)
        CartItemRequest addFirstVariantReq = CartItemRequest.builder()
                .variantId(firstVariantId)
                .quantity(2)
                .build();

        MvcResult addItem1Result = mockMvc.perform(post("/api/cart/items")
                        .with(user(customerUser.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addFirstVariantReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.variantId").value(firstVariantId))
                .andExpect(jsonPath("$.data.sku").value("HOODIE-BLK-M"))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(500000.00))
                .andReturn();

        Long cartItem1Id = objectMapper.readTree(addItem1Result.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        CartItemRequest addSecondVariantReq = CartItemRequest.builder()
                .variantId(secondVariantId)
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .with(user(customerUser.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addSecondVariantReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.variantId").value(secondVariantId))
                .andExpect(jsonPath("$.data.sku").value("HOODIE-WHT-L"))
                .andExpect(jsonPath("$.data.quantity").value(1))
                .andExpect(jsonPath("$.data.subtotal").value(275000.00));

        // 6. CUSTOMER attempts to update cart item quantity beyond stock (6 > 5) -> 400 Bad Request
        UpdateCartItemRequest excessiveUpdateReq = UpdateCartItemRequest.builder()
                .quantity(6)
                .build();

        mockMvc.perform(put("/api/cart/items/" + cartItem1Id)
                        .with(user(customerUser.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveUpdateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Stok tidak mencukupi"));

        // 7. CUSTOMER verifies Cart summary (2 distinct items, totalPrice = 775000.00)
        mockMvc.perform(get("/api/cart")
                        .with(user(customerUser.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.totalPrice").value(775000.00));

        // 8. CUSTOMER performs Checkout
        CheckoutRequest checkoutReq = new CheckoutRequest();
        checkoutReq.setShippingAddress("Jl. Sudirman No. 123, Jakarta Pusat");

        MvcResult checkoutResult = mockMvc.perform(post("/api/orders/checkout")
                        .with(user(customerUser.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalPrice").value(775000.00))
                .andExpect(jsonPath("$.data.shippingAddress").value("Jl. Sudirman No. 123, Jakarta Pusat"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].productName").value("Premium Fleece Hoodie"))
                .andExpect(jsonPath("$.data.items[0].variantAttributes").isNotEmpty())
                .andReturn();

        Long orderId = objectMapper.readTree(checkoutResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 9. Verify atomic stock reduction on both ProductVariants in DB
        assertEquals(3, productVariantRepository.findById(firstVariantId).orElseThrow().getStock());
        assertEquals(2, productVariantRepository.findById(secondVariantId).orElseThrow().getStock());

        // 10. Verify Cart is emptied after checkout
        mockMvc.perform(get("/api/cart")
                        .with(user(customerUser.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.totalItems").value(0))
                .andExpect(jsonPath("$.data.totalPrice").value(0));

        // 11. Verify Order can be retrieved by Customer and variant used in Order is protected from deletion
        mockMvc.perform(get("/api/orders/" + orderId)
                        .with(user(customerUser.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        mockMvc.perform(delete("/api/admin/variants/" + firstVariantId)
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot delete variant because it is associated with existing orders"));
    }

    @Test
    @WithMockUser(username = "budi@example.com", roles = "CUSTOMER")
    @DisplayName("Integration Flow: Checkout fails with 400 when variant stock becomes insufficient before checkout")
    void checkoutFailsWhenStockDropsBelowCartQuantity() throws Exception {
        // 1. Admin creates product with initial variant stock = 4
        ProductRequest createProductReq = buildCreateProductRequest();
        createProductReq.getVariants().get(0).setSku("HOODIE-LIMITED-M");
        createProductReq.getVariants().get(0).setStock(4);

        MvcResult createResult = mockMvc.perform(post("/api/admin/products")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createProductReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long variantId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("variants").get(0).get("id").asLong();

        // 2. Customer adds 3 units to cart (valid since 3 <= 4)
        CartItemRequest addToCartReq = CartItemRequest.builder()
                .variantId(variantId)
                .quantity(3)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartReq)))
                .andExpect(status().isCreated());

        // 3. Admin updates variant stock down to 2 before customer checks out
        ProductVariantRequest updateVariantReq = ProductVariantRequest.builder()
                .sku("HOODIE-LIMITED-M")
                .price(new BigDecimal("250000.00"))
                .stock(2)
                .weight(new BigDecimal("0.60"))
                .variantOptions(List.of(
                        VariantOptionRequest.builder().optionName("Color").value("Black").build(),
                        VariantOptionRequest.builder().optionName("Size").value("M").build()
                ))
                .build();

        mockMvc.perform(put("/api/admin/variants/" + variantId)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVariantReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(2));

        // 4. Customer attempts checkout -> fails with 400 Bad Request and stock remains 2
        CheckoutRequest checkoutReq = new CheckoutRequest();
        checkoutReq.setShippingAddress("Jl. Gatot Subroto No. 10, Jakarta");

        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Stok Premium Fleece Hoodie tidak mencukupi"));

        assertEquals(2, productVariantRepository.findById(variantId).orElseThrow().getStock());
    }
}
