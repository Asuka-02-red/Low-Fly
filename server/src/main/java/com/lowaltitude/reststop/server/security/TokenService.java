package com.lowaltitude.reststop.server.security;

import com.lowaltitude.reststop.server.common.BizException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    public String createToken(SessionUser user) {
        String payload = user.id() + ":" + user.username() + ":" + user.role().name() + ":" + user.displayName();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public SessionUser parse(String token) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = payload.split(":", 4);
            return new SessionUser(Long.parseLong(parts[0]), parts[1], RoleType.valueOf(parts[2]), parts[3]);
        } catch (Exception ex) {
            throw new BizException(401, "无效令牌");
        }
    }
}
