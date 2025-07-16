package com.ashish.elasticsearch_playground.sec04;

import com.ashish.elasticsearch_playground.AbstractTest;
import com.ashish.elasticsearch_playground.sec04.entity.Article;
import com.ashish.elasticsearch_playground.sec04.repository.ArticleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class QueryAnnotationTest extends AbstractTest {

    @Autowired
    private ArticleRepository repository;

    @BeforeAll
    public void dataSetup(){
        var articles = this.readResource("sec04/articles.json", new TypeReference<List<Article>>() {
        });
        this.repository.saveAll(articles);
        Assertions.assertEquals(11,this.repository.count());
    }

    @Test
    public void searchArticles(){
        var searchHits = this.repository.search("spring seasan"); //intentionally misspelled to check fuzziness
        searchHits.forEach(this.print());
        Assertions.assertEquals(4,searchHits.getTotalHits());
    }
}
