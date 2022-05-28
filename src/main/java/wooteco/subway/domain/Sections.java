package wooteco.subway.domain;

import wooteco.subway.dto.SectionsToBeCreatedAndUpdated;
import wooteco.subway.dto.SectionsToBeDeletedAndUpdated;
import wooteco.subway.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Sections {

    private final List<Section> values;

    public Sections(List<Section> values) {
        this.values = values;
    }

    public SectionsToBeCreatedAndUpdated add(Section newSection) {
        validateExistStationInLine(newSection);
        Long currentLastUpStationId = findLastUpSection().getUpStationId();
        Long currentLastDownStationId = findLastDownSection().getDownStationId();

        if (newSection.isNewLastStation(currentLastUpStationId, currentLastDownStationId)) {
            return new SectionsToBeCreatedAndUpdated(newSection);
        }
        return addMiddleSection(newSection);
    }

    private void validateExistStationInLine(Section section) {
        boolean hasUpStation = hasStation(section.getUpStationId());
        boolean hasDownStation = hasStation(section.getDownStationId());
        if (!hasUpStation && !hasDownStation) {
            throw new IllegalArgumentException("구간을 추가하기 위해서는 노선에 들어있는 역이 필요합니다.");
        }
        if (hasUpStation && hasDownStation) {
            throw new IllegalArgumentException("상행역과 하행역이 이미 노선에 모두 등록되어 있습니다.");
        }
    }

    private boolean hasStation(Long stationId) {
        return values.stream()
                .anyMatch(s -> s.getUpStationId().equals(stationId) || s.getDownStationId().equals(stationId));
    }

    private SectionsToBeCreatedAndUpdated addMiddleSection(Section newSection) {
        Section existNearSection = findNearSection(newSection);
        validateNewSectionDistance(newSection, existNearSection);
        Section sectionThatNeedToBeUpdated = null;
        if (newSection.getUpStationId().equals(existNearSection.getUpStationId())) {
            sectionThatNeedToBeUpdated = new Section(existNearSection.getId(), existNearSection.getLineId(),
                    newSection.getDownStationId(), existNearSection.getDownStationId(),
                    existNearSection.getDistance() - newSection.getDistance());
        }
        if (newSection.getDownStationId().equals(existNearSection.getDownStationId())) {
            sectionThatNeedToBeUpdated = new Section(existNearSection.getId(), existNearSection.getLineId(),
                    existNearSection.getUpStationId(), newSection.getUpStationId(),
                    existNearSection.getDistance() - newSection.getDistance());
        }
        return new SectionsToBeCreatedAndUpdated(newSection, sectionThatNeedToBeUpdated);
    }

    private void validateNewSectionDistance(Section newSection, Section existNearSection) {
        if (newSection.getDistance() >= existNearSection.getDistance()) {
            throw new IllegalArgumentException("새로운 구간의 길이는 기존 역 사이의 길이보다 작아야 합니다.");
        }
    }

    private Section findNearSection(Section newSection) {
        return values.stream()
                .filter(s -> s.getUpStationId().equals(newSection.getUpStationId()) ||
                        s.getDownStationId().equals(newSection.getDownStationId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("중간역 생성중 기존역을 찾지 못하였습니다."));
    }

    public SectionsToBeDeletedAndUpdated delete(Long stationId) {
        validateExistStation(stationId);
        validateRemainOneSection();
        Section currentLastUpSection = findLastUpSection();
        Section currentLastDownSection = findLastDownSection();
        if (currentLastUpSection.isUpStation(stationId) ||
                currentLastDownSection.isDownStation(stationId)) {
            return deleteLastSection(currentLastUpSection, currentLastDownSection, stationId);
        }

        Section upSideStation = extractUpSideStation(stationId);
        Section downSideStation = extractDownSideStation(stationId);
        Section sectionToBeUpdated = new Section(upSideStation.getId(), upSideStation.getLineId(), upSideStation.getUpStationId(),
                downSideStation.getDownStationId(), upSideStation.getDistance() + downSideStation.getDistance());
        return new SectionsToBeDeletedAndUpdated(downSideStation, sectionToBeUpdated);
    }

    private void validateExistStation(Long stationId) {
        if (!hasStation(stationId)) {
            throw new NotFoundException("현재 라인에 존재하지 않는 역입니다.");
        }
    }

    private void validateRemainOneSection() {
        if (values.size() == 1) {
            throw new IllegalArgumentException("구간이 하나인 노선에서는 구간 삭제가 불가합니다.");
        }
    }

    private Section findLastUpSection() {
        return values.stream()
                .filter(this::isLastUpStation)
                .findAny()
                .orElseThrow(() -> new NotFoundException("상행 종점을 찾지 못했습니다."));
    }

    private boolean isLastUpStation(Section section) {
        return values.stream()
                .noneMatch(s -> s.getDownStationId().equals(section.getUpStationId()));
    }

    private Section findLastDownSection() {
        return values.stream()
                .filter(this::isLastDownStation)
                .findAny()
                .orElseThrow(() -> new NotFoundException("하행 종점을 찾지 못했습니다."));
    }

    private boolean isLastDownStation(Section section) {
        return values.stream()
                .noneMatch(s -> s.getUpStationId().equals(section.getDownStationId()));
    }

    private SectionsToBeDeletedAndUpdated deleteLastSection(Section lastUpSection, Section lastDownSection, Long stationId) {
        if (stationId.equals(lastUpSection.getUpStationId())) {
            return new SectionsToBeDeletedAndUpdated(lastUpSection);
        }
        if (stationId.equals(lastDownSection.getDownStationId())) {
            return new SectionsToBeDeletedAndUpdated(lastDownSection);
        }
        return null;
    }

    private Section extractUpSideStation(Long stationId) {
        return values.stream()
                .filter(s -> s.getDownStationId().equals(stationId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("중간역 삭제중 상행역을 찾지 못하였습니다."));
    }

    private Section extractDownSideStation(Long stationId) {
        return values.stream()
                .filter(s -> s.getUpStationId().equals(stationId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("중간역 삭제중 하행역을 찾지 못하였습니다."));
    }

    public List<Long> getSortedStationIds() {
        Section lastUpSection = findLastUpSection();
        Long downStationId = lastUpSection.getDownStationId();

        List<Long> sortedStationIds = new ArrayList<>();
        sortedStationIds.add(lastUpSection.getUpStationId());
        sortedStationIds.add(lastUpSection.getDownStationId());

        while (values.size() >= sortedStationIds.size()) {
            downStationId = getNextDownStationId(downStationId);
            sortedStationIds.add(downStationId);
        }
        return sortedStationIds;
    }

    private Long getNextDownStationId(Long downStationId) {
        Optional<Section> nextSection = values.stream()
                .filter(s -> s.getUpStationId().equals(downStationId))
                .findFirst();
        return nextSection.map(Section::getDownStationId).orElseThrow();
    }
}
