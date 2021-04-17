# Amore_search_test
## 문제
* A 쇼핑몰에서는 10 가지 카테고리에 총 1000 개의 상품을 판매하고 있다.
* 카테고리란, 개별 상품이 속하는 상품 범주의 종류. 아래 그림에서 메이크업/스킨케어 등을 1 차 카테고리, 스킨토너/로션에멀젼 등을 2 차카테고리라고 하며, 본 문제에서의 카테고리는 "스킨케 어-스킨토너" 형태로 1,2 차 카테고리를 합친 문자열 형태를 갖는다.
* 하나의 개별 상품은 단 1개의 카테고리에만 속한다.
* 카테고리는 카테고리명이라는 단 1 개의 속성만 갖는다.
* 개별 상품은 상품명, 카테고리, 가격이라는 3 가지 속성을 가진다.

## 1. 데이터 Mapping Template
한글 노리 형태소 분석기를 활용한 elsticsearch mapping template 작성하였습니다. 사용자 사전을 통해 상품 도메인의 키워드를 등록하였으며, 추가로 synonym filter도 추가 하였습니다.

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


## 2. 데이터 수집/정제/적재
SQL Dump 데이터를 Elsticsearch 색인한다. 별도의 정제 과정은 현 과제에서는 불필요하다 생각하여 작성하지 않았습니다.  
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

## 3. 상품 쿼리 작성

### 1.`손크림` 검색시 `러빙데이즈핸드크림`도 같이 검색될 수 있도록 해야한다.:
동의어 사전을 등록을 통해 `손` 검색시 `핸드`도 같이 검색될 수 있도록 하였다. 
```text
손,핸드
```
다만 `손`과 한 글자를 동의어로 등록하는 것은 사이드 이팩트가 생길 수 있다. `손크림`, `핸드크림`을 사용자 사전에 등록하고 동의어 사전을 작성하는 방식도 고려해 볼수 있겠다. 

### 2.검색결과 리스트중 생활 용품 카테고리는 최상위리스트에 와야 한다.
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
 
### 4. API 
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
