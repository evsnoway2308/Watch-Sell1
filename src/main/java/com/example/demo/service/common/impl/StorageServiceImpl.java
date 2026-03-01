package com.example.demo.service.common.impl;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.exception.AppException;
import com.example.demo.service.common.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "STORAGE-SERVICE")
public class StorageServiceImpl implements StorageService {
    private final Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file, String folderName) {
        try {

            if (file.isEmpty()) {
                throw new AppException("Không thể upload file rỗng");
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new AppException("Chỉ cho phép upload file ảnh");
            }

            String fileName = UUID.randomUUID().toString();
            Map<String, Object> params = ObjectUtils.asMap(
                    "public_id", fileName,
                    "folder", folderName,
                    "resource_type", "image");

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            
            String url = (String) uploadResult.get("secure_url");
            log.info("Upload success: {}", url);
            return url;

        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage());
            throw new AppException("Lỗi khi upload ảnh lên Cloudinary");
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteFile'");
    }

}