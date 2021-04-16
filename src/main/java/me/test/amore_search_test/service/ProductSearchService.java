package me.test.amore_search_test.service;

import lombok.RequiredArgsConstructor;
import me.test.amore_search_test.domain.Product;
import me.test.amore_search_test.dto.ProductRequest;
import me.test.amore_search_test.dto.SearchResult;
import me.test.amore_search_test.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;

    public SearchResult search(ProductRequest request) {
        List<Product> products = productRepository.search(request);
        return new SearchResult(products);
    }
}
