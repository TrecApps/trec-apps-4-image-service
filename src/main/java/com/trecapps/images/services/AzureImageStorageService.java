package com.trecapps.images.services;

import com.azure.core.credential.AzureNamedKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobDownloadContentAsyncResponse;
import com.azure.storage.blob.models.BlockBlobItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trecapps.images.models.ImageRecord;
import com.trecapps.images.models.ImageState;
import com.trecapps.images.models.ObjectResponseException;
import com.trecapps.images.models.ResponseObj;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
public class AzureImageStorageService implements IImageStorageService {
    BlobServiceAsyncClient client;
    ObjectMapper objectMapper;
    BlobContainerAsyncClient publicContainerClient;
    BlobContainerAsyncClient hiddenContainerClient;

    void prepContainers(){
        this.publicContainerClient.createIfNotExists().subscribe();
        this.hiddenContainerClient.createIfNotExists().subscribe();
    }

    AzureImageStorageService(String name,
                   String key,
                   String endpoint,
                   String publicContainerName,
                   String hiddenContainerName,
                   ObjectMapper objectMapperBuilder) {
        AzureNamedKeyCredential credential = new AzureNamedKeyCredential(name, key);
        this.client = (new BlobServiceClientBuilder()).credential(credential).endpoint(endpoint).buildAsyncClient();
        this.publicContainerClient = client.getBlobContainerAsyncClient(publicContainerName);
        this.hiddenContainerClient = client.getBlobContainerAsyncClient(hiddenContainerName);
        this.objectMapper = objectMapperBuilder;
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        prepContainers();
    }

    AzureImageStorageService(
            ObjectMapper objectMapperBuilder,
            String publicContainerName,
            String hiddenContainerName,
            String endpoint
    ) {

        this.client = new BlobServiceClientBuilder()
                .credential(new DefaultAzureCredentialBuilder()
                        .build())
                .endpoint(endpoint)
                .buildAsyncClient();
        this.publicContainerClient = client.getBlobContainerAsyncClient(publicContainerName);
        this.hiddenContainerClient = client.getBlobContainerAsyncClient(hiddenContainerName);
        this.objectMapper = objectMapperBuilder;
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        prepContainers();
    }


    @Override
    public Mono<ResponseObj> persistImage(ImageRecord record, byte[] content) {
        boolean isPublic = record.getState().isUsePublic();
        BlobContainerAsyncClient containerClient =
                (isPublic) ? publicContainerClient : hiddenContainerClient;

        String name = getName(record);

        return containerClient.getBlobAsyncClient(name)
                .upload(BinaryData.fromBytes(content))
                .doOnNext((BlockBlobItem item) -> {
                    if(isPublic) record.setState(ImageState.PUBLIC);
                }).thenReturn(ResponseObj.getInstance("Success!", name));
    }

    @Override
    public Mono<ResponseObj> transferImage(ImageRecord record) {
        boolean isPublic = record.getState().isUsePublic();
        BlobContainerAsyncClient toContainerClient =
                (isPublic) ? publicContainerClient : hiddenContainerClient;
        BlobContainerAsyncClient fromContainerClient =
                (isPublic) ?  hiddenContainerClient: publicContainerClient;

        record.setState(isPublic ? ImageState.NON_ADULT : ImageState.PUBLIC);

        return retrieveImage(record, null)
                .flatMap((byte[] bytes) -> {
                    record.setState(isPublic ? ImageState.PUBLIC : ImageState.NON_ADULT);
                    return persistImage(record, bytes);
                })
                .flatMap((ResponseObj ignore) -> {
                    record.setState(isPublic ? ImageState.NON_ADULT : ImageState.PUBLIC);
                    return this.deleteImage(record);
                })
                .doOnNext((ResponseObj obj) -> {
                    record.setState(isPublic ? ImageState.PUBLIC : ImageState.NON_ADULT);
                });
    }

    @Override
    public Mono<byte[]> retrieveImage(ImageRecord record, String crop) {
        boolean isPublic = record.getState().isUsePublic();
        BlobContainerAsyncClient containerClient =
                (isPublic) ? publicContainerClient : hiddenContainerClient;

        String name = getName(record);

        return containerClient.getBlobAsyncClient(name)
                .downloadContentWithResponse(null, null)
                .map((BlobDownloadContentAsyncResponse response) -> {
                    int status = response.getStatusCode();
                    if(status == 404)
                        throw new ObjectResponseException(String.format("Image %s not found in Storage!", name), HttpStatus.NOT_FOUND);
                    if(status >= 400 && status < 500)
                    {
                        log.error("Status {} discovered with {} - error message: {}", status, name, response.getDeserializedHeaders().getErrorCode());
                        throw new ObjectResponseException(String.format("Image %s retrieval failure!", name), HttpStatus.BAD_REQUEST);
                    }
                    if(status >= 500) {
                        log.error("Status {} discovered with {} - error message: {}", status, name, response.getDeserializedHeaders().getErrorCode());
                        throw new ObjectResponseException(String.format("Image %s retrieval failure!", name), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    return response.getValue().toBytes();
                })
                .map((byte[] content) -> {
                    try {
                        return applyCrop(content, crop, record);
                    } catch(IOException e){
                        log.error("IO Exception parsing {}", name, e);
                        return content;
                    }
                });

    }

//    @Override
//    public Mono<byte[]> retrievePublicImage(String name, String crop) {
//        return publicContainerClient.getBlobAsyncClient(name)
//                .downloadContentWithResponse(null, null)
//                .map((BlobDownloadContentAsyncResponse response) -> {
//                    int status = response.getStatusCode();
//                    if(status == 404)
//                        throw new ObjectResponseException(String.format("Image %s not found in Storage!", name), HttpStatus.NOT_FOUND);
//                    if(status >= 400 && status < 500)
//                    {
//                        log.error("Status {} discovered with {} - error message: {}", status, name, response.getDeserializedHeaders().getErrorCode());
//                        throw new ObjectResponseException(String.format("Image %s retrieval failure!", name), HttpStatus.BAD_REQUEST);
//                    }
//                    if(status >= 500) {
//                        log.error("Status {} discovered with {} - error message: {}", status, name, response.getDeserializedHeaders().getErrorCode());
//                        throw new ObjectResponseException(String.format("Image %s retrieval failure!", name), HttpStatus.INTERNAL_SERVER_ERROR);
//                    }
//                    return response.getValue().toBytes();
//                }).map((byte[] content) -> {
//                    try {
//                        ImageRecord record = new Record();
//                        return applyCrop(content, crop, record);
//                    } catch(IOException e){
//                        log.error("IO Exception parsing {}", name, e);
//                        return content;
//                    }
//                });
//    }

    @Override
    public Mono<ResponseObj> deleteImage(ImageRecord record) {

        boolean isPublic = record.getState().isUsePublic();
        BlobContainerAsyncClient containerClient =
                (isPublic) ? publicContainerClient : hiddenContainerClient;

        String name = getName(record);
        return containerClient
                .getBlobAsyncClient(name)
                .deleteIfExists()
                .thenReturn(ResponseObj.getInstance(HttpStatus.OK, "Success"));
    }
}
