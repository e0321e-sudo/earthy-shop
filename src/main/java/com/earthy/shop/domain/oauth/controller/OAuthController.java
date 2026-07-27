package com.earthy.shop.domain.oauth.controller;

import com.earthy.shop.domain.member.dto.response.MemberLoginResponseDto;
import com.earthy.shop.domain.oauth.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${oauth.kakao.frontend-redirect-uri}")
    private String frontendRedirectUri;

    // 카카오 로그인 시작
    @GetMapping("/kakao")
    public RedirectView redirectToKakaoLogin() {
        String kakaoLoginUrl = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .build()
                .toUriString();

        return new RedirectView(kakaoLoginUrl);
    }

    // 카카오 로그인 콜백
    @GetMapping("/kakao/callback")
    public RedirectView kakaoCallback(
            @RequestParam String code
    ) {
        MemberLoginResponseDto responseDto = oAuthService.loginWithKakao(code);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendRedirectUri)
                .queryParam("oauth", "kakao")
                .queryParam("accessToken", responseDto.accessToken())
                .queryParam("refreshToken", responseDto.refreshToken())
                .build()
                .toUriString();

        return new RedirectView(redirectUrl);
    }
}
