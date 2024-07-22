package com.shzlw.poli.dao;

import com.shzlw.poli.metrics.CustomMetrics;
import com.shzlw.poli.model.Report;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ReportDao {

    @Autowired
    private JdbcTemplate jt;

    @Autowired
    private NamedParameterJdbcTemplate npjt;

    @Autowired
    private Tracer tracer;

    @Autowired
    private CustomMetrics customMetrics;

    public List<Report> findAll() {
        Span span = tracer.spanBuilder("findAllReports").startSpan();
        long startTime = System.currentTimeMillis();

        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT id, name, style, project FROM p_report";
            List<Report> result = jt.query(sql, new ReportRowMapper());

            customMetrics.getDbQueryCounter().add(1);
            return result;
        } catch (Exception e) {
            customMetrics.getDbFailedQueryCounter().add(1);
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            span.end();
        }
    }

    public List<Report> findByViewer(long userId) {
        Span span = tracer.spanBuilder("findByViewer").startSpan();
        long startTime = System.currentTimeMillis();

        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT d.id, d.name, d.style, d.project "
                        + "FROM p_group_report gd, p_report d, p_group_user gu "
                        + "WHERE gd.report_id = d.id "
                        + "AND gd.group_id = gu.group_id "
                        + "AND gu.user_id = ?";
            List<Report> result = jt.query(sql, new Object[] { userId }, new ReportRowMapper());

            customMetrics.getDbQueryCounter().add(1);
            return result;
        } catch (Exception e) {
            customMetrics.getDbFailedQueryCounter().add(1);
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            span.end();
        }
    }

    public Report findById(long id) {
        Span span = tracer.spanBuilder("findReportById").startSpan();

        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT id, name, style, project FROM p_report WHERE id=?";
            try {
                Report report = jt.queryForObject(sql, new Object[]{ id }, new ReportRowMapper());
                customMetrics.getDbQueryCounter().add(1);
                return report;
            } catch (EmptyResultDataAccessException e) {
                customMetrics.getDbFailedQueryCounter().add(1);
                return null;
            }
        } catch (Exception e) {
            customMetrics.getDbFailedQueryCounter().add(1);
            throw e;
        } finally {
            span.end();
        }
    }

    public List<Report> findFavouritesByUserId(long userId) {
        Span span = tracer.spanBuilder("findFavouritesByUserId").startSpan();

        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT r.id, r.name, r.project "
                        + "FROM p_report r, p_user_favourite uf "
                        + "WHERE r.id = uf.report_id "
                        + "AND uf.user_id = ?";
            List<Report> result = jt.query(sql, new Object[] { userId }, new ReportRowMapper());

            customMetrics.getDbQueryCounter().add(1);
            return result;
        } catch (Exception e) {
            customMetrics.getDbFailedQueryCounter().add(1);
            throw e;
        } finally {
            span.end();
        }
    }

    public Report findByName(String name) {
        Span span = tracer.spanBuilder("findReportByName").startSpan();

        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT id, name, style, project FROM p_report WHERE name=?";
            try {
                Report report = jt.queryForObject(sql, new Object[]{ name }, new ReportRowMapper());
                customMetrics.getDbQueryCounter().add(1);
                return report;
            } catch (EmptyResultDataAccessException e) {
                customMetrics.getDbFailedQueryCounter().add(1);
                return null;
            }
        } catch (Exception e) {
            customMetrics.getDbFailedQueryCounter().add(1);
            throw e;
        } finally {
            span.end();
        }
    }

    public long insert(String name, String style, String project) {
        Span span = tracer.spanBuilder("insertReport").startSpan();

        try (Scope scope = span.makeCurrent()) {
            String sql = "INSERT INTO p_report(name, style, project) VALUES(:name, :style, :project)";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(Report.NAME, name);
            params.addValue(Report.STYLE, style);
            params.addValue(Report.PROJECT, project);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            npjt.update(sql, params, keyHolder, new String[] { Report.ID});
            customMetrics.getDbQueryCounter().add(1);
            return keyHolder.getKey().longValue();
        } catch (Exception e) {
            customMetrics.getDbFailedQueryCounter().add(1);
            throw e;
        } finally {
            span.end();
        }
    }

    public int update(Report r) {
        Span span = tracer.spanBuilder("updateReport").startSpan();
        long startTime = System.currentTimeMillis();

        try (Scope scope = span.makeCurrent()) {
            String sql = "UPDATE p_report SET name=?, style=?, project=? WHERE id=?";
            int result = jt.update(sql, new Object[] { r.getName(), r.getStyle(), r.getProject(), r.getId() });

            customMetrics.getDbQueryCounter().add(1);
            return result;
        } catch (Exception e) {
            customMetrics.getDbFailedQueryCounter().add(1);
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            span.end();
        }
    }

    public int delete(long id) {
        Span span = tracer.spanBuilder("deleteReport").startSpan();
        long startTime = System.currentTimeMillis();

        try (Scope scope = span.makeCurrent()) {
            String sql = "DELETE FROM p_report WHERE id=?";
            int result = jt.update(sql, id);

            customMetrics.getDbQueryCounter().add(1);
            return result;
        } catch (Exception e) {
            customMetrics.getDbFailedQueryCounter().add(1);
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            span.end();
        }
    }

    private static class ReportRowMapper implements RowMapper<Report> {
        @Override
        public Report mapRow(ResultSet rs, int rowNum) throws SQLException {
            Report report = new Report();
            report.setId(rs.getLong("id"));
            report.setName(rs.getString("name"));
            report.setStyle(rs.getString("style"));
            report.setProject(rs.getString("project"));
            return report;
        }
    }
}
