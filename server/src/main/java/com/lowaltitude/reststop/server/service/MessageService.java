package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.MessageConversationEntity;
import com.lowaltitude.reststop.server.entity.MessageEntryEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.MessageConversationMapper;
import com.lowaltitude.reststop.server.mapper.MessageEntryMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RequestIdContext;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private final MessageConversationMapper messageConversationMapper;
    private final MessageEntryMapper messageEntryMapper;
    private final UserAccountMapper userAccountMapper;
    private final TaskService taskService;
    private final AuditLogService auditLogService;

    public MessageService(
            MessageConversationMapper messageConversationMapper,
            MessageEntryMapper messageEntryMapper,
            UserAccountMapper userAccountMapper,
            TaskService taskService,
            AuditLogService auditLogService
    ) {
        this.messageConversationMapper = messageConversationMapper;
        this.messageEntryMapper = messageEntryMapper;
        this.userAccountMapper = userAccountMapper;
        this.taskService = taskService;
        this.auditLogService = auditLogService;
    }

    public List<ApiDtos.MessageConversationView> listMessageConversations(SessionUser user) {
        List<MessageConversationEntity> conversations = findConversationsForUser(user);
        Map<Long, UserAccountEntity> users = findUsersByIds(conversations.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getEnterpriseId(), item.getPilotId()))
                .collect(Collectors.toSet()));

        return conversations.stream()
                .map(conversation -> {
                    UserAccountEntity counterpart = resolveCounterpart(user, conversation, users);
                    MessageEntryEntity latest = findLatestMessage(conversation.getId());
                    int unreadCount = countCounterpartMessages(conversation.getId(), user.id());
                    String pilotUid = PlatformUtils.toUid(conversation.getPilotId());
                    String enterpriseUid = PlatformUtils.toUid(conversation.getEnterpriseId());
                    return new ApiDtos.MessageConversationView(
                            conversation.getId(),
                            conversationTitle(conversation, counterpart),
                            conversationSubtitle(conversation),
                            PlatformUtils.displayName(counterpart),
                            PlatformUtils.toUid(counterpart == null ? null : counterpart.getId()),
                            PlatformUtils.displayRole(counterpart == null ? null : counterpart.getRole()),
                            pilotUid,
                            enterpriseUid,
                            latest == null ? "暂无消息，立即发起协同沟通。" : latest.getContent(),
                            PlatformUtils.formatDateTime(latest == null ? conversation.getLastMessageTime() : latest.getCreateTime()),
                            unreadCount);
                })
                .toList();
    }

    public ApiDtos.MessageThreadView getMessageThread(SessionUser user, Long conversationId) {
        MessageConversationEntity conversation = getConversation(conversationId);
        ensureConversationAccess(user, conversation);
        Map<Long, UserAccountEntity> users = findUsersByIds(Set.of(conversation.getEnterpriseId(), conversation.getPilotId()));
        UserAccountEntity counterpart = resolveCounterpart(user, conversation, users);
        List<ApiDtos.MessageEntryView> messages = messageEntryMapper.selectList(new LambdaQueryWrapper<MessageEntryEntity>()
                        .eq(MessageEntryEntity::getConversationId, conversationId)
                        .orderByAsc(MessageEntryEntity::getCreateTime)
                        .orderByAsc(MessageEntryEntity::getId))
                .stream()
                .map(entry -> toMessageEntryView(entry, users.get(entry.getSenderUserId()), user.id()))
                .toList();
        return new ApiDtos.MessageThreadView(
                conversation.getId(),
                conversationTitle(conversation, counterpart),
                conversationSubtitle(conversation),
                PlatformUtils.toUid(conversation.getPilotId()),
                PlatformUtils.toUid(conversation.getEnterpriseId()),
                messages);
    }

    public ApiDtos.MessageReadReceiptResponse syncReadReceipts(SessionUser user, ApiDtos.MessageReadReceiptRequest request) {
        List<Long> syncedIds = new ArrayList<>();
        for (Long msgId : request.msgIds()) {
            if (msgId == null) {
                continue;
            }
            MessageEntryEntity entry = messageEntryMapper.selectById(msgId);
            if (entry == null) {
                continue;
            }
            MessageConversationEntity conversation = getConversation(entry.getConversationId());
            ensureConversationAccess(user, conversation);
            syncedIds.add(msgId);
        }
        audit(user, "MESSAGE", String.valueOf(user.id()), "READ_RECEIPT", "count=" + syncedIds.size());
        return new ApiDtos.MessageReadReceiptResponse(syncedIds.size(), syncedIds);
    }

    @Transactional
    public ApiDtos.MessageThreadView sendMessage(SessionUser user, Long conversationId, ApiDtos.MessageSendRequest request) {
        MessageConversationEntity conversation = getConversation(conversationId);
        ensureConversationAccess(user, conversation);
        MessageEntryEntity entry = new MessageEntryEntity();
        entry.setConversationId(conversationId);
        entry.setSenderUserId(user.id());
        entry.setSenderRole(user.role().name());
        entry.setContent(request.content().trim());
        entry.setCreateTime(LocalDateTime.now());
        messageEntryMapper.insert(entry);
        conversation.setLastMessageTime(entry.getCreateTime());
        messageConversationMapper.updateById(conversation);
        audit(user, "MESSAGE", String.valueOf(conversationId), "SEND", request.content().trim());
        return getMessageThread(user, conversationId);
    }

    public ApiDtos.PilotProfileView getPilotProfile(SessionUser user, String uid) {
        UserAccountEntity target = requireUserByUid(uid);
        if (RoleType.valueOf(target.getRole()) != RoleType.PILOT) {
            throw new BizException(404, "飞手不存在");
        }
        return new ApiDtos.PilotProfileView(PlatformUtils.toUid(target.getId()), PlatformUtils.displayName(target));
    }

    public ApiDtos.EnterpriseInfoView getEnterpriseInfo(SessionUser user, String uid) {
        UserAccountEntity target = requireUserByUid(uid);
        if (RoleType.valueOf(target.getRole()) != RoleType.ENTERPRISE) {
            throw new BizException(404, "企业不存在");
        }
        return new ApiDtos.EnterpriseInfoView(PlatformUtils.toUid(target.getId()), PlatformUtils.defaultIfBlank(target.getCompanyName(), PlatformUtils.displayName(target)));
    }

    MessageConversationEntity getConversation(Long id) {
        MessageConversationEntity conversation = messageConversationMapper.selectById(id);
        if (conversation == null) {
            throw new BizException(404, "会话不存在");
        }
        return conversation;
    }

    private List<MessageConversationEntity> findConversationsForUser(SessionUser user) {
        if (user.role() == RoleType.ENTERPRISE) {
            return messageConversationMapper.selectList(new LambdaQueryWrapper<MessageConversationEntity>()
                    .eq(MessageConversationEntity::getEnterpriseId, user.id())
                    .orderByDesc(MessageConversationEntity::getLastMessageTime)
                    .orderByDesc(MessageConversationEntity::getId));
        }
        if (user.role() == RoleType.PILOT) {
            return messageConversationMapper.selectList(new LambdaQueryWrapper<MessageConversationEntity>()
                    .eq(MessageConversationEntity::getPilotId, user.id())
                    .orderByDesc(MessageConversationEntity::getLastMessageTime)
                    .orderByDesc(MessageConversationEntity::getId));
        }
        return List.of();
    }

    private void ensureConversationAccess(SessionUser user, MessageConversationEntity conversation) {
        boolean accessible = user.role() == RoleType.ADMIN
                || Objects.equals(conversation.getEnterpriseId(), user.id())
                || Objects.equals(conversation.getPilotId(), user.id());
        if (!accessible) {
            throw new BizException(403, "当前用户无权访问该会话");
        }
    }

    private UserAccountEntity resolveCounterpart(SessionUser user, MessageConversationEntity conversation, Map<Long, UserAccountEntity> users) {
        Long counterpartId = Objects.equals(conversation.getEnterpriseId(), user.id())
                ? conversation.getPilotId()
                : conversation.getEnterpriseId();
        return users.get(counterpartId);
    }

    private MessageEntryEntity findLatestMessage(Long conversationId) {
        return messageEntryMapper.selectOne(new LambdaQueryWrapper<MessageEntryEntity>()
                .eq(MessageEntryEntity::getConversationId, conversationId)
                .orderByDesc(MessageEntryEntity::getCreateTime)
                .orderByDesc(MessageEntryEntity::getId)
                .last("limit 1"));
    }

    int countCounterpartMessages(Long conversationId, Long currentUserId) {
        Long count = messageEntryMapper.selectCount(new LambdaQueryWrapper<MessageEntryEntity>()
                .eq(MessageEntryEntity::getConversationId, conversationId)
                .ne(MessageEntryEntity::getSenderUserId, currentUserId));
        return count == null ? 0 : count.intValue();
    }

    private ApiDtos.MessageEntryView toMessageEntryView(MessageEntryEntity entry, UserAccountEntity sender, Long currentUserId) {
        MessageConversationEntity conversation = getConversation(entry.getConversationId());
        return new ApiDtos.MessageEntryView(
                entry.getId(),
                entry.getSenderUserId(),
                PlatformUtils.toUid(conversation.getPilotId()),
                PlatformUtils.toUid(conversation.getEnterpriseId()),
                PlatformUtils.displayName(sender),
                PlatformUtils.displayRole(entry.getSenderRole()),
                entry.getContent(),
                PlatformUtils.formatDateTime(entry.getCreateTime()),
                Objects.equals(entry.getSenderUserId(), currentUserId),
                Objects.equals(entry.getSenderUserId(), currentUserId));
    }

    String conversationTitle(MessageConversationEntity conversation, UserAccountEntity counterpart) {
        String counterpartName = PlatformUtils.displayName(counterpart);
        String title = PlatformUtils.defaultIfBlank(conversation.getTitle(), "协同沟通");
        if (counterpartName.equals("-")) {
            return title;
        }
        return counterpartName + " / " + title;
    }

    String conversationSubtitle(MessageConversationEntity conversation) {
        if (conversation.getTaskId() == null) {
            return "企业与飞手实时协同";
        }
        try {
            TaskEntity task = taskService.getTaskById(conversation.getTaskId());
            return PlatformUtils.defaultIfBlank(task.getLocation(), "协同沟通") + " · " + PlatformUtils.defaultIfBlank(task.getTaskType(), "任务沟通");
        } catch (BizException ignored) {
            return "企业与飞手实时协同";
        }
    }

    private UserAccountEntity requireUserByUid(String uid) {
        if (PlatformUtils.isBlank(uid)) {
            throw new BizException(404, "用户不存在");
        }
        if (uid.chars().allMatch(Character::isDigit)) {
            return getUserById(Long.parseLong(uid));
        }
        return getUserByUsername(uid.trim());
    }

    UserAccountEntity getUserById(Long id) {
        UserAccountEntity user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    UserAccountEntity getUserByUsername(String username) {
        UserAccountEntity user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private Map<Long, UserAccountEntity> findUsersByIds(Set<Long> ids) {
        Set<Long> distinctIds = ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(UserAccountEntity::getId, Function.identity()));
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
