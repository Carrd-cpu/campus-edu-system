package com.scaffold.controller;

import com.scaffold.common.Result;
import com.scaffold.service.FileUploadService;
import com.scaffold.service.ISysLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileUploadService fileUploadService;
    @Autowired
    private ISysLogService sysLogService;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile file) {
        String url = fileUploadService.upload(file);
        String name = file == null ? "" : file.getOriginalFilename();
        sysLogService.logOperation(request, "文件管理", "上传文件",
                "上传文件：" + (name == null || name.isBlank() ? url : name) + " → " + url);
        return Result.ok(Map.of("url", url));
    }
}
