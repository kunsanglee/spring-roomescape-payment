package roomescape.web.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import roomescape.infrastructure.auth.JwtProvider;
import roomescape.service.MemberAuthService;
import roomescape.service.request.MemberSignUpDto;
import roomescape.service.response.MemberDto;
import roomescape.web.controller.request.MemberSignUpRequest;
import roomescape.web.controller.request.TokenRequest;
import roomescape.web.controller.response.MemberResponse;

@RestController
public class MemberAuthController {

    private final MemberAuthService memberAuthService;
    private final JwtProvider jwtProvider;

    public MemberAuthController(MemberAuthService memberAuthService, JwtProvider jwtProvider) {
        this.memberAuthService = memberAuthService;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody TokenRequest request,
                                      HttpServletResponse response) {
        if (memberAuthService.isExistsMemberByEmailAndPassword(request.email(), request.password())) {
            String token = jwtProvider.createToken(request.email());
            response.addCookie(memberAuthService.createCookieByToken(token));
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/login/check")
    public ResponseEntity<MemberResponse> findMember(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new IllegalArgumentException("쿠키가 없습니다. 다시 로그인 해주세요.");
        }
        String token = memberAuthService.extractTokenFromCookies(request.getCookies());
        String email = jwtProvider.getPayload(token);
        MemberDto appResponse = memberAuthService.findMemberByEmail(email);
        MemberResponse response = new MemberResponse(appResponse.id(), appResponse.name(), appResponse.role());

        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signUp(@Valid @RequestBody MemberSignUpRequest request) {
        MemberDto appResponse = memberAuthService.signUp(
                new MemberSignUpDto(request.name(), request.email(), request.password()));

        MemberResponse response = new MemberResponse(appResponse.id(), appResponse.name(), appResponse.role());
        return ResponseEntity.created(URI.create("/member" + appResponse.id())).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = memberAuthService.extractTokenFromCookies(request.getCookies());
        String expiredToken = jwtProvider.createExpiredToken(token);
        response.addCookie(memberAuthService.createCookieByToken(expiredToken));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/members")
    public ResponseEntity<List<MemberResponse>> getMembers() {
        List<MemberDto> appResponses = memberAuthService.findAll();

        List<MemberResponse> responses = appResponses.stream()
                .map(MemberResponse::from)
                .toList();

        return ResponseEntity.ok().body(responses);
    }
}
