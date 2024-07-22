package com.shzlw.poli.rest;

import com.shzlw.poli.dao.GroupDao;
import com.shzlw.poli.dao.UserDao;
import com.shzlw.poli.model.Group;
import com.shzlw.poli.service.ReportService;
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

import java.util.List;

@RestController
@RequestMapping("/ws/groups")
public class GroupWs {
    @Autowired
    Tracer tracer; 

    @Autowired
    GroupDao groupDao;

    @Autowired
    ReportService reportService;

    @Autowired
    UserDao userDao;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public List<Group> findAll() {
        Span span = tracer.spanBuilder("findAllGroups").startSpan();
        try (Scope scope = span.makeCurrent()) {
            return groupDao.findAll();
        } finally {
            span.end();
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public Group findOne(@PathVariable("id") long groupId) {
        Span span = tracer.spanBuilder("findOneGroup").startSpan();
        try (Scope scope = span.makeCurrent()) {
            Group group = groupDao.findById(groupId);
            if (group == null) {
                return null;
            }
            List<Long> groupReports = groupDao.findGroupReports(groupId);
            group.setGroupReports(groupReports);
            return group;
        } finally {
            span.end();
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    @Transactional
    public ResponseEntity<Long> add(@RequestBody Group group) {
        Span span = tracer.spanBuilder("addGroup").startSpan();
        try (Scope scope = span.makeCurrent()) {
            long groupId = groupDao.insertGroup(group.getName());
            groupDao.insertGroupReports(groupId, group.getGroupReports());

            List<Long> userIds = userDao.findGroupUsers(groupId);
            for (Long userId : userIds) {
                reportService.invalidateCache(userId);
            }
            return new ResponseEntity<>(groupId, HttpStatus.CREATED);
        } finally {
            span.end();
        }
    }

    @RequestMapping(method = RequestMethod.PUT)
    @Transactional
    public ResponseEntity<?> update(@RequestBody Group group) {
        Span span = tracer.spanBuilder("updateGroup").startSpan();
        try (Scope scope = span.makeCurrent()) {
            long groupId = group.getId();
            groupDao.updateGroup(group);
            groupDao.deleteGroupReports(groupId);
            groupDao.insertGroupReports(groupId, group.getGroupReports());

            List<Long> userIds = userDao.findGroupUsers(groupId);
            for (Long userId : userIds) {
                reportService.invalidateCache(userId);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } finally {
            span.end();
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public ResponseEntity<?> delete(@PathVariable("id") long groupId) {
        Span span = tracer.spanBuilder("deleteGroup").startSpan();
        try (Scope scope = span.makeCurrent()) {
            groupDao.deleteGroupUsers(groupId);
            groupDao.deleteGroupReports(groupId);
            groupDao.deleteGroup(groupId);

            List<Long> userIds = userDao.findGroupUsers(groupId);
            for (Long userId : userIds) {
                reportService.invalidateCache(userId);
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } finally {
            span.end();
        }
    }
}
