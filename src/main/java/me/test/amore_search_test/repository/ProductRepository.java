package me.test.amore_search_test.repository;

import me.test.amore_search_test.domain.Product;
import me.test.amore_search_test.dto.ProductRequest;

import java.util.List;

public interface ProductRepository {

    List<Product> search(ProductRequest request);
}
