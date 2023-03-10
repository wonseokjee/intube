package com.ssafy.interview.config;

import com.ssafy.interview.api.service.user.AuthService;
import com.ssafy.interview.common.auth.JwtAuthenticationFilter;
import com.ssafy.interview.common.auth.SsafyUserDetailService;
import com.ssafy.interview.db.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 인증(authentication) 와 인가(authorization) 처리를 위한 스프링 시큐리티 설정 정의.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private SsafyUserDetailService ssafyUserDetailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    // Password 인코딩 방식에 BCrypt 암호화 방식 사용
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // DAO 기반으로 Authentication Provider를 생성
    // BCrypt Password Encoder와 UserDetailService 구현체를 설정
    @Bean
    DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        daoAuthenticationProvider.setUserDetailsService(this.ssafyUserDetailService);
        return daoAuthenticationProvider;
    }

    // DAO 기반의 Authentication Provider가 적용되도록 설정
    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .httpBasic().disable()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 토큰 기반 인증이므로 세션 사용 하지않음
                .and()
                .addFilter(new JwtAuthenticationFilter(authenticationManager(), userRepository, authService)) //HTTP 요청에 JWT 토큰 인증 필터를 거치도록 필터를 추가
                .authorizeRequests()
                //인증이 필요한 URL과 필요하지 않은 URL에 대하여 설정
                .antMatchers("/user/me").authenticated()
                .antMatchers("/user/image").authenticated()
                .antMatchers("/user/interviewer/**").authenticated()
                .antMatchers("/user/interviewee/mypage").authenticated()
                .antMatchers("/user/interviewer").authenticated()
                .antMatchers("/user/interviewee").authenticated()
                .antMatchers("/interviews/apply/**").authenticated()
                .antMatchers("/interviews/cancel/**").authenticated()
                .antMatchers("/interviews/search").authenticated()
                .antMatchers("/interviews/delete").authenticated()
                .antMatchers("/interviews/interviewer/expired-interview").authenticated()
                .antMatchers("/interviews/interviewer/finish-interview").authenticated()
                .antMatchers("/result/create").authenticated()
                .antMatchers("/result/search").authenticated()
                .antMatchers("/result/modify").authenticated()
                .antMatchers("/result/modify/all").authenticated()
                .antMatchers("/result/search/dialog").authenticated()
                .antMatchers("/result/delete/dialog").authenticated()
                .antMatchers(HttpMethod.GET, "/interviews/search/{interview_id}").authenticated()
                .antMatchers(HttpMethod.PUT, "/user").authenticated()
                .antMatchers("/conference/start").authenticated()
                .antMatchers("/conference/in").authenticated()
                .antMatchers(HttpMethod.PUT, "/user/password").authenticated()
                .antMatchers(HttpMethod.POST, "/interviews").authenticated()
                .antMatchers(HttpMethod.POST, "/auth/check-password").authenticated()
                .antMatchers(HttpMethod.DELETE, "/auth/logout").authenticated()
                .antMatchers(HttpMethod.DELETE, "/user").authenticated()
                .antMatchers("/auth").permitAll()
                .antMatchers(HttpMethod.POST, "/user").permitAll()
                .antMatchers("/user/nickname", "/user/find-email", "/user/find-password").permitAll()
                .anyRequest().permitAll()
                .and().cors();
    }
}