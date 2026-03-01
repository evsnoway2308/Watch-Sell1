package com.example.demo.service.common;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String uploadFile(MultipartFile file, String folderName);
    
    void deleteFile(String fileUrl);

}