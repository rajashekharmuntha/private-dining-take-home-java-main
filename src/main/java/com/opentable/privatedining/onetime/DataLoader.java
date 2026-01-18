package com.opentable.privatedining.onetime;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.opentable.privatedining.model.Reservation;
import com.opentable.privatedining.model.Restaurant;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DataLoader implements ApplicationRunner {
	
    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final MongoTemplate mongoTemplate;

    public DataLoader(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var yaml = new ClassPathResource("init-db.yml");

        log.info("Starting data dump...");
        long restaurantCount = mongoTemplate.count(new Query(), Restaurant.class);
        long reservationCount = mongoTemplate.count(new Query(), Reservation.class);

        if (restaurantCount > 0 || reservationCount > 0) {
        	log.info("Data already exists. Skipping import.");
            return;
        }

        log.info("Starting data dump...1");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(df));

        YAMLMapper yamlMapper = new YAMLMapper();
        yamlMapper.registerModule(module);
        try (var inputStream = yaml.getInputStream()) {
            var data = yamlMapper.readValue(inputStream, Data.class);

            mongoTemplate.dropCollection(Restaurant.class);
            mongoTemplate.dropCollection(Reservation.class);
            
            log.info("Starting data dump...2");
            mongoTemplate.indexOps(Reservation.class).ensureIndex(
                    new CompoundIndexDefinition(new Document()
                        .append("customerEmail", 1)
                        .append("restaurantId", 1)
                        .append("spaceId", 1)
                        .append("startTime", 1))
                    .unique()
                );

            mongoTemplate.insertAll(data.getRestaurants());
            mongoTemplate.insertAll(data.getReservations());
            
            long count = mongoTemplate.count(new Query(), Restaurant.class);
            log.info("Data dump complete. Total restaurants in DB: {}", count);
            
            count = mongoTemplate.count(new Query(), Reservation.class);
            log.info("Data dump complete. Total reservations in DB: {}", count);
        }catch(Exception e) {
        	 log.info("Starting data dump...Final", e);
        }
    }
}
