package com.trecapps.images.repos;

import com.trecapps.images.models.ImageRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ImageRepo extends ReactiveMongoRepository<ImageRecord, String> {

    @Query("{'owner': owner, 'album': album}")
    Flux<ImageRecord> findByOwnerAndAlbum(String owner, String album, Pageable page);

    @Query("{'owner': owner, 'app': app}")
    Flux<ImageRecord> findByOwnerAndApp(String owner, String app, Pageable page);

    @Query("{'owner': owner, 'app': app, 'album': album}")
    Flux<ImageRecord> findByOwnerAppAndAlbum(String owner, String app, String album, Pageable page);

    Flux<ImageRecord> findByOwner(String owner, Pageable page);
}
