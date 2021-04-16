package me.test.amore_search_test.api;


import lombok.RequiredArgsConstructor;
import me.test.amore_search_test.dto.ProductRequest;
import me.test.amore_search_test.dto.SearchResult;
import me.test.amore_search_test.service.ProductSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductSearchService productSearchService;

    @GetMapping("/products")
    public ResponseEntity<SearchResult> search(ProductRequest request) {
        return ResponseEntity.ok(productSearchService.search(request));
    }
}
