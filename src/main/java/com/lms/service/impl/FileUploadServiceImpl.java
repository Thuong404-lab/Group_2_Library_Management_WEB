package com.lms.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.lms.exception.FileStorageException;
import com.lms.service.FileUploadService;
import com.lms.service.LocalizedMessageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class FileUploadServiceImpl implements FileUploadService {


    private final LocalizedMessageService messages;

    private final Cloudinary cloudinary;

    public FileUploadServiceImpl(Cloudinary cloudinary, LocalizedMessageService messages) {
        this.cloudinary = cloudinary;
        this.messages = messages;
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            return null;
        }

        try {
            // Upload ảnh lên Cloudinary
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

            // Trả về đường dẫn bảo mật (HTTPS) của ảnh trên Cloudinary
            return uploadResult.get("secure_url").toString();

        } catch (IOException ex) {
            throw new FileStorageException(messages.get("backend.file.uploadFailed"), ex);
        }
    }
}
