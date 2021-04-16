package me.test.amore_search_test.dto;


import lombok.Getter;
import me.test.amore_search_test.domain.Product;

import java.util.List;

@Getter
public class SearchResult {
    private List<Product> products;

    public SearchResult(List<Product> products) {
        this.products = products;
    }
}
