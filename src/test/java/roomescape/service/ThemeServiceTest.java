package roomescape.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import roomescape.domain.repository.ReservationRepository;
import roomescape.domain.repository.ThemeRepository;
import roomescape.exception.RoomescapeException;
import roomescape.service.request.ThemeSaveAppRequest;

@ExtendWith(MockitoExtension.class)
class ThemeServiceTest {

    private final String validName = "방탈출";
    private final String validDescription = "재밌는 방탈출 게임이다";
    private final String validThumbnail = "https://aaa.jpg";
    @InjectMocks
    private ThemeService themeService;
    @Mock
    private ThemeRepository themeRepository;
    @Mock
    private ReservationRepository reservationRepository;

    @DisplayName("이름이 null 또는 빈 값이면 예외가 발생한다.")
    @ParameterizedTest
    @NullAndEmptySource
    void save_IllegalName(String name) {
        assertThatThrownBy(() -> themeService.save(new ThemeSaveAppRequest(name, validDescription, validThumbnail)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("description이 null 또는 빈 값이면 예외가 발생한다.")
    @ParameterizedTest
    @NullAndEmptySource
    void save_IllegalDescription(String description) {
        assertThatThrownBy(() -> themeService.save(new ThemeSaveAppRequest(validName, description, validThumbnail)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("thumbnail 형식이 잘못된 경우 예외가 발생한다.")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ftp://hello.jpg"})
    void save_IllegalThumbnail(String thumbnail) {
        assertThatThrownBy(() -> themeService.save(new ThemeSaveAppRequest(validName, validDescription, thumbnail)))
                .isInstanceOf(RoomescapeException.class);
    }

    @DisplayName("이름이 동일한 방탈출 테마를 저장하면 예외가 발생한다.")
    @Test
    void save_DuplicatedName() {
        when(themeRepository.existsByName(validName))
                .thenReturn(true);

        assertThatThrownBy(
                () -> themeService.save(new ThemeSaveAppRequest(validName, validDescription, validThumbnail)))
                .isInstanceOf(RoomescapeException.class);
    }

    @DisplayName("테마 사용하는 예약이 존재하면 삭제 불가")
    @Test
    void delete_ReservationExists() {
        Long themeId = 1L;
        when(reservationRepository.existsByThemeId(themeId))
                .thenReturn(true);

        assertThatThrownBy(() -> themeService.delete(themeId))
                .isInstanceOf(RoomescapeException.class);
    }
}
