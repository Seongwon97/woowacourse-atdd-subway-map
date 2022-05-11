package wooteco.subway.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import wooteco.subway.domain.Station;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
class StationDaoTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataSource dataSource;
    private StationDao stationDao;

    @BeforeEach
    void setUp() {
        stationDao = new StationDao(jdbcTemplate, dataSource);
    }

    @DisplayName("역 정보를 저장한다.")
    @Test
    void save() {
        Station expected = new Station("강남역");
        Station actual = stationDao.insert(expected);

        assertThat(actual.getName()).isEqualTo(expected.getName());
    }

    @DisplayName("모든 역 정보를 조회한다.")
    @Test
    void findAll() {
        Station station1 = new Station("강남역");
        Station station2 = new Station("신논현역");
        Station savedStation1 = stationDao.insert(station1);
        Station savedStation2 = stationDao.insert(station2);

        List<Station> actual = stationDao.findAll();

        assertThat(actual).containsExactly(savedStation1, savedStation2);
    }

    @DisplayName("역을 삭제한다.")
    @Test
    void delete() {
        Station station = stationDao.insert(new Station("강남역"));

        stationDao.deleteById(station.getId());
        List<Station> stations = stationDao.findAll();

        assertThat(stations.contains(station)).isFalse();
    }

    @DisplayName("id를 통해 역의 존재 여부를 판단한다.")
    @Test
    void existStationById() {
        Station savedStation = stationDao.insert(new Station("강남역"));
        boolean isExisted = stationDao.existStationById(savedStation.getId());

        assertThat(isExisted).isTrue();
    }
}
