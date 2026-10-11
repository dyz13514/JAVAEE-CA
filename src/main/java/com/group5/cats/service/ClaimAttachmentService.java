package com.group5.cats.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.group5.cats.model.ClaimAttachment;

public interface ClaimAttachmentService {

    String validateUpload(MultipartFile file, boolean required);

    ClaimAttachment store(MultipartFile file);

    Resource load(ClaimAttachment attachment);
}
