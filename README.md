# Amore_search_test

## Use Case

### 1. Mapping Template
상품 Index의 template 

[Product Index Template 확인](https://github.com/SunKyungCho/amore_search_test/blob/main/product_index_tempalte.json) 

```javascript
{
  "index_patterns": [
    "product*"
  ], 
  "settings": {
    "number_of_replicas": 0,
    "number_of_shards": 1,
    "analysis": {
      "tokenizer": {
        "nori_tokenizer": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed",
          "user_dictionary": "dictionary/user_dic.txt"
        }
      },
      "filter": {
        "synonym": {
          "type": "synonym",
          "synonyms_path": "dictionary/synonym.txt"
        }
      },
      "analyzer": {
        "korean": {
          "filter": [
            "lowercase",
            "synonym"
          ],
          "tokenizer": "nori_tokenizer"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "@timestamp": {
        "type": "date"
      },
      "@version": {
        "type": "keyword"
      },
      "brand_name": {
        "type": "keyword",
        "fields": {
          "korean": {
            "type": "text",
            "analyzer": "korean"
          }
        }
      },
      "category_name": {
        "type": "keyword",
        "fields": {
          "korean": {
            "type": "text",
            "analyzer": "korean"
          }
        }
      },
      "category_no": {
        "type": "long"
      },
      "depth": {
        "type": "long"
      },
      "product_name": {
        "type": "keyword",
        "fields": {
          "korean": {
            "type": "text",
            "analyzer": "korean"
          }
        }
      },
      "product_no": {
        "type": "long"
      },
      "product_price": {
        "type": "long"
      }
    }
  }
}


```


### 2. 데이터 수집
Logstash를 활용한 데이터 수집:

[Logstash pipeline conf 파일 확인](https://github.com/SunKyungCho/amore_search_test/blob/main/product_logstash.conf)

```javascript
input {
  jdbc {
    jdbc_driver_library =>"/Users/sunkyung/elasticsearch/logstash-7.12.0/mysql-connector-java-8.0.18.jar" 
    jdbc_driver_class => "com.mysql.jdbc.Driver"
    jdbc_connection_string => "jdbc:mysql://localhost:3306/test"
    jdbc_user => "root"
    jdbc_password => ""
    statement => "select * from product INNER JOIN category c on product.category_no = c.category_no"
  }
}

filter {}

output {
    elasticsearch {
      hosts => ["http://localhost:9200/"]
      index => "product"
    }
    stdout {
        #codec => rubydebug
        codec => "dots"
    }
}
```

### 3. 상품쿼리작성

1.`손크림` 검색시 `러빙데이즈핸드크림`도 같이 검색될 수 있도록 해야한다.:
동의어 사전을 등록을 통해 `손` 검색시 `핸드`도 같이 검색될 수 있도록 하였다. 
```text
손,핸드
```
다만 `손`과 한 글자를 동의어로 등록하는 것은 사이드 이팩트가 생길 수 있다. `손크림`, `핸드크림`을 사용자 사전에 등록하고 동의어 사전을 작성하는 방식도 고려해 볼수 있겠다. 

2.검색결과 리스트중 생활 용품 카테고리는 최상위리스트에 와야 한다.
funciton score query를 통한 해결하였다. function을 사용하여 `생활용품`의 가중치를 높게 작성하였다. 
쿼리는 아래와 같다. 
```javascript
{
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "should": [
            {
              "match": {
                "brand_name.korean": {
                  "query": "클렌징",
                  "operator": "and"
                }
              }
            },
            {
              "match": {
                "product_name.korean": {
                  "query": "클렌징",
                  "operator": "and"
                }
              }
            }
          ]
        }
      },
      "functions": [
        {
          "filter": {
            "term": {
              "category_name": "생활용품"
            }
          }, 
          "weight": 10000
        }
        ]
    }
  }
}
```
 
### API 
사용 기술: Java, Spring boot

소스를 받아 빌드하거나 릴리즈된 jar 파일을 [다운로드](https://github.com/SunKyungCho/amore_search_test/releases/tag/0.0.1) 하여 실행해볼 수 있다.

실행방법
```
java -jar amore_search_test-0.0.1.jar

```

호출 URL:
```javascript
http://localhost:8080/products?keyword=면세&pageSize=5
```

***Response:***
```javascript
{
    "products": [
        {
            "product_name": "ET.순정 스킨케어 세트(이지)(5EA)(면세)",
            "category_name": "생활용품",
            "product_price": 31400000
        },
        {
            "product_name": "ET.수분가득콜라겐3종세트(16년/면세)",
            "category_name": "생활용품",
            "product_price": 42000000
        },
        {
            "product_name": "ET.베이킹파우더B.B딥클렌징폼3종(면세)",
            "category_name": "생활용품",
            "product_price": 21600000
        },
        {
            "product_name": "ET.마이미니트래블키트(면세)",
            "category_name": "클랜징 티슈",
            "product_price": 15000000
        },
        {
            "product_name": "ET.스킨맑음2종세트(면세)",
            "category_name": "클랜징 티슈",
            "product_price": 24000000
        }
    ]
}
```
