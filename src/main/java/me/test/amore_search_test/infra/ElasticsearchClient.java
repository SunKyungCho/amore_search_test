package me.test.amore_search_test.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.test.amore_search_test.domain.Product;
import me.test.amore_search_test.dto.ProductRequest;
import me.test.amore_search_test.repository.ProductRepository;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder;


@Repository
@RequiredArgsConstructor
public class ElasticsearchClient implements ProductRepository {

    private final static String PRODUCT_INDEX = "product";

    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<Product> search(ProductRequest request) {
        final String keyword = request.getKeyword();
        final int page = request.getPage();
        final int size = request.getPageSize();

        try {
            SearchRequest searchRequest = new SearchRequest(PRODUCT_INDEX);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(createQuery(keyword));
            searchSourceBuilder.from((page * size) - size);
            searchSourceBuilder.size(size);
            searchRequest.source(searchSourceBuilder);

            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            if (isEmpty(hits)) {
                return new ArrayList<>();
            }
            return Stream.of(hits.getHits())
                    .map(SearchHit::getSourceAsMap)
                    .map(x -> objectMapper.convertValue(x, Product.class))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new ElasticsearchException("");
        }
    }

    private boolean isEmpty(SearchHits hits) {
        return hits.getTotalHits().value == 0;
    }

    private QueryBuilder createQuery(String keyword) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder
                .should(QueryBuilders.matchQuery("category_name", keyword).operator(Operator.OR))
                .should(QueryBuilders.matchQuery("category_name.korean", keyword).operator(Operator.AND))
                .should(QueryBuilders.matchQuery("product_name", keyword).operator(Operator.OR))
                .should(QueryBuilders.matchQuery("product_name.korean", keyword).operator(Operator.AND));

        FilterFunctionBuilder[] functions = {
                new FilterFunctionBuilder(
                        QueryBuilders.termQuery("category_name", "생활용품").boost(1000),
                        ScoreFunctionBuilders.weightFactorFunction(1000))
        };
        return QueryBuilders.functionScoreQuery(boolQueryBuilder, functions);
    }
}
