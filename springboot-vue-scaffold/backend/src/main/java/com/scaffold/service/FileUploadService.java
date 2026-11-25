package com.scaffold.service;

import com.scaffold.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = resolveExtension(originalFilename, file.getContentType());
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持图片（jpg/png/gif/webp/svg）");
        }

        try {
            Path dir = resolveUploadDir();
            String filename = UUID.randomUUID() + "." + extension;
            Path target = dir.resolve(filename);
            // 使用 Files.copy，避免 Windows 下 MultipartFile.transferTo 路径问题
            try (var in = file.getInputStream()) {
                Files.copy(in, target);
            }
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new BusinessException("文件上传失败：" + ex.getMessage());
        }
    }

    private Path resolveUploadDir() throws IOException {
        Path dir = Paths.get(uploadPath).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (IMAGE_EXTENSIONS.contains(ext)) {
                return ext;
            }
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase();
            if (ct.contains("jpeg") || ct.contains("jpg")) {
                return "jpg";
            }
            if (ct.contains("png")) {
                return "png";
            }
            if (ct.contains("gif")) {
                return "gif";
            }
            if (ct.contains("webp")) {
                return "webp";
            }
            if (ct.contains("svg")) {
                return "svg";
            }
        }
        return "jpg";
    }
}
