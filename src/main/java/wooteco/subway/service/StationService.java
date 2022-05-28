package wooteco.subway.service;

import org.springframework.stereotype.Service;
import wooteco.subway.dao.StationDao;
import wooteco.subway.domain.Station;
import wooteco.subway.dto.StationRequest;
import wooteco.subway.dto.StationResponse;
import wooteco.subway.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StationService {

    private final StationDao stationDao;

    public StationService(StationDao stationDao) {
        this.stationDao = stationDao;
    }

    public StationResponse create(StationRequest request) {
        Station station = new Station(request.getName());
        Station newStation = stationDao.insert(station);
        return StationResponse.of(newStation);
    }

    public List<StationResponse> findAll() {
        List<Station> stations = stationDao.findAll();
        return stations.stream()
                .map(StationResponse::of)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        validateExistData(id);
        stationDao.deleteById(id);
    }

    private void validateExistData(Long lineId) {
        boolean isExisted = stationDao.existStationById(lineId);
        if (!isExisted) {
            throw new NotFoundException("접근하려는 역이 존재하지 않습니다.");
        }
    }
}
