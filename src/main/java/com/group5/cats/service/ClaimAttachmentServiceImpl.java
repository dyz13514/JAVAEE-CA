package com.group5.cats.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.group5.cats.model.ClaimAttachment;
import com.group5.cats.repository.ClaimAttachmentRepository;

@Service
public class ClaimAttachmentServiceImpl implements ClaimAttachmentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final ClaimAttachmentRepository attachmentRepository;
    private final Path uploadDirectory;

    public ClaimAttachmentServiceImpl(ClaimAttachmentRepository attachmentRepository,
            @Value("${cats.claims.upload-directory:./uploads/claims}") String uploadDirectory) {
        this.attachmentRepository = attachmentRepository;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public String validateUpload(MultipartFile file, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required) {
                return "Please attach a file.";
            }
            return null;
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return "Each attachment must be no larger than 10 MB.";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "The attachment must have a filename.";
        }
        String filename = originalFilename.toLowerCase(Locale.ROOT);
        String detectedType;
        try (InputStream input = file.getInputStream()) {
            detectedType = detectContentType(input.readNBytes(8));
        } catch (IOException exception) {
            return "The attachment could not be read. Please upload it again.";
        }

        if ("application/pdf".equals(detectedType) && filename.endsWith(".pdf")) {
            return null;
        }
        if ("image/png".equals(detectedType) && filename.endsWith(".png")) {
            return null;
        }
        if ("image/jpeg".equals(detectedType)
                && (filename.endsWith(".jpg") || filename.endsWith(".jpeg"))) {
            return null;
        }
        return "Only PDF, PNG and JPEG attachments are accepted. The file contents must match its extension.";
    }

    @Override
    public ClaimAttachment store(MultipartFile file) {
        String error = validateUpload(file, true);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Attachments must be saved together with a claim.");
        }

        try {
            byte[] contents;
            try (InputStream input = file.getInputStream()) {
                contents = input.readNBytes((int) MAX_FILE_SIZE + 1);
            }
            if (contents.length > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Each attachment must be no larger than 10 MB.");
            }
            String contentType = detectContentType(contents);
            String extension;
            if ("application/pdf".equals(contentType)) {
                extension = ".pdf";
            } else if ("image/png".equals(contentType)) {
                extension = ".png";
            } else if ("image/jpeg".equals(contentType)) {
                extension = ".jpg";
            } else {
                throw new IllegalArgumentException("The attachment contents are invalid.");
            }

            String storedFilename = UUID.randomUUID().toString() + extension;
            Path storedPath = resolvePath(storedFilename);
            Files.createDirectories(uploadDirectory);
            Files.createFile(storedPath);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        try {
                            Files.deleteIfExists(storedPath);
                        } catch (IOException exception) {
                            System.getLogger(ClaimAttachmentServiceImpl.class.getName())
                                    .log(System.Logger.Level.WARNING, "Could not remove a rolled-back claim attachment.", exception);
                        }
                    }
                }
            });

            Files.write(storedPath, contents, StandardOpenOption.WRITE);

            ClaimAttachment attachment = new ClaimAttachment();
            attachment.setOriginalFilename(cleanFilename(file.getOriginalFilename()));
            attachment.setStoredFilename(storedFilename);
            attachment.setContentType(contentType);
            attachment.setFileSize(contents.length);
            attachment.setUploadedAt(LocalDateTime.now());
            return attachmentRepository.save(attachment);
        } catch (IOException exception) {
            throw new IllegalStateException("The attachment could not be saved. Please try again.", exception);
        }
    }

    @Override
    public Resource load(ClaimAttachment attachment) {
        Path storedPath = resolvePath(attachment.getStoredFilename());
        if (!Files.isRegularFile(storedPath) || !Files.isReadable(storedPath)) {
            throw new IllegalArgumentException("The attachment file is not available.");
        }
        return new FileSystemResource(storedPath);
    }

    private Path resolvePath(String storedFilename) {
        Path storedPath = uploadDirectory.resolve(storedFilename).normalize();
        if (!storedPath.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("The attachment filename is invalid.");
        }
        return storedPath;
    }

    private String cleanFilename(String originalFilename) {
        String filename = originalFilename.replace('\\', '/');
        int separator = filename.lastIndexOf('/');
        if (separator >= 0) {
            filename = filename.substring(separator + 1);
        }
        filename = filename.replaceAll("[\\p{Cntrl}]", "_");
        if (filename.length() > 180) {
            filename = filename.substring(filename.length() - 180);
        }
        return filename;
    }

    private String detectContentType(byte[] contents) {
        if (contents.length >= 5 && contents[0] == '%' && contents[1] == 'P'
                && contents[2] == 'D' && contents[3] == 'F' && contents[4] == '-') {
            return "application/pdf";
        }
        if (contents.length >= 8 && (contents[0] & 255) == 137 && contents[1] == 'P'
                && contents[2] == 'N' && contents[3] == 'G' && contents[4] == 13
                && contents[5] == 10 && contents[6] == 26 && contents[7] == 10) {
            return "image/png";
        }
        if (contents.length >= 3 && (contents[0] & 255) == 255
                && (contents[1] & 255) == 216 && (contents[2] & 255) == 255) {
            return "image/jpeg";
        }
        return null;
    }
}
