package com.ashish.elasticsearch_playground.sec05;

import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggester;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import com.ashish.elasticsearch_playground.AbstractTest;
import com.ashish.elasticsearch_playground.sec05.entity.Garment;
import com.ashish.elasticsearch_playground.sec05.repository.GarmentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NativeAndCriteriaQueryTest extends AbstractTest {

    @Autowired
    private GarmentRepository repository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @BeforeAll
    public void dataSetup(){
        var garments = this.readResource("sec05/garments.json", new TypeReference<List<Garment>>() {
        });
        this.repository.saveAll(garments);
        Assertions.assertEquals(20,this.repository.count());

    }

    /*
    Sometimes we need to build a complex query programmatically with "and" "or" "not" conditions etc.
    We might not be able to use the hard coded @Query. In those cases, criteria query could be useful.
    */

    @Test
    public void criteriaQuery(){
        var nameIsShirt = Criteria.where("name").is("Shirt");
        this.verify(nameIsShirt,1);

        var priceAbove100 = Criteria.where("price").greaterThan(100);
        this.verify(priceAbove100,5);

        this.verify(nameIsShirt.or(priceAbove100),6);

        var brandIsZara = Criteria.where("brand").is("Zara").boost(3.0f);
        this.verify(priceAbove100.and(brandIsZara.not()),3);

        var fuzzyMatchShirt = Criteria.where("name").fuzzy("short");
        this.verify(fuzzyMatchShirt,1);

    }

    private void verify(Criteria criteria, int expectedResultsCount){
        var query = CriteriaQuery.builder(criteria).build();
        var searchHits = this.elasticsearchOperations.search(query, Garment.class);
        searchHits.forEach(this.print());
        Assertions.assertEquals(expectedResultsCount,searchHits.getTotalHits());
    }

    //We will build query for below query.
    /*{
        "query": {
        "bool": {
            "filter": [
            {
                "term": {
                "occasion": "Casual"
            }
            },
            {
                "range": {
                "price": {
                    "lte": 50
                }
            }
            }
      ],
            "should": [
            {
                "term": {
                "color": "Brown"
            }
            }
      ]
        }
    }
    }*/
    @Test
    public void boolQuery(){

        var occasionCasual = Query.of(b->b.term(
                TermQuery.of(tq -> tq.field("occasion").value("Casual"))
        ));

        var colorBrown = Query.of(b->b.term(
                TermQuery.of(tq->tq.field("color").value("Brown"))
        ));

        var priceBelow50 = Query.of(b->b.range(
                RangeQuery.of(rq->rq.number(
                        NumberRangeQuery.of(nrq->nrq.field("price").lte(50d))
                ))
        ));

        var query = Query.of(b->b.bool(
                BoolQuery.of(bq->bq.filter(occasionCasual,priceBelow50).should(colorBrown))
        ));


        var nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .build();

        var searchHits = this.elasticsearchOperations.search(nativeQuery, Garment.class);
        searchHits.forEach(this.print());
        Assertions.assertEquals(4,searchHits.getTotalHits());

    }

    //We will build aggregation query for below query.
    /*
    * {
  "size": 0,
  "aggs": {
    "price-stats": {
      "stats": {
        "field": "price"
      }
    },
    "group-by-brand": {
      "terms": {
        "field": "brand"
      }
    },
    "group-by-color": {
      "terms": {
        "field": "color"
      }
    },
    "price-range": {
      "range": {
        "field": "price",
        "ranges": [
          {
            "to": 50
          },
          {
            "from": 50,
            "to": 100
          },
          {
            "from": 100,
            "to": 150
          },
          {
            "from": 150
          }
        ]
      }
    }
  }
}*/

    @Test
    public void aggregation(){
        var priceStats = Aggregation.of(b->b.stats(
                StatsAggregation.of(sa->sa.field("price"))
        ));

        var brandTerms = Aggregation.of(b->b.terms(
                TermsAggregation.of(ta->ta.field("brand"))
        ));

        var colorTerms = Aggregation.of(b->b.terms(
                TermsAggregation.of(ta->ta.field("color"))
        ));

        var ranges = List.of(
                AggregationRange.of(b->b.to(50d)),
                AggregationRange.of(b->b.from(50d).to(100d)),
                AggregationRange.of(b->b.from(100d).to(150d)),
                AggregationRange.of(b->b.from(150d))
        );

        var priceRange = Aggregation.of(b->b.range(
                RangeAggregation.of(ra->ra.field("price").ranges(ranges))
        ));

        var nativeQuery = NativeQuery.builder()
                .withMaxResults(0) //size is 0
                .withAggregation("price-stats",priceStats)
                .withAggregation("group-by-brand",brandTerms)
                .withAggregation("group-by-color",colorTerms)
                .withAggregation("price-range",priceRange)
                .build();

        var searchHits = this.elasticsearchOperations.search(nativeQuery, Garment.class);
        var aggregations = (List<ElasticsearchAggregation>)searchHits.getAggregations().aggregations();

        var map = aggregations.stream()
                .map(ElasticsearchAggregation::aggregation)
                .collect(Collectors.toMap(
                        a->a.getName(),
                        a->a.getAggregate()
                ));

        this.print().accept(map);
        Assertions.assertEquals(4,map.size());

        Assertions.assertTrue(map.get("price-stats").isStats());
        Assertions.assertTrue(map.get("group-by-brand").isSterms());
        Assertions.assertTrue(map.get("group-by-color").isSterms());
        Assertions.assertTrue(map.get("price-range").isRange());

        if(map.get("group-by-brand").isSterms()){
            map.get("group-by-brand").sterms()
                    .buckets()
                    .array()
                    .stream()
                    .map(b->b.key().stringValue()+":"+b.docCount())
                    .forEach(this.print());
        }

    }

    //We will build below autocomplete query for below query
    /*
    {
  "suggest": {
    "product-suggest": {
      "prefix": "ca",
      "completion": {
          "field": "name.completion"
      }
    }
  },
  "_source": false
}
    */
    @Test
    public void suggestion(){
        var fieldSuggestor = FieldSuggester.of(b->b.prefix("ca").completion(
                CompletionSuggester.of(cs->cs.field("name.completion").skipDuplicates(true).size(10))
        ));

        var suggestor = Suggester.of(b->b.suggesters("product-suggest",fieldSuggestor));
        var query = NativeQuery.builder()
                .withSuggester(suggestor)
                .withMaxResults(0)
                .withSourceFilter(FetchSourceFilter.of(b->b.withExcludes("*")))
                .build();

        var searchHits = this.elasticsearchOperations.search(query, Garment.class);

        Assertions.assertNotNull(searchHits.getSuggest());

        var suggestions = searchHits.getSuggest().getSuggestion("product-suggest")
                .getEntries()
                .get(0)
                .getOptions()
                .stream()
                .map(Suggest.Suggestion.Entry.Option::getText)
                .collect(Collectors.toSet());

        Assertions.assertEquals(Set.of("Casual Wrap", "Casual Maxi"), suggestions);

    }

}
