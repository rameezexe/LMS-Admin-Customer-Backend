package com.lms.borrowing.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {
    
    private final String token;
    private final Long memberId;

    public JwtAuthenticationToken(Object principal, Object credentials, 
                                  Collection<? extends GrantedAuthority> authorities, 
                                  String token, Long memberId) {
        super(principal, credentials, authorities);
        this.token = token;
        this.memberId = memberId;
    }

    public String getToken() {
        return token;
    }

    public Long getMemberId() {
        return memberId;
    }
}
