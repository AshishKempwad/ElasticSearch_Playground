package com.ashish.elasticsearch_playground.sec04.repository;

import com.ashish.elasticsearch_playground.sec04.entity.Article;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.HighlightField;
import org.springframework.data.elasticsearch.annotations.HighlightParameters;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/*
 * @Query -> { "query" : .... }
 * Do NOT provide query like this: { "query": { "match": {..}}}
 * Instead, provide only this { "match": {..}}
 * Parameters reference:
 *   ?0, ?1 --> refers to the first and second parameters respectively
 *   #{#paramter-name} --> we can also use the method parameter name to access
 * */


@Repository
public interface ArticleRepository extends ElasticsearchRepository<Article, String> {

    @Query("""
            {
                "multi_match": {
                  "query": "#{#searchTerm}",
                  "fields": [ "title^3", "body" ],
                  "operator": "and",
                  "fuzziness": 1,
                  "type": "best_fields",
                  "tie_breaker": 0.7
                }
            }
             """)
    @Highlight(fields = {
            @HighlightField(
                    name = "body",
                    parameters = @HighlightParameters(
                            preTags = "<b>",
                            postTags = "</b>"
                    )
            )
    })
    SearchHits<Article> search(String searchTerm);
}
