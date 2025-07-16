package com.ashish.elasticsearch_playground.sec05.repository;

import com.ashish.elasticsearch_playground.sec05.entity.Garment;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarmentRepository extends ElasticsearchRepository<Garment, String> {
}
