package roomescape.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationDate;
import roomescape.domain.ReservationTime;
import roomescape.domain.repository.ReservationRepository;
import roomescape.domain.repository.ReservationTimeRepository;
import roomescape.exception.RoomescapeErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.service.request.ReservationTimeSaveDto;
import roomescape.service.response.BookableReservationTimeDto;
import roomescape.service.response.ReservationTimeDto;

@Service
@Transactional(readOnly = true)
public class ReservationTimeService {

    private final ReservationTimeRepository reservationTimeRepository;
    private final ReservationRepository reservationRepository;

    public ReservationTimeService(ReservationTimeRepository reservationTimeRepository,
                                  ReservationRepository reservationRepository) {
        this.reservationTimeRepository = reservationTimeRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public ReservationTimeDto save(ReservationTimeSaveDto request) {
        ReservationTime newReservationTime = new ReservationTime(request.startAt());
        validateDuplication(newReservationTime.getStartAt());
        ReservationTime savedTime = reservationTimeRepository.save(newReservationTime);

        return ReservationTimeDto.from(savedTime);
    }

    private void validateDuplication(LocalTime parsedTime) {
        if (reservationTimeRepository.existsByStartAt(parsedTime)) {
            throw new RoomescapeException(RoomescapeErrorCode.DUPLICATED_TIME, "이미 존재하는 예약 시간 정보 입니다.");
        }
    }

    @Transactional
    public void delete(Long id) {
        if (reservationRepository.existsByTimeId(id)) {
            throw new RoomescapeException(RoomescapeErrorCode.ALREADY_RESERVED, "해당 데이터를 사용하는 예약이 존재합니다.");
        }
        reservationTimeRepository.deleteById(id);
    }

    public List<ReservationTimeDto> findAll() {
        return reservationTimeRepository.findAll().stream()
                .map(ReservationTimeDto::from)
                .toList();
    }

    public List<BookableReservationTimeDto> findAllWithBookAvailability(String date, Long themeId) {
        List<Reservation> reservations = reservationRepository.findAllByDateAndThemeId(
                new ReservationDate(date), themeId
        );
        List<ReservationTime> reservedTimes = reservations.stream()
                .map(Reservation::getTime)
                .toList();

        return reservationTimeRepository.findAll().stream()
                .map(time -> BookableReservationTimeDto.of(time, isBooked(reservedTimes, time)))
                .toList();
    }

    private boolean isBooked(List<ReservationTime> reservedTimes, ReservationTime time) {
        return reservedTimes.stream()
                .anyMatch(reservationTime -> Objects.equals(reservationTime.getId(), time.getId()));
    }
}
