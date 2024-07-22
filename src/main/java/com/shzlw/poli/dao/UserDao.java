package com.shzlw.poli.dao;

import com.shzlw.poli.model.User;
import com.shzlw.poli.model.UserAttribute;
import com.shzlw.poli.util.CommonUtils;
import com.shzlw.poli.util.PasswordUtils;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class UserDao {

    @Autowired
    Tracer tracer;

    @Autowired
    JdbcTemplate jt;

    @Autowired
    NamedParameterJdbcTemplate npjt;

    public User findByUsernameAndPassword(String username, String rawPassword) {
        Span span = tracer.spanBuilder("findByUsernameAndPassword").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String encryptedPassword = PasswordUtils.getMd5Hash(rawPassword);
            String sql = "SELECT id, username, name, sys_role "
                        + "FROM p_user "
                        + "WHERE username=? AND password=?";
            try {
                User user = (User) jt.queryForObject(sql, new Object[]{username, encryptedPassword}, new UserInfoRowMapper());
                return user;
            } catch (EmptyResultDataAccessException e) {
                return null;
            }
        } finally {
            span.end();
        }
    }

    public User findByUsernameAndTempPassword(String username, String rawTempPassword) {
        Span span = tracer.spanBuilder("findByUsernameAndTempPassword").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String encryptedPassword = PasswordUtils.getMd5Hash(rawTempPassword);
            String sql = "SELECT id, username, name, sys_role "
                        + "FROM p_user "
                        + "WHERE username=? AND temp_password=?";
            try {
                User user = (User) jt.queryForObject(sql, new Object[]{username, encryptedPassword}, new UserInfoRowMapper());
                return user;
            } catch (EmptyResultDataAccessException e) {
                return null;
            }
        } finally {
            span.end();
        }
    }

    public User findBySessionKey(String sessionKey) {
        Span span = tracer.spanBuilder("findBySessionKey").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT id, username, name, sys_role "
                        + "FROM p_user WHERE session_key=?";
            try {
                User user = (User) jt.queryForObject(sql, new Object[]{ sessionKey }, new UserInfoRowMapper());
                return user;
            } catch (EmptyResultDataAccessException e) {
                return null;
            }
        } finally {
            span.end();
        }
    }

    public User findByApiKey(String apiKey) {
        Span span = tracer.spanBuilder("findByApiKey").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT id, username, name, sys_role, session_key "
                        + "FROM p_user WHERE api_key=?";
            try {
                User user = (User) jt.queryForObject(sql, new Object[]{ apiKey }, new UserSesssionKeyMapper());
                return user;
            } catch (EmptyResultDataAccessException e) {
                return null;
            }
        } finally {
            span.end();
        }
    }

    public User findByShareKey(String shareKey) {
        Span span = tracer.spanBuilder("findByShareKey").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT u.id, u.username, u.name, u.sys_role, u.session_key "
                        + "FROM p_user u, p_shared_report sr "
                        + "WHERE u.id = sr.user_id "
                        + "AND sr.share_key=?";
            try {
                User user = (User) jt.queryForObject(sql, new Object[]{ shareKey }, new UserSesssionKeyMapper());
                return user;
            } catch (EmptyResultDataAccessException e) {
                return null;
            }
        } finally {
            span.end();
        }
    }

    public User findAccount(long id) {
        Span span = tracer.spanBuilder("findAccount").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT id, username, name, sys_role, api_key "
                        + "FROM p_user WHERE id=?";
            try {
                User user = (User) jt.queryForObject(sql, new Object[]{ id }, new UserAccountMapper());
                return user;
            } catch (EmptyResultDataAccessException e) {
                return null;
            }
        } finally {
            span.end();
        }
    }

    public User findAccountBySessionKey(String sessionKey) {
        Span span = tracer.spanBuilder("findAccountBySessionKey").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT id, username, name, sys_role, api_key "
                        + "FROM p_user WHERE session_key=?";
            try {
                User user = (User) jt.queryForObject(sql, new Object[]{ sessionKey }, new UserAccountMapper());
                return user;
            } catch (EmptyResultDataAccessException e) {
                return null;
            }
        } finally {
            span.end();
        }
    }

    public User findById(long id) {
        Span span = tracer.spanBuilder("findById").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT id, username, name, sys_role "
                        + "FROM p_user WHERE id=?";
            try {
                User user = (User) jt.queryForObject(sql, new Object[]{ id }, new UserInfoRowMapper());
                return user;
            } catch (EmptyResultDataAccessException e) {
                return null;
            }
        } finally {
            span.end();
        }
    }

    public int updateSessionKey(long userId, String sessionKey) {
        Span span = tracer.spanBuilder("updateSessionKey").startSpan();
        try (Scope scope = span.makeCurrent()) {
            long sessionTimeout = CommonUtils.toEpoch(LocalDateTime.now());
            String sql = "UPDATE p_user SET session_key=?, session_timeout=? WHERE id=?";
            return jt.update(sql, new Object[] { sessionKey, sessionTimeout, userId});
        } finally {
            span.end();
        }
    }

    public int updateApiKey(long userId, String apiKey) {
        Span span = tracer.spanBuilder("updateApiKey").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "UPDATE p_user SET api_key=? WHERE id=?";
            return jt.update(sql, new Object[] { apiKey, userId });
        } finally {
            span.end();
        }
    }

    public int updateTempPassword(long userId, String rawNewPassword) {
        Span span = tracer.spanBuilder("updateTempPassword").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String encryptedPassword = PasswordUtils.getMd5Hash(rawNewPassword);
            String sql = "UPDATE p_user SET temp_password=NULL, password=? WHERE id=?";
            return jt.update(sql, new Object[] { encryptedPassword, userId });
        } finally {
            span.end();
        }
    }

    public List<User> findNonAdminUsers(long myUserId) {
        Span span = tracer.spanBuilder("findNonAdminUsers").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT id, username, name, sys_role "
                        + "FROM p_user WHERE sys_role IN ('viewer', 'developer') AND id != ?";
            return jt.query(sql, new Object[]{myUserId}, new UserInfoRowMapper());
        } finally {
            span.end();
        }
    }

    public List<User> findViewerUsers(long myUserId) {
        Span span = tracer.spanBuilder("findViewerUsers").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT id, username, name, sys_role "
                        + "FROM p_user WHERE sys_role = 'viewer' AND id != ?";
            return jt.query(sql, new Object[]{myUserId}, new UserInfoRowMapper());
        } finally {
            span.end();
        }
    }

    public List<Long> findUserGroups(long userId) {
        Span span = tracer.spanBuilder("findUserGroups").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT group_id FROM p_group_user WHERE user_id = ?";
            return jt.queryForList(sql, new Object[]{ userId }, Long.class);
        } finally {
            span.end();
        }
    }

    public List<Long> findGroupUsers(long groupId) {
        Span span = tracer.spanBuilder("findGroupUsers").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT user_id FROM p_group_user WHERE group_id = ?";
            return jt.queryForList(sql, new Object[]{ groupId }, Long.class);
        } finally {
            span.end();
        }
    }

    public List<UserAttribute> findUserAttributes(long userId) {
        Span span = tracer.spanBuilder("findUserAttributes").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "SELECT attr_key, attr_value FROM p_user_attribute WHERE user_id = ?";
            return jt.query(sql, new Object[]{ userId }, (rs, i) -> {
                UserAttribute r = new UserAttribute();
                r.setAttrKey(rs.getString(UserAttribute.ATTR_KEY));
                r.setAttrValue(rs.getString(UserAttribute.ATTR_VALUE));
                return r;
            });
        } finally {
            span.end();
        }
    }

    public long insertUser(String username, String name, String rawTempPassword, String sysRole) {
        Span span = tracer.spanBuilder("insertUser").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String encryptedPassword = PasswordUtils.getMd5Hash(rawTempPassword);
            String sql = "INSERT INTO p_user(username, name, temp_password, sys_role) "
                        + "VALUES(:username, :name, :temp_password, :sys_role)";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(User.USERNAME, username);
            params.addValue(User.NAME, name);
            params.addValue(User.TEMP_PASSWORD, encryptedPassword);
            params.addValue(User.SYS_ROLE, sysRole);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            npjt.update(sql, params, keyHolder, new String[] { User.ID });
            return keyHolder.getKey().longValue();
        } finally {
            span.end();
        }
    }

    public void insertUserGroups(long userId, List<Long> userGroups) {
        Span span = tracer.spanBuilder("insertUserGroups").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "INSERT INTO p_group_user(group_id, user_id) VALUES(?, ?)";
            jt.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setLong(1, userGroups.get(i));
                    ps.setLong(2, userId);
                }

                @Override
                public int getBatchSize() {
                    return userGroups.size();
                }
            });
        } finally {
            span.end();
        }
    }

    public void insertUserAttributes(long userId, List<UserAttribute> userAttributes) {
        Span span = tracer.spanBuilder("insertUserAttributes").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "INSERT INTO p_user_attribute(user_id, attr_key, attr_value) VALUES(?, ?, ?)";
            jt.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setLong(1, userId);
                    ps.setString(2, userAttributes.get(i).getAttrKey());
                    ps.setString(3, userAttributes.get(i).getAttrValue());
                }

                @Override
                public int getBatchSize() {
                    return userAttributes.size();
                }
            });
        } finally {
            span.end();
        }
    }

    public long updateUser(User user) {
        Span span = tracer.spanBuilder("updateUser").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String rawTempPassword = user.getTempPassword();
            if (StringUtils.isEmpty(rawTempPassword)) {
                String sql = "UPDATE p_user SET username=?, name=?, sys_role=? WHERE id=?";
                return jt.update(sql, new Object[]{
                        user.getUsername(),
                        user.getName(),
                        user.getSysRole(),
                        user.getId()
                });
            } else {
                String encryptedPassword = PasswordUtils.getMd5Hash(rawTempPassword);
                String sql = "UPDATE p_user SET username=?, name=?, sys_role=?, password=NULL, temp_password=? "
                            + "WHERE id=?";
                return jt.update(sql, new Object[]{
                        user.getUsername(),
                        user.getName(),
                        user.getSysRole(),
                        encryptedPassword,
                        user.getId()
                });
            }
        } finally {
            span.end();
        }
    }

    public long updateUserAccount(long userId, String name, String rawPassword) {
        Span span = tracer.spanBuilder("updateUserAccount").startSpan();
        try (Scope scope = span.makeCurrent()) {
            if (StringUtils.isEmpty(rawPassword)) {
                String sql = "UPDATE p_user SET name=? WHERE id=?";
                return jt.update(sql, new Object[]{ name, userId });
            } else {
                String encryptedPassword = PasswordUtils.getMd5Hash(rawPassword);
                String sql = "UPDATE p_user SET name=?, password=? WHERE id=?";
                return jt.update(sql, new Object[]{ name, encryptedPassword, userId });
            }
        } finally {
            span.end();
        }
    }

    public int deleteUser(long userId) {
        Span span = tracer.spanBuilder("deleteUser").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "DELETE FROM p_user WHERE id=?";
            return jt.update(sql, new Object[]{ userId });
        } finally {
            span.end();
        }
    }

    public int deleteUserGroups(long userId) {
        Span span = tracer.spanBuilder("deleteUserGroups").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "DELETE FROM p_group_user WHERE user_id=?";
            return jt.update(sql, new Object[]{ userId });
        } finally {
            span.end();
        }
    }

    public int deleteUserAttributes(long userId) {
        Span span = tracer.spanBuilder("deleteUserAttributes").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String sql = "DELETE FROM p_user_attribute WHERE user_id=?";
            return jt.update(sql, new Object[]{ userId });
        } finally {
            span.end();
        }
    }

    private static class UserInfoRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int i) throws SQLException {
            User r = new User();
            r.setId(rs.getLong(User.ID));
            r.setUsername(rs.getString(User.USERNAME));
            r.setName(rs.getString(User.NAME));
            r.setSysRole(rs.getString(User.SYS_ROLE));
            return r;
        }
    }

    private static class UserAccountMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int i) throws SQLException {
            User r = new User();
            r.setId(rs.getLong(User.ID));
            r.setUsername(rs.getString(User.USERNAME));
            r.setName(rs.getString(User.NAME));
            r.setSysRole(rs.getString(User.SYS_ROLE));
            r.setApiKey(rs.getString(User.API_KEY));
            return r;
        }
    }

    private static class UserSesssionKeyMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int i) throws SQLException {
            User r = new User();
            r.setId(rs.getLong(User.ID));
            r.setUsername(rs.getString(User.USERNAME));
            r.setName(rs.getString(User.NAME));
            r.setSysRole(rs.getString(User.SYS_ROLE));
            r.setSessionKey(rs.getString(User.SESSION_KEY));
            return r;
        }
    }
}
