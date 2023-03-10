package com.ssafy.interview.api.service.user;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.interview.api.request.user.UserLoginPostReq;
import com.ssafy.interview.common.auth.SsafyUserDetails;
import com.ssafy.interview.common.model.KakaoAccountDto;
import com.ssafy.interview.common.model.KakaoPropertiesDto;
import com.ssafy.interview.common.model.KakaoUserInfoDto;
import com.ssafy.interview.common.util.JwtTokenUtil;
import com.ssafy.interview.db.entitiy.User;
import com.ssafy.interview.db.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service("authService")
public class AuthServiceImpl implements AuthService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    RedisTemplate<String, String> redisTemplate;
    @Value("${jwt.expiration.access}")
    int accessExpireTime;
    @Value("${jwt.expiration.refresh}")
    int refreshExpireTime;

    @Override
    public String getKakaoAccessToken(String code) {
        String accessToken = "";
        String refreshToken = "";
        String requestURL = "https://kauth.kakao.com/oauth/token";

        try {
            // HTTP Header ??????
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

            // HTTP Body ??????
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", "dc6c7559412fd1c77ad3e0a798803e27");
            body.add("redirect_uri", "https://intube.store/auth/kakao/callback");
            body.add("code", code);

            // HTTP ?????? ?????????
            HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> response = rt.exchange(
                    "https://kauth.kakao.com/oauth/token",
                    HttpMethod.POST,
                    kakaoTokenRequest,
                    String.class
            );
            // HTTP ?????? (JSON) -> ????????? ?????? ??????
            String responseBody = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("access_token").asText();
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public KakaoUserInfoDto getKakaUserInfo(String accessToken) {
        try {
            // HTTP Header ??????
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);
            headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

            // HTTP ?????? ?????????
            HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> response = rt.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.POST,
                    kakaoUserInfoRequest,
                    String.class
            );

            // responseBody??? ?????? ????????? ??????
            String responseBody = response.getBody();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            Long id = jsonNode.get("id").asLong();
            String connectedAt = jsonNode.get("connected_at").asText();
            KakaoAccountDto kakaoAccountDto = new KakaoAccountDto(jsonNode.get("kakao_account"));
            KakaoPropertiesDto kakaoPropertiesDto = null;
            if (jsonNode.has("properties")) {
                kakaoPropertiesDto = new KakaoPropertiesDto(jsonNode.get("properties"));
            }

            return new KakaoUserInfoDto(id, connectedAt, kakaoAccountDto, kakaoPropertiesDto);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public Map<String, String> getToken(UserLoginPostReq loginInfo) throws NoSuchElementException {
        String email = loginInfo.getEmail();
        String password = loginInfo.getPassword();

        User user = userRepository.findByEmail(email).get();

        // ????????? ????????? ??????????????? ????????? ???????????? ??? ????????? ????????? ????????? ???????????? ??????????????? ????????? ??????.(????????? ?????????????????? ?????? ??????)
        if (passwordEncoder.matches(password, user.getPassword())) {
            // ????????? ??????????????? ?????? ??????, ????????? ???????????? ??????.(????????? ????????? ???????????? ????????? ??????)
            String accessToken = JwtTokenUtil.getAccessToken(email);
            String refreshToken = JwtTokenUtil.getRefreshToken(email);

            Map<String, String> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);

            return response;
        }
        // ???????????? ?????? ??????????????? ??????, ????????? ????????? ??????.
        return null;
    }

    @Override
    public ResponseCookie setRefreshToken(String email, String refreshToken) {
        // 1. Redis??? ?????? - ?????? ?????? ????????? ?????? ?????? ?????? ??????
        setAuthKey(email, refreshToken, refreshExpireTime);

        // 2. ????????? ?????? - response header ????????? ??????
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .domain("intube.store")
                .maxAge(refreshExpireTime/1000)
                .path("/")
                .secure(true)
                .sameSite("None")
                .httpOnly(true)
                .build();

        return cookie;
    }

    @Override
    public void setAccessToken(String email) {
        setAuthKey(email+"-BlackList", "Forced expiration", accessExpireTime);
    }

    @Override
    public String issueToken(Cookie cookie) throws NullPointerException {
        String cookieToken = cookie.getValue();

        // Refresh Token ??????
        JWTVerifier verifier = JwtTokenUtil.getVerifier("RT");
        JwtTokenUtil.handleError(verifier, cookieToken);
        DecodedJWT decodedJWT = verifier.verify(cookieToken);
        String userId = decodedJWT.getSubject();

        // Redis?????? ????????? Refresh Token ?????? ????????????.
        String refreshToken = getAuthKey(userId);
        if (refreshToken == null) {
            // refresh token??? ?????????
            throw new NullPointerException();
        }

        // ?????? ?????????
        return JwtTokenUtil.getAccessToken(userId);
    }

    @Override
    public int getKakaoRegisterInfo(KakaoUserInfoDto kakaoUserInfoDto) {
        String email = kakaoUserInfoDto.getKakaoAccount().getEmail();

        Optional<User> user = userRepository.findKakaoUser(email, 0);
        if (user.isPresent()) {
            // ?????? ??????????????? ??? ??????
            return 201;
        }

        return 200;
    }

    @Override
    public String getAuthKey(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void setAuthKey(String key, String value, int expireTime) {
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean hasAuthKey(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public void deleteAuthKey(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public String getEmailByAuthentication(Authentication authentication) {
        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getDetails();
        return userDetails.getUsername();
    }

    @Override
    public Long getIdByAuthentication(Authentication authentication) {
        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getDetails();
        return userDetails.getUser().getId();
    }

    @Override
    public boolean checkMatches(String input, String data) {
        return passwordEncoder.matches(input, data);
    }

}
