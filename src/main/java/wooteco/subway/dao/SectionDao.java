package wooteco.subway.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import wooteco.subway.domain.Section;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class SectionDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public SectionDao(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("SECTION")
                .usingGeneratedKeyColumns("id");
    }

    public Section insert(Section section) {
        final SqlParameterSource parameters = new BeanPropertySqlParameterSource(section);
        final Long id = simpleJdbcInsert.executeAndReturnKey(parameters).longValue();
        return new Section(id, section.getLineId(), section.getUpStationId(), section.getDownStationId(), section.getDistance());
    }

    public Section findById(Long id) {
        String SQL = "select * from section where id = ?";
        return jdbcTemplate.queryForObject(SQL, rowMapper(), id);
    }

    public List<Section> findAllByLineId(Long id) {
        String SQL = "select * from section where line_id = ?";
        return jdbcTemplate.query(SQL, rowMapper(), id);
    }

    public void update(Section section) {
        String SQL = "update section set up_station_id = ?, down_station_id = ?, distance = ? where id = ?";
        jdbcTemplate.update(SQL, section.getUpStationId(), section.getDownStationId(), section.getDistance(), section.getId());
    }

    private RowMapper<Section> rowMapper() {
        return (rs, rowNum) -> {
            final Long id = rs.getLong("id");
            final Long lineId = rs.getLong("line_id");
            final Long upStationId = rs.getLong("up_station_id");
            final Long downStationId = rs.getLong("down_station_id");
            final int distance = rs.getInt("distance");
            return new Section(id, lineId, upStationId, downStationId, distance);
        };
    }
}