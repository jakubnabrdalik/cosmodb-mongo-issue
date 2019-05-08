package com.vattenfall.reactivemongodb;

import com.mongodb.WriteConcern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteConcernResolver;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StopWatch;
import reactor.core.publisher.Flux;

import static java.lang.System.out;


/**
 * Without tweaks, breaks with cosmodb at 50 objects.
 *
 * There might be several reasons. First is that each insert is created in a different connection (limited at 500 by default)
 * as described here:
 * https://github.com/spring-projects/spring-framework/issues/22332
 * and here
 * https://github.com/spring-projects/spring-framework/issues/20338
 *
 * We can limit the number of concurrent connections on the driver level by adding "&maxPoolSize=10" to db.url
 *
 * When we limit the number of connections, we can insert 50 objects.
 *
 * But when we move to 500, we get "Request rate is large" even though RU/s is set to almost 10k
 * https://blogs.msdn.microsoft.com/bigdatasupport/2015/09/02/dealing-with-requestratetoolarge-errors-in-azure-documentdb-and-testing-performance/
 *
 * We switch the indexing to Lazy, to make sure we don't saturate the RU/s
 * az cosmosdb collection update \
 *     --resource-group-name emobility-platf-rg-dev \
 *     --name emobility-mongodb-dev1 \
 *     --db-name roaming-evse-exporter \
 *     --collection-name TestObject		 \
 *     --indexing-policy "{\"indexingMode\": \"lazy\", \"includedPaths\": [{ \"path\": \"/*\" }] }"
 *
 * The 500 inserts break with a message:
 * "The MAC signature found in the HTTP request is not the same as the computed signature. Server used following string to sign..."
 *
 */
@SpringBootApplication
public class ReactiveMongodbApplication {

    @Bean
    public WriteConcernResolver writeConcernResolver() {
        return action -> {
            System.out.println("Using Write Concern of Acknowledged");
            return WriteConcern.ACKNOWLEDGED;
        };
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ReactiveMongodbApplication.class, args);
        MongoTemplate mongoTemplate = context.getBean("mongoTemplate", MongoTemplate.class);
        int numberOfObjects = 500;
        mongoTemplate.dropCollection(DomainObject.collectionName);
        ReactiveRepo reactiveRepo = context.getBean("reactiveRepo", ReactiveRepo.class);
        reactiveTest(reactiveRepo, mongoTemplate, numberOfObjects);
        context.stop();
        System.exit(0);
    }

    private static void reactiveTest(ReactiveRepo reactiveRepo, MongoTemplate mongoTemplate, int numberOfObjects) {
        printHeader("Starting reactive test");
        StopWatch stopWatch = new StopWatch("reactive test");
        stopWatch.start();
        Flux.range(1, numberOfObjects)
                .flatMap(integer -> reactiveRepo.insert(DomainObject.create()))
                .blockLast();
        stopWatch.stop();
        printFooter(numberOfObjects, mongoTemplate, stopWatch);
    }

    private static void printHeader(String s) {
        out.println(s);
    }

    private static void printFooter(int numberOfObjects, MongoTemplate mongoTemplate, StopWatch stopWatch) {
        out.println(stopWatch.prettyPrint());
        out.println();
        out.println("Avg time per insert (ms): " + stopWatch.getTotalTimeMillis() / numberOfObjects);
        long foundObjects = mongoTemplate.count(Query.query(Criteria.where("_id").exists(true)), DomainObject.collectionName);
        out.println("Found " + foundObjects + " objects");
    }
}
