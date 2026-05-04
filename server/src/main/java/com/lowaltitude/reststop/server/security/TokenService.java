package com.lowaltitude.reststop.server.security;

import com.lowaltitude.reststop.server.common.BizException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Service;

/**
 * 令牌服务。
 * <p>
 * 负责创建和解析Base64编码的认证令牌，将SessionUser信息序列化为令牌字符串，
 * 以及从令牌反序列化恢复SessionUser对象，令牌无效时抛出401业务异常。
 * </p>
 */
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
