package com.wcm.campaign.controller;

import com.wcm.campaign.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<ByteArrayResource> export(@RequestParam(defaultValue = "ALL") String range
    ) {

        logger.info("Exporting campaign report with range={}", range);

        byte[] report = reportService.exportCampaignReport(range);
        String filename = "campaign-report-" + range.toLowerCase() + ".xlsx";

        logger.info("Campaign report generated successfully with size={} bytes", report.length);

        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename)
                        .build()
                        .toString())
                .contentLength(report.length)
                .body(new ByteArrayResource(report));
    }
}