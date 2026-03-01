package com.example.demo.controller.common;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.common.StorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "Upload Controller", description = "API upload hình ảnh")
@Slf4j(topic ="FILE-UPLOAD-CONTROLLER" )
public class FileUploadController {

    private final StorageService storageService;

    @Operation(summary = "Upload Avatar", description = "Upload ảnh đại diện (Lưu vào folder avatars)")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = storageService.uploadFile(file, "avatars");
        return ResponseEntity.ok(Map.of("url", url));
    }

    @Operation(summary = "Upload Product Image", description = "Upload ảnh sản phẩm (Lưu vào folder products)")
    @PostMapping(value = "/product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> uploadProductImage(@RequestParam("file") MultipartFile file) {
        String url = storageService.uploadFile(file, "products");
        return ResponseEntity.ok(Map.of("url", url));
    }
}
