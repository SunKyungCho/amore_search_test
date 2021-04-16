package me.test.amore_search_test.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Product {

    @JsonProperty("product_name")
    private String productName;
    @JsonProperty("category_name")
    private String categoryName;
    @JsonProperty("product_price")
    private long price;

}
