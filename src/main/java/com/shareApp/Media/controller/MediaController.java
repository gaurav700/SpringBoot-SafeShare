package com.shareApp.Media.controller;

import com.shareApp.Media.model.Media;
import com.shareApp.Media.service.MediaService;
import com.shareApp.Utils.security.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final JWTService jwtService;

    @PostMapping("/upload")
    public ResponseEntity<Media> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authToken
    ) {
        String userId = jwtService.validateTokenAndGetUserId(authToken);
        Media media = mediaService.upload(userId, file);
        return ResponseEntity.ok(media);
    }

    @GetMapping
    public ResponseEntity<Page<Media>> list(
            HttpServletRequest request,
            @RequestHeader("Authorization") String authToken,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        JWTService service = new JWTService();
        String userId = jwtService.validateTokenAndGetUserId(authToken);
        return ResponseEntity.ok(mediaService.listUserMedia(userId, page, size));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @RequestHeader("Authorization") String authToken,
            @PathVariable String id
    ) {
        String userId = jwtService.validateTokenAndGetUserId(authToken);
        Resource resource = mediaService.download(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
