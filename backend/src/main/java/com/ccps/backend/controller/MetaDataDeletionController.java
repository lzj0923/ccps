package com.ccps.backend.controller;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ccps.backend.service.MetaDataDeletionService;

@RestController
@RequestMapping("/meta/data-deletion")
public class MetaDataDeletionController {
    private final MetaDataDeletionService service;
    public MetaDataDeletionController(MetaDataDeletionService service) { this.service = service; }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> receive(@RequestParam(name = "signed_request", required = false) String signedRequest) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.receive(signedRequest));
    }

    @GetMapping(value = "/status/{code}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> status(@PathVariable("code") String code) {
        String message = service.publicStatus(code);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow, noarchive")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")
                // Only fixed messages from the service are rendered; no user ID, submitted payload or review note.
                .body("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                        + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                        + "<title>CCPS Data Deletion Request Status</title></head><body><main>"
                        + "<h1>CCPS Data Deletion Request Status / 资料删除申请状态</h1><p>" + message
                        + "</p><p>Contact / 联系邮箱：qq2290715152@gmail.com</p></main></body></html>");
    }
}
