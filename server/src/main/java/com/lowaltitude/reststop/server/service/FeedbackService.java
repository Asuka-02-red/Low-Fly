package com.lowaltitude.reststop.server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.FeedbackTicketEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.FeedbackTicketMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RequestIdContext;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;

@Service
public class FeedbackService {

    private final FeedbackTicketMapper feedbackTicketMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditLogService auditLogService;

    public FeedbackService(
            FeedbackTicketMapper feedbackTicketMapper,
            UserAccountMapper userAccountMapper,
            AuditLogService auditLogService
    ) {
        this.feedbackTicketMapper = feedbackTicketMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ApiDtos.FeedbackTicketView createFeedbackTicket(SessionUser user, ApiDtos.FeedbackTicketRequest request) {
        FeedbackTicketEntity ticket = new FeedbackTicketEntity();
        ticket.setTicketNo("FBK" + System.currentTimeMillis());
        ticket.setSubmitUserId(user.id());
        ticket.setSubmitUserRole(user.role().name());
        ticket.setContact(PlatformUtils.normalizeNullable(request.contact()));
        ticket.setDetail(request.detail().trim());
        ticket.setStatus("OPEN");
        feedbackTicketMapper.insert(ticket);
        audit(user, "FEEDBACK", ticket.getTicketNo(), "CREATE", ticket.getDetail());
        return toFeedbackTicketView(ticket, PlatformUtils.displayName(getUserById(user.id())));
    }

    public List<ApiDtos.FeedbackTicketView> listMyFeedbackTickets(SessionUser user) {
        return feedbackTicketMapper.selectList(new LambdaQueryWrapper<FeedbackTicketEntity>()
                        .eq(FeedbackTicketEntity::getSubmitUserId, user.id())
                        .orderByDesc(FeedbackTicketEntity::getCreateTime)
                        .orderByDesc(FeedbackTicketEntity::getId))
                .stream()
                .map(ticket -> toFeedbackTicketView(ticket, PlatformUtils.displayName(getUserById(user.id()))))
                .toList();
    }

    public List<ApiDtos.FeedbackTicketView> listAdminFeedbackTickets() {
        List<FeedbackTicketEntity> tickets = feedbackTicketMapper.selectList(new LambdaQueryWrapper<FeedbackTicketEntity>()
                .orderByDesc(FeedbackTicketEntity::getCreateTime)
                .orderByDesc(FeedbackTicketEntity::getId));
        Map<Long, UserAccountEntity> owners = findUsersByIds(tickets.stream()
                .map(FeedbackTicketEntity::getSubmitUserId)
                .collect(Collectors.toSet()));
        return tickets.stream()
                .map(ticket -> toFeedbackTicketView(ticket, PlatformUtils.displayName(owners.get(ticket.getSubmitUserId()))))
                .toList();
    }

    @Transactional
    public ApiDtos.FeedbackTicketView replyFeedbackTicket(SessionUser admin, Long ticketId, ApiDtos.FeedbackTicketReplyRequest request) {
        PlatformUtils.ensureRole(admin, RoleType.ADMIN);
        FeedbackTicketEntity ticket = getFeedbackTicket(ticketId);
        ticket.setReply(PlatformUtils.normalizeNullable(request.reply()));
        ticket.setStatus(PlatformUtils.normalizeStatus(request.status()));
        if ("CLOSED".equals(ticket.getStatus())) {
            ticket.setCloseTime(LocalDateTime.now());
        } else {
            ticket.setCloseTime(null);
        }
        feedbackTicketMapper.updateById(ticket);
        audit(admin, "FEEDBACK", ticket.getTicketNo(), "REPLY", PlatformUtils.defaultIfBlank(ticket.getReply(), ticket.getStatus()));
        String submitterName = PlatformUtils.displayName(getUserById(ticket.getSubmitUserId()));
        return toFeedbackTicketView(ticket, submitterName);
    }

    private FeedbackTicketEntity getFeedbackTicket(Long id) {
        FeedbackTicketEntity ticket = feedbackTicketMapper.selectById(id);
        if (ticket == null) {
            throw new BizException(404, "工单不存在");
        }
        return ticket;
    }

    UserAccountEntity getUserById(Long id) {
        UserAccountEntity user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    Map<Long, UserAccountEntity> findUsersByIds(Set<Long> ids) {
        Set<Long> distinctIds = ids.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(UserAccountEntity::getId, java.util.function.Function.identity()));
    }

    private ApiDtos.FeedbackTicketView toFeedbackTicketView(FeedbackTicketEntity ticket, String submitterName) {
        return new ApiDtos.FeedbackTicketView(
                ticket.getId(),
                ticket.getTicketNo(),
                submitterName,
                PlatformUtils.displayRole(ticket.getSubmitUserRole()),
                PlatformUtils.defaultIfBlank(ticket.getContact(), "未填写"),
                ticket.getDetail(),
                PlatformUtils.mapFeedbackStatus(ticket.getStatus()),
                PlatformUtils.defaultIfBlank(ticket.getReply(), "待客服回复"),
                PlatformUtils.formatDateTime(ticket.getCreateTime()),
                PlatformUtils.formatDateTime(ticket.getUpdateTime()),
                ticket.getCloseTime() == null ? "-" : PlatformUtils.formatDateTime(ticket.getCloseTime()));
    }

    void audit(SessionUser actor, String bizType, String bizId, String eventType, String payload) {
        Long actorUserId = actor == null ? null : actor.id();
        String actorRole = actor == null ? null : actor.role().name();
        auditLogService.record(
                RequestIdContext.get(),
                actorUserId,
                actorRole,
                bizType,
                bizId,
                eventType,
                payload);
    }
}
