package roomescape.service;

import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationDate;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.repository.MemberRepository;
import roomescape.domain.repository.ReservationRepository;
import roomescape.domain.repository.ReservationTimeRepository;
import roomescape.domain.repository.ThemeRepository;
import roomescape.exception.RoomescapeErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.service.request.AdminSearchedReservationAppRequest;
import roomescape.service.request.PaymentApproveAppRequest;
import roomescape.service.request.ReservationSaveAppRequest;
import roomescape.service.response.ReservationAppResponse;
import roomescape.service.specification.ReservationSpecification;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final ThemeRepository themeRepository;
    private final PaymentClient paymentClient;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationTimeRepository reservationTimeRepository,
                              ThemeRepository themeRepository,
                              MemberRepository memberRepository,
                              PaymentClient paymentClient) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.themeRepository = themeRepository;
        this.memberRepository = memberRepository;
        this.paymentClient = paymentClient;
    }

    @Transactional
    public ReservationAppResponse save(ReservationSaveAppRequest reservationSaveAppRequest) {
        Reservation reservation = createReservation(reservationSaveAppRequest);
        Reservation savedReservation = reservationRepository.save(reservation);

        PaymentApproveAppRequest paymentApproveAppRequest = reservationSaveAppRequest.paymentApproveAppRequest();
        if (paymentApproveAppRequest != null) {
            paymentClient.approve(paymentApproveAppRequest);
        }

        return ReservationAppResponse.from(savedReservation);
    }

    private Reservation createReservation(ReservationSaveAppRequest reservationSaveAppRequest) {
        Member member = findMember(reservationSaveAppRequest.memberId());
        ReservationDate date = new ReservationDate(reservationSaveAppRequest.date());
        ReservationTime time = findTime(reservationSaveAppRequest.timeId());
        Theme theme = findTheme(reservationSaveAppRequest.themeId());
        Reservation reservation = new Reservation(member, date, time, theme);
        validatePastReservation(reservation);
        validateDuplication(date, reservationSaveAppRequest.timeId(), reservationSaveAppRequest.themeId());
        return reservation;
    }

    private ReservationTime findTime(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new RoomescapeException(RoomescapeErrorCode.NOT_FOUND_TIME,
                        "예약에 대한 예약시간이 존재하지 않습니다."));
    }

    private Theme findTheme(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new RoomescapeException(RoomescapeErrorCode.NOT_FOUND_THEME,
                        "예약에 대한 테마가 존재하지 않습니다."));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new RoomescapeException(RoomescapeErrorCode.NOT_FOUND_MEMBER,
                        memberId + "|예약에 대한 사용자가 존재하지 않습니다."));
    }

    private void validatePastReservation(Reservation reservation) {
        if (reservation.isPast()) {
            throw new RoomescapeException(RoomescapeErrorCode.PAST_REQUEST, "과거의 시간으로 예약할 수 없습니다.");
        }
    }

    private void validateDuplication(ReservationDate date, Long timeId, Long themeId) {
        if (reservationRepository.existsByDateAndTimeIdAndThemeId(date, timeId, themeId)) {
            throw new RoomescapeException(RoomescapeErrorCode.ALREADY_RESERVED, "이미 존재하는 예약 정보 입니다.");
        }
    }

    @Transactional
    public void delete(Long id) {
        reservationRepository.deleteById(id);
    }

    public List<ReservationAppResponse> findAll() {
        return reservationRepository.findAll().stream()
                .map(ReservationAppResponse::from)
                .toList();
    }

    public List<ReservationAppResponse> findAllSearched(AdminSearchedReservationAppRequest request) {
        Specification<Reservation> reservationSpecification = new ReservationSpecification().generate(request);

        return reservationRepository.findAll(reservationSpecification).stream()
                .map(ReservationAppResponse::from)
                .toList();
    }

    public List<ReservationAppResponse> findByMemberId(Long id) {

        return reservationRepository.findAllByMemberId(id).stream()
                .map(ReservationAppResponse::from)
                .toList();
    }
}
