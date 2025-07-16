package com.ashish.elasticsearch_playground.sec03.repository;

import com.ashish.elasticsearch_playground.sec03.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProductRepository extends ElasticsearchRepository<Product,Integer> {

    SearchHits<Product> findByCategory(String category);

    SearchHits<Product> findByCategoryIn(List<String> categories);

    SearchHits<Product> findByCategoryAndBrand(String category,String brand);

    SearchHits<Product> findByName(String name);

    SearchHits<Product> findByPriceLessThan(Integer price);

    SearchHits<Product> findByPriceBetween(Integer from, Integer to, Sort sort);

    //Pagination Demo
    SearchPage<Product> findByCategory(String category, Pageable pageable);

}
