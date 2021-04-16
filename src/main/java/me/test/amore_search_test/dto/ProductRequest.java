package me.test.amore_search_test.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    private int page = 1;
    private int pageSize = 10;
    private String keyword;

}
