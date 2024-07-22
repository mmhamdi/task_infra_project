package com.shzlw.poli.rest;

import com.shzlw.poli.dao.ComponentDao;
import com.shzlw.poli.dao.JdbcDataSourceDao;
import com.shzlw.poli.dao.SavedQueryDao;
import com.shzlw.poli.dto.Table;
import com.shzlw.poli.model.JdbcDataSource;
import com.shzlw.poli.service.JdbcDataSourceService;
import com.shzlw.poli.service.JdbcQueryService;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.List;

@RestController
@RequestMapping("/ws/jdbcdatasources")
public class JdbcDataSourceWs {

    @Autowired
    Tracer tracer ;

    @Autowired
    JdbcDataSourceDao jdbcDataSourceDao;

    @Autowired
    JdbcQueryService jdbcQueryService;

    @Autowired
    JdbcDataSourceService jdbcDataSourceService;

    @Autowired
    ComponentDao componentDao;

    @Autowired
    SavedQueryDao savedQueryDao;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public List<JdbcDataSource> findAll() {
        Span span = tracer.spanBuilder("findAll").startSpan();
        try (Scope scope = span.makeCurrent()) {
            return jdbcDataSourceDao.findAllWithNoCredentials();
        } finally {
            span.end();
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public JdbcDataSource one(@PathVariable("id") long id) {
        Span span = tracer.spanBuilder("one").startSpan();
        try (Scope scope = span.makeCurrent()) {
            return jdbcDataSourceDao.findByIdWithNoCredentials(id);
        } finally {
            span.end();
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    @Transactional
    public ResponseEntity<Long> add(@RequestBody JdbcDataSource ds) {
        Span span = tracer.spanBuilder("add").startSpan();
        try (Scope scope = span.makeCurrent()) {
            long id = jdbcDataSourceDao.insert(ds);
            return new ResponseEntity<>(id, HttpStatus.CREATED);
        } finally {
            span.end();
        }
    }

    @RequestMapping(method = RequestMethod.PUT)
    @Transactional
    public ResponseEntity<?> update(@RequestBody JdbcDataSource ds) {
        Span span = tracer.spanBuilder("update").startSpan();
        try (Scope scope = span.makeCurrent()) {
            jdbcDataSourceService.removeFromCache(ds.getId());
            jdbcDataSourceDao.update(ds);
            return new ResponseEntity<>(HttpStatus.OK);
        } finally {
            span.end();
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public ResponseEntity<?> delete(@PathVariable("id") long id) {
        Span span = tracer.spanBuilder("delete").startSpan();
        try (Scope scope = span.makeCurrent()) {
            jdbcDataSourceService.removeFromCache(id);
            componentDao.updateByDataSourceId(id);
            savedQueryDao.clearDataSourceId(id);
            jdbcDataSourceDao.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } finally {
            span.end();
        }
    }

    @RequestMapping(value = "/ping/{id}", method = RequestMethod.GET)
    @Transactional(readOnly = true)
    public String ping(@PathVariable("id") long id) {
        Span span = tracer.spanBuilder("ping").startSpan();
        try (Scope scope = span.makeCurrent()) {
            JdbcDataSource ds = jdbcDataSourceDao.findByIdWithNoCredentials(id);
            DataSource dataSource = jdbcDataSourceService.getDataSource(id);
            return jdbcQueryService.ping(dataSource, ds.getPing());
        } finally {
            span.end();
        }
    }

    @RequestMapping(
            value = "/schema/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public List<Table> getSchema(@PathVariable("id") long id) {
        Span span = tracer.spanBuilder("getSchema").startSpan();
        try (Scope scope = span.makeCurrent()) {
            DataSource dataSource = jdbcDataSourceService.getDataSource(id);
            return jdbcQueryService.getSchema(dataSource);
        } finally {
            span.end();
        }
    }
}
