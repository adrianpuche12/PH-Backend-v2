package balance.catalog.controller;

import balance.catalog.dto.ProductRequestDTO;
import balance.catalog.dto.ProductResponseDTO;
import balance.catalog.dto.RecipeItemDTO;
import balance.catalog.repository.ProductRecipeRepository;
import balance.catalog.repository.ProductRepository;
import balance.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRecipeRepository recipeRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/api/v2/stores/{storeId}/products")
    public ResponseEntity<List<ProductResponseDTO>> getByStore(
            @PathVariable Long storeId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.findByStore(storeId, active, categoryId, search));
    }

    @GetMapping("/api/v2/products/{id}")
    public ResponseEntity<ProductResponseDTO> getById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/v2/stores/{storeId}/products")
    public ResponseEntity<?> create(@PathVariable Long storeId,
                                    @Valid @RequestBody ProductRequestDTO dto) {
        try {
            return productService.create(storeId, dto)
                    .map(p -> ResponseEntity.status(HttpStatus.CREATED).body(p))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/v2/products/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @Valid @RequestBody ProductRequestDTO dto) {
        try {
            return productService.update(id, dto)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/v2/products/{id}/toggle")
    public ResponseEntity<ProductResponseDTO> toggle(@PathVariable Long id) {
        return productService.toggle(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/v2/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (productService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // â”€â”€ Receta de producto manufacturado â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/api/v2/products/{id}/recipe")
    public ResponseEntity<List<RecipeItemDTO>> getRecipe(@PathVariable Long id) {
        if (!productRepository.existsById(id)) return ResponseEntity.notFound().build();
        List<RecipeItemDTO> items = recipeRepository.findByProductIdWithIngredient(id)
                .stream().map(RecipeItemDTO::from).toList();
        return ResponseEntity.ok(items);
    }

    @PutMapping("/api/v2/products/{id}/recipe")
    public ResponseEntity<?> saveRecipe(@PathVariable Long id,
                                        @Valid @RequestBody List<RecipeItemDTO> items) {
        try {
            if (!productRepository.existsById(id)) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(productService.saveRecipe(id, items));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/v2/products/{productId}/recipe/{itemId}")
    public ResponseEntity<Void> deleteRecipeItem(@PathVariable Long productId,
                                                  @PathVariable Long itemId) {
        if (!productService.deleteRecipeItem(productId, itemId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}

