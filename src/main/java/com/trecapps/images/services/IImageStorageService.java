package com.trecapps.images.services;

import com.trecapps.images.models.ImageRecord;
import com.trecapps.images.models.ResponseObj;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Service
public interface IImageStorageService {

    default String imageType(ImageRecord record){
        String[] elements = record.getType().split("/");
        return elements[elements.length-1];
    }

    default boolean canUseCrop(String crop, ImageRecord record)
    {
        if(crop == null)
            return true;    // Simply means no cropping will be done

        if(!HelperMethods.isCropFormatted(crop))
            return false;                       // Formatting is incorrect - don't use
        String[] cropParam = crop.split(",");
        int[] intParams = new int[4];

        for(int c = 0; c < cropParam.length && c < intParams.length; c++){
            intParams[c] = Integer.parseInt(cropParam[c]);

            if(intParams[c] < 0)
                return false;

        }

        if(intParams[0] + intParams[2] > record.getWidth())
            return false;
        return intParams[1] + intParams[3] <= record.getHeight();
    }

    default byte[] applyCrop(byte[] image, String crop, ImageRecord record) throws IOException {
        if(!canUseCrop(crop, record)) return image;

        String[] cropParam = crop.split(",");
        int[] intParams = new int[4];

        for(int c = 0; c < cropParam.length && c < intParams.length; c++){
            intParams[c] = Integer.parseInt(cropParam[c]);
        }
        InputStream inputStream = new ByteArrayInputStream(image);
        BufferedImage sourceImage = ImageIO.read(inputStream);

        BufferedImage croppedImage = sourceImage.getSubimage(intParams[0], intParams[1], intParams[2], intParams[3]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        boolean worked = ImageIO.write(croppedImage, imageType(record),outputStream);
        inputStream.close();
        if(!worked)
            throw new IOException("Failed to find image writer!");
        byte[] ret = outputStream.toByteArray();
        outputStream.close();
        return ret;
    }

    default Mono<String> retrieveImageBase64(ImageRecord record, String crop){
        return retrieveImage(record, crop)
                .map((byte[] contents) -> {
                    String data = Base64.getEncoder().encodeToString(contents);
                    return String.format("data:%s;base64,%s", record.getType(), data);
                });
    }

    default String getName(ImageRecord record){
        return  String.format("%s.%s", record.getId(), imageType(record));
    }

    Mono<ResponseObj> persistImage(ImageRecord record, byte[] content);

    Mono<ResponseObj> transferImage(ImageRecord record);

    Mono<byte[]> retrieveImage(ImageRecord record, String crop);

    //Mono<byte[]> retrievePublicImage(String name, String crop);

    Mono<ResponseObj> deleteImage(ImageRecord record);



}
