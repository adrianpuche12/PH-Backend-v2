package balance.catalog.controller;

import balance.catalog.dto.ProductRequestDTO;
import balance.catalog.dto.ProductResponseDTO;
import balance.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/api/v2/stores/{storeId}/products")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    public ResponseEntity<List<ProductResponseDTO>> getByStore(
            @PathVariable Long storeId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.findByStore(storeId, active, categoryId, search));
    }

    @GetMapping("/api/v2/products/{id}")
    @PreAuthorize("hasAnyRole('admin', 'user')")
    public ResponseEntity<ProductResponseDTO> getById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/v2/stores/{storeId}/products")
    @PreAuthorize("hasRole('admin')")
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
    @PreAuthorize("hasRole('admin')")
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
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ProductResponseDTO> toggle(@PathVariable Long id) {
        return productService.toggle(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/v2/products/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (productService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
