package com.dtao.lms.config;

import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class MongoIndexCreator {

    private final MongoTemplate mongoTemplate;

    public MongoIndexCreator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        // TTL index on "expiresAt" with expireAfterSeconds = 0 (expire at the datetime value)
        Index ttlIndex = new Index()
                .on("expiresAt", Sort.Direction.ASC)
                .expire(0); // expireAfterSeconds = 0 -> remove when expiresAt is reached

        // create the index (createIndex is the non-deprecated replacement of ensureIndex)
        mongoTemplate.indexOps("email_verifications").createIndex(ttlIndex);
    }
}
