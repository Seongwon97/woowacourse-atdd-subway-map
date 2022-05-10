package wooteco.subway.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wooteco.subway.dao.LineDao;
import wooteco.subway.dao.SectionDao;
import wooteco.subway.dao.StationDao;
import wooteco.subway.domain.Line;
import wooteco.subway.domain.Section;
import wooteco.subway.domain.Station;
import wooteco.subway.dto.LineRequest;
import wooteco.subway.dto.LineResponse;
import wooteco.subway.dto.StationResponse;
import wooteco.subway.exception.AccessNoneDataException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineService {

    private final LineDao lineDao;
    private final SectionDao sectionDao;
    private final StationDao stationDao;

    public LineService(LineDao lineDao, StationDao stationDao, SectionDao sectionDao) {
        this.lineDao = lineDao;
        this.stationDao = stationDao;
        this.sectionDao = sectionDao;
    }

    @Transactional
    public LineResponse create(LineRequest request) {
        Line line = new Line(request.getName(), request.getColor());
        Line savedLine = lineDao.insert(line);

        validateExistStation(request.getUpStationId(), request.getDownStationId());
        Section section = new Section(savedLine.getId(),
                request.getUpStationId(), request.getDownStationId(), request.getDistance());
        sectionDao.insert(section);

        List<StationResponse> stationResponses = finAllStationsByLineId(savedLine);
        return new LineResponse(savedLine.getId(), savedLine.getName(), savedLine.getColor(), stationResponses);
    }

    private void validateExistStation(Long upStationId, Long downStationId) {
        if (!stationDao.existStationById(upStationId) || !stationDao.existStationById(downStationId)) {
            throw new IllegalArgumentException("존재하지 않는 역으로 구간을 만드는 시도가 있었습니다.");
        }
    }

    private List<StationResponse> finAllStationsByLineId(Line savedLine) {
        List<Station> stations = stationDao.findAllByLineId(savedLine.getId());
        return stations.stream()
                .distinct()
                .map(s -> new StationResponse(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    public List<LineResponse> findAll() {
        return lineDao.findAll()
                .stream()
                .map(l -> LineResponse.of(l, createStationResponseByLineId(l.getId())))
                .collect(Collectors.toList());
    }

    public LineResponse findById(Long lineId) {
        Line line = lineDao.findById(lineId);
        List<StationResponse> stationResponses = createStationResponseByLineId(lineId);
        return LineResponse.of(line, stationResponses);
    }

    private List<StationResponse> createStationResponseByLineId(Long lineId) {
        List<Station> stations = stationDao.findAllByLineId(lineId);
        return stations.stream()
                .distinct()
                .map(s -> new StationResponse(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    public void update(Long lineId, LineRequest request) {
        validateExistData(lineId);
        lineDao.update(new Line(lineId, request.getName(), request.getColor()));
    }

    public void delete(Long lineId) {
        validateExistData(lineId);
        lineDao.delete(lineId);
    }

    private void validateExistData(Long lineId) {
        boolean isExist = lineDao.existLineById(lineId);
        if (!isExist) {
            throw new AccessNoneDataException();
        }
    }
}
