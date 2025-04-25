package com.trecapps.images.repos;

import com.trecapps.images.models.ImageProfile;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ProfileRepo extends ReactiveMongoRepository<ImageProfile, String> {
}
