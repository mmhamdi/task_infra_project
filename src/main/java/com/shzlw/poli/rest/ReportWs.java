package com.shzlw.poli.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shzlw.poli.config.AppProperties;
import com.shzlw.poli.dao.*;
import com.shzlw.poli.dto.ExportRequest;
import com.shzlw.poli.metrics.CustomMetrics;
import com.shzlw.poli.model.Report;
import com.shzlw.poli.model.SharedReport;
import com.shzlw.poli.model.User;
import com.shzlw.poli.service.HttpClient;
import com.shzlw.poli.service.ReportService;
import com.shzlw.poli.service.SharedReportService;
import com.shzlw.poli.util.CommonUtils;
import com.shzlw.poli.util.Constants;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/ws/reports")
public class ReportWs {

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private ComponentDao componentDao;

    @Autowired
    private ReportService reportService;

    @Autowired
    private SharedReportDao sharedReportDao;

    @Autowired
    private UserFavouriteDao userFavouriteDao;

    @Autowired
    private SharedReportService sharedReportService;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Tracer tracer;

    @Autowired
    private CustomMetrics customMetrics;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public List<Report> findAll(HttpServletRequest request) {
        long startTime = System.nanoTime();
        Span span = tracer.spanBuilder("ReportWs.findAll").startSpan();
        List<Report> reports = null;
        try {
            User user = (User) request.getAttribute(Constants.HTTP_REQUEST_ATTR_USER);
            reports = reportService.getReportsByUser(user);
            customMetrics.getRequestCounter().add(1);
        } catch (Exception e) {
            customMetrics.getErrorCounter().add(1);
        } finally {
            span.end();
        }
        return reports;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public Report findOneById(@PathVariable("id") long id, HttpServletRequest request) {
        long startTime = System.nanoTime();
        Span span = tracer.spanBuilder("ReportWs.findOneById").startSpan();
        Report report = null;
        try {
            List<Report> reports = findAll(request);
            report = reports.stream().filter(d -> d.getId() == id).findFirst().orElse(null);
            if (report != null) {
                User user = (User) request.getAttribute(Constants.HTTP_REQUEST_ATTR_USER);
                boolean isFavourite = userFavouriteDao.isFavourite(user.getId(), report.getId());
                report.setFavourite(isFavourite);
            }
            customMetrics.getRequestCounter().add(1);
        } catch (Exception e) {
            customMetrics.getErrorCounter().add(1);
        } finally {
            span.end();
        }
        return report;
    }

    @RequestMapping(value = "/name/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public Report findOneByName(@PathVariable("name") String name, HttpServletRequest request) {
        long startTime = System.nanoTime();
        Span span = tracer.spanBuilder("ReportWs.findOneByName").startSpan();
        Report report = null;
        try {
            List<Report> reports = findAll(request);
            report = reports.stream().filter(d -> d.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (report != null) {
                User user = (User) request.getAttribute(Constants.HTTP_REQUEST_ATTR_USER);
                boolean isFavourite = userFavouriteDao.isFavourite(user.getId(), report.getId());
                report.setFavourite(isFavourite);
            }
            customMetrics.getRequestCounter().add(1);
        } catch (Exception e) {
            customMetrics.getErrorCounter().add(1);
        } finally {
            long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            span.end();
        }
        return report;
    }

    @RequestMapping(value = "/sharekey/{shareKey}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public Report findOneBySharekey(@PathVariable("shareKey") String shareKey) {
        long startTime = System.nanoTime();
        Span span = tracer.spanBuilder("ReportWs.findOneBySharekey").startSpan();
        Report report = null;
        try {
            SharedReport sharedReport = sharedReportDao.findByShareKey(shareKey);
            if (sharedReport != null && sharedReport.getExpiredBy() >= CommonUtils.toEpoch(LocalDateTime.now())) {
                report = reportDao.findById(sharedReport.getReportId());
            }
            customMetrics.getRequestCounter().add(1);
        } catch (Exception e) {
            customMetrics.getErrorCounter().add(1);
        } finally {
            span.end();
        }
        return report;
    }

    @RequestMapping(method = RequestMethod.POST)
    @Transactional
    public ResponseEntity<Long> add(@RequestBody Report report, HttpServletRequest request) {
        long startTime = System.nanoTime();
        Span span = tracer.spanBuilder("ReportWs.add").startSpan();
        try {
            User user = (User) request.getAttribute(Constants.HTTP_REQUEST_ATTR_USER);
            reportService.invalidateCache(user.getId());
            long id = reportDao.insert(report.getName(), report.getStyle(), report.getProject());
            customMetrics.getRequestCounter().add(1);
            customMetrics.getServiceMethodCounter().add(1);
            customMetrics.getServiceOperationSuccessCounter().add(1);
            return new ResponseEntity<>(id, HttpStatus.CREATED);
        } catch (Exception e) {
            customMetrics.getServiceOperationFailureCounter().add(1);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            span.end();
        }
    }

    @RequestMapping(method = RequestMethod.PUT)
    @Transactional
    public ResponseEntity<?> update(@RequestBody Report report, HttpServletRequest request) {
        long startTime = System.nanoTime();
        Span span = tracer.spanBuilder("ReportWs.update").startSpan();
        try {
            User user = (User) request.getAttribute(Constants.HTTP_REQUEST_ATTR_USER);
            reportService.invalidateCache(user.getId());
            reportDao.update(report);
            customMetrics.getRequestCounter().add(1);
            customMetrics.getServiceMethodCounter().add(1);
            customMetrics.getServiceOperationSuccessCounter().add(1);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            customMetrics.getServiceOperationFailureCounter().add(1);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            span.end();
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public ResponseEntity<?> delete(@PathVariable("id") long reportId, HttpServletRequest request) {
        long startTime = System.nanoTime();
        Span span = tracer.spanBuilder("ReportWs.delete").startSpan();
        try {
            User user = (User) request.getAttribute(Constants.HTTP_REQUEST_ATTR_USER);
            reportService.invalidateCache(user.getId());
            sharedReportService.invalidateSharedLinkInfoCacheByReportId(reportId);
            sharedReportDao.deleteByReportId(reportId);
            userFavouriteDao.deleteByReportId(reportId);
            componentDao.deleteByReportId(reportId);
            reportDao.delete(reportId);
            customMetrics.getRequestCounter().add(1);
            customMetrics.getServiceMethodCounter().add(1);
            customMetrics.getServiceOperationSuccessCounter().add(1);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            customMetrics.getServiceOperationFailureCounter().add(1);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            span.end();
        }
    }

    @RequestMapping(value = "/favourite/{id}/{status}", method = RequestMethod.POST)
    @Transactional
    public void updateFavourite(@PathVariable("id") long reportId,
                                @PathVariable("status") String status,
                                HttpServletRequest request) {
        User user = (User) request.getAttribute(Constants.HTTP_REQUEST_ATTR_USER);
        long userId = user.getId();
        if (status.equals("add")) {
            if (!userFavouriteDao.isFavourite(userId, reportId)) {
                userFavouriteDao.insertFavourite(userId, reportId);
            }
        } else {
            userFavouriteDao.deleteFavourite(userId, reportId);
        }
    }

    @RequestMapping(value = "/favourite", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public List<Report> findAllFavourites(HttpServletRequest request) {
        User user = (User) request.getAttribute(Constants.HTTP_REQUEST_ATTR_USER);
        return reportDao.findFavouritesByUserId(user.getId());
    }

    @RequestMapping(
            value = "/pdf",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<?> exportToPdf(@RequestBody ExportRequest exportRequest,
                                         HttpServletRequest request) {

        User user = (User) request.getAttribute(Constants.HTTP_REQUEST_ATTR_USER);
        exportRequest.setSessionKey(user.getSessionKey());

        try {
            byte[] pdfData = httpClient.postJson(appProperties.getExportServerUrl(), mapper.writeValueAsString(exportRequest));
            ByteArrayResource resource = new ByteArrayResource(pdfData);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + exportRequest.getReportName() + ".pdf");
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentLength(pdfData.length)
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(resource);
        } catch (IOException e) {
            return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
        }
    }
}
