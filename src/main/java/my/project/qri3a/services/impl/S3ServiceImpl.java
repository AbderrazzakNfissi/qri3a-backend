package my.project.qri3a.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.project.qri3a.services.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Override
    public String uploadFile(MultipartFile file, String key) throws IOException {
        log.info("Uploading file to S3: {}", key);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        String fileUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
        log.info("File uploaded to S3: {}", fileUrl);
        return fileUrl;
    }

    @Override
    public byte[] getFile(String key) {
        log.info("Retrieving file from S3: {}", key);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
        return objectBytes.asByteArray();
    }

    @Override
    public void deleteFile(String key) {
        log.info("Deleting file from S3: {}", key);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("File deleted from S3: {}", key);
    }

    @Override
    public List<S3Object> listFiles() {
        log.info("Listing files in S3 bucket: {}", bucketName);
        ListObjectsV2Request listObjects = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Response res = s3Client.listObjectsV2(listObjects);
        return res.contents();
    }
}
