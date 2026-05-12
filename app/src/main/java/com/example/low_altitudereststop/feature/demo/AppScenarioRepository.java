package com.example.low_altitudereststop.feature.demo;

import androidx.annotation.NonNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 应用演示场景数据仓库，构建预置的业务场景数据集。
 * <p>
 * 提供四套完整场景：园区巡检、桥梁建模、活动保障和训练场复检，
 * 每套场景包含对应的飞手、企业、任务、订单、课程、飞行申请、
 * 禁飞区、天气锚点和消息对话数据，供演示模式和离线降级使用。
 * </p>
 */
public final class AppScenarioRepository {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static volatile List<AppScenarioModels.ScenarioBundle> cachedScenarios;

    private AppScenarioRepository() {
    }

    @NonNull
    public static List<AppScenarioModels.ScenarioBundle> buildScenarios() {
        if (cachedScenarios != null) {
            return cachedScenarios;
        }
        synchronized (AppScenarioRepository.class) {
            if (cachedScenarios != null) {
                return cachedScenarios;
            }
            LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
            List<AppScenarioModels.ScenarioBundle> items = new ArrayList<>();
            items.add(buildInspectionScenario(now));
            items.add(buildMappingScenario(now));
            items.add(buildEventScenario(now));
            items.add(buildPolicyScenario(now));
            cachedScenarios = Collections.unmodifiableList(items);
            return cachedScenarios;
        }
    }

    private static AppScenarioModels.ScenarioBundle buildInspectionScenario(LocalDateTime now) {
        AppScenarioModels.ScenarioBundle item = new AppScenarioModels.ScenarioBundle();
        item.conversationIdPilot = 9001L;
        item.conversationIdEnterprise = 9101L;
        item.taskId = 101L;
        item.orderId = 7001L;
        item.courseId = -101L;
        item.zoneRemoteId = 501L;
        item.orderNo = "ORD-20260423-001";
        item.applicationNo = "FLY-20260423-001";

        item.pilot = person("pilot_a01", "刘一飞", "飞手", "13900001201");
        item.company = company("enterprise_dispatch", "云巡科技", "企业调度中心", "苏调度", "023-6718-9001");
        item.task = task(
                "两江新区智慧园区日常巡检",
                "巡检",
                "针对园区楼宇外立面、配电设施和无人机起降点进行例行巡检，重点检查图传链路、异常标注和弱网回传情况。",
                "重庆两江新区数字园区",
                format(now.plusHours(16)),
                "29.56376",
                "106.55046",
                "29.54576",
                "106.52846",
                950,
                "2680",
                "待执行",
                "云巡科技"
        );
        item.order = order("待支付", "2680", "待支付", "企业对公", format(now.minusHours(5)), format(now.plusHours(14)), "对应园区巡检项目首单，建议在起飞前 30 分钟完成支付确认。");
        item.course = course(
                "低空巡检任务标准化实操",
                "围绕园区巡检任务的航前检查、异常上报和图传回传，形成可直接上手的执行路径。",
                "课程大纲\n1. 航前电池与图传链路检查\n2. 标准化巡检航线与取景规范\n3. 异常缺陷拍照与消息回执模板\n4. 弱网环境下的回传与补传策略\n\n配套场景\n- 对应任务：两江新区智慧园区日常巡检\n- 对应消息：企业调度中心的当日巡检安排",
                "巡检规划",
                "OFFLINE",
                "低空驿站飞行学院",
                24,
                8,
                188,
                42,
                "699",
                "LEARNING",
                true,
                "ENR-101",
                "LEARNING"
        );
        item.application = application("两江园区巡检项目", "重庆两江新区数字园区", format(now.plusHours(15)), "园区电力和外立面例行巡检", "待审核", "资料已提交，等待企业审核", "已核对飞手证照与设备清单，待确认起飞窗口。", format(now.minusMinutes(40)));
        item.zone = zone("两江机场净空保护区", "FORBIDDEN", "29.71953", "106.64115", 5000, "2026-04-01 00:00", "2026-12-31 23:59", "机场净空保护", "机场周边飞行严格禁入，园区巡检任务需提前核对航线边界。", true);
        item.weatherAnchor = weather("两江新区数字园区巡检空域", "CQ-LJ-PARK", "春季偏湿，清晨低云和阵风概率较高");

        addMessage(item.pilotMessages, 90011L, "企业调度中心", "企业", false, true, "明天 09:00 开始执行园区巡检，先确认电池循环和图传链路。", now.minusHours(3));
        addMessage(item.pilotMessages, 90012L, "刘一飞", "飞手", true, true, "收到，我会在 08:30 前到场并完成航前检查。", now.minusHours(2).minusMinutes(36));
        addMessage(item.pilotMessages, 90013L, "企业调度中心", "企业", false, false, "最新提醒：B2 楼东侧外立面有历史缺陷点位，请优先复核并及时回传。", now.minusHours(1).minusMinutes(18));

        addMessage(item.enterpriseMessages, 91011L, "系统通知", "系统", false, true, "飞手刘一飞已完成证照同步，可执行两江新区巡检项目。", now.minusHours(3).minusMinutes(8));
        addMessage(item.enterpriseMessages, 91012L, "刘一飞", "飞手", false, true, "收到排班，我会按企业要求执行并回传缺陷照片。", now.minusHours(2).minusMinutes(24));
        addMessage(item.enterpriseMessages, 91013L, "企业调度中心", "企业", true, true, "请在起飞前确认空域边界和禁飞区距离，避免触发净空告警。", now.minusHours(1).minusMinutes(10));
        return item;
    }

    private static AppScenarioModels.ScenarioBundle buildMappingScenario(LocalDateTime now) {
        AppScenarioModels.ScenarioBundle item = new AppScenarioModels.ScenarioBundle();
        item.conversationIdPilot = 9002L;
        item.conversationIdEnterprise = 9102L;
        item.taskId = 102L;
        item.orderId = 7002L;
        item.courseId = -102L;
        item.zoneRemoteId = 502L;
        item.orderNo = "ORD-20260423-002";
        item.applicationNo = "FLY-20260422-018";

        item.pilot = person("pilot_b02", "陈晓航", "飞手", "13900001202");
        item.company = company("enterprise_ops", "城轨智航", "项目调度台", "韩项目经理", "023-6820-3002");
        item.task = task(
                "江北嘴桥梁三维建模采集",
                "测绘",
                "对桥梁主体、桥下通道和临江面进行分层建模采集，要求保留统一命名和控制点标记，便于后续建模拼接。",
                "重庆江北嘴滨江桥区",
                format(now.plusDays(1).withHour(14).withMinute(0)),
                "29.72358",
                "106.63882",
                "29.70558",
                "106.61682",
                1200,
                "4200",
                "执行中",
                "城轨智航"
        );
        item.order = order("已支付", "4200", "已支付", "对公转账", format(now.minusDays(1).withHour(15).withMinute(20)), format(now.plusDays(1).withHour(13).withMinute(30)), "建模项目已确认支付，需同步留存控制点照片与作业日志。");
        item.course = course(
                "桥梁建模航线规划与应急处置",
                "结合临江桥区的风场和遮挡情况，梳理建模采集航线、控制点命名与异常返航策略。",
                "课程内容\n1. 桥梁建模的控制点布设\n2. 临江区域风场识别与返航策略\n3. 建模照片命名规范与素材归档\n4. 与企业项目调度协同的消息模板\n\n关联说明\n- 对应项目：江北嘴桥梁三维建模采集",
                "测绘建模",
                "ARTICLE",
                "城轨智航建模中心",
                999,
                999,
                236,
                58,
                "0",
                "COMPLETED",
                true,
                "ENR-102",
                "COMPLETED"
        );
        item.application = application("江北桥梁建模项目", "重庆江北嘴滨江桥区", format(now.plusDays(1).withHour(14).withMinute(0)), "桥梁三维建模", "已通过", "完成企业审核并下发放行", "符合当前风场和空域要求，可按时执行。", format(now.minusHours(7)));
        item.zone = zone("江北滨江风廊注意区", "RESTRICTED", "29.71662", "106.62780", 1800, "2026-04-01 00:00", "2026-12-31 23:59", "临江风场提醒", "该区域午后横风增强，桥梁建模飞行建议控制高度并缩短单架次航时。", true);
        item.weatherAnchor = weather("江北嘴桥梁建模空域", "CQ-JBZ-BRIDGE", "午后横风明显，能见度通常较好");

        addMessage(item.pilotMessages, 90021L, "项目调度台", "企业", false, true, "明天下午桥梁建模按既定航线执行，优先完成桥面主塔和临江侧采集。", now.minusHours(4));
        addMessage(item.pilotMessages, 90022L, "陈晓航", "飞手", true, true, "已收到，我会带上控制点记录板并按命名规范回传素材。", now.minusHours(3).minusMinutes(20));
        addMessage(item.pilotMessages, 90023L, "项目调度台", "企业", false, true, "注意江北滨江风廊注意区，14:00 后风速可能抬升，建议优先完成临江面。", now.minusHours(1).minusMinutes(42));

        addMessage(item.enterpriseMessages, 91021L, "系统通知", "系统", false, true, "桥梁建模项目已完成支付确认，当前可进入执行准备阶段。", now.minusHours(4).minusMinutes(5));
        addMessage(item.enterpriseMessages, 91022L, "陈晓航", "飞手", false, true, "控制点和素材命名规范已确认，计划按 14:00 起飞窗口执行。", now.minusHours(2).minusMinutes(35));
        addMessage(item.enterpriseMessages, 91023L, "项目调度台", "企业", true, true, "请先完成临江面采集，如风速抬升则切换到桥面主体分段建模。", now.minusHours(1).minusMinutes(25));
        return item;
    }

    private static AppScenarioModels.ScenarioBundle buildEventScenario(LocalDateTime now) {
        AppScenarioModels.ScenarioBundle item = new AppScenarioModels.ScenarioBundle();
        item.conversationIdPilot = 9003L;
        item.conversationIdEnterprise = 9103L;
        item.taskId = 103L;
        item.orderId = 7003L;
        item.courseId = -103L;
        item.zoneRemoteId = 503L;
        item.orderNo = "ORD-20260423-003";
        item.applicationNo = "FLY-20260421-011";

        item.pilot = person("pilot_c03", "周启明", "飞手", "13900001203");
        item.company = company("enterprise_policy", "礼嘉会展保障中心", "保障调度台", "王保障", "023-6999-1803");
        item.task = task(
                "礼嘉会展活动保障巡航",
                "巡检",
                "围绕会展中心外围道路、人流聚集点和临时起降区开展活动保障巡航，重点关注临时管制区域和活动广播时段。",
                "重庆礼嘉会展中心",
                format(now.plusHours(22)),
                "29.62218",
                "106.48765",
                "29.60418",
                "106.46565",
                900,
                "3200",
                "待确认",
                "礼嘉会展保障中心"
        );
        item.order = order("待确认", "3200", "待确认", "平台担保", format(now.minusHours(10)), format(now.plusHours(20)), "活动保障项目等待最终放行，支付将在确认空域后自动触发。");
        item.course = course(
                "低空法规与活动保障政策更新",
                "聚焦临时管制、活动保障和现场协同消息处理，适合活动保障飞手快速复训。",
                "章节安排\n1. 临时活动管制区判读\n2. 活动保障任务的飞手报备要求\n3. 政策更新消息的快速处理方式\n4. 现场广播、返航与应急口令\n\n配套说明\n- 对应场景：礼嘉会展活动保障巡航",
                "法规合规",
                "ARTICLE",
                "低空驿站法规中心",
                999,
                999,
                164,
                41,
                "99",
                "OPEN",
                true,
                "ENR-103",
                "ENROLLED"
        );
        item.application = application("礼嘉会展活动保障", "重庆礼嘉会展中心", format(now.plusHours(22)), "活动保障巡航", "已驳回", "审核驳回，等待重新提交", "飞行时间与临时管制时段重叠，请调整起飞窗口后重提。", format(now.minusHours(2)));
        item.zone = zone("礼嘉会展临时管制区", "RESTRICTED", "29.62218", "106.48765", 2000, format(now.minusDays(1).withHour(8).withMinute(0)), format(now.plusDays(3).withHour(20).withMinute(0)), "重大活动保障", "活动期间仅允许白名单计划飞行，建议提前 24 小时完成报备调整。", false);
        item.weatherAnchor = weather("礼嘉会展保障空域", "CQ-LJ-EXPO", "局地阵雨和人流活动叠加，适合中低风速窗口执行");

        addMessage(item.pilotMessages, 90031L, "保障调度台", "企业", false, true, "礼嘉会展保障任务已进入排期，请先学习最新活动管制提醒。", now.minusHours(6));
        addMessage(item.pilotMessages, 90032L, "周启明", "飞手", true, true, "已查看政策课程，准备调整起飞窗口后重新提交报备。", now.minusHours(4).minusMinutes(15));
        addMessage(item.pilotMessages, 90033L, "保障调度台", "企业", false, false, "临时管制区今天 18:00 后收紧，请把起飞时间改到明日 09:30 后。", now.minusHours(1));

        addMessage(item.enterpriseMessages, 91031L, "系统通知", "系统", false, true, "礼嘉会展活动保障任务触发临时管制冲突，请尽快协调飞手调整起飞窗口。", now.minusHours(6).minusMinutes(5));
        addMessage(item.enterpriseMessages, 91032L, "周启明", "飞手", false, true, "已根据政策课程检查管制时段，建议改为明日 09:30 起飞。", now.minusHours(3).minusMinutes(20));
        addMessage(item.enterpriseMessages, 91033L, "保障调度台", "企业", true, true, "收到，等你重新提交报备后我来完成活动保障审批。", now.minusHours(1).minusMinutes(12));
        return item;
    }

    private static AppScenarioModels.ScenarioBundle buildPolicyScenario(LocalDateTime now) {
        AppScenarioModels.ScenarioBundle item = new AppScenarioModels.ScenarioBundle();
        item.conversationIdPilot = 9004L;
        item.conversationIdEnterprise = 9104L;
        item.taskId = 104L;
        item.orderId = 7004L;
        item.courseId = -104L;
        item.zoneRemoteId = 504L;
        item.orderNo = "ORD-20260423-004";
        item.applicationNo = "FLY-20260420-008";

        item.pilot = person("pilot_self", "陈伶", "飞手", "13900001204");
        item.company = company("enterprise_sys", "平台服务助手", "平台服务助手", "平台值班", "023-6700-0000");
        item.task = task(
                "航前设备维护与训练场复检",
                "巡检",
                "面向新人飞手的设备维护和训练场复检任务，重点在电池循环、图传自检和返航点确认。",
                "重庆照母山训练场",
                format(now.plusDays(2).withHour(10).withMinute(30)),
                "29.65138",
                "106.52910",
                "29.63338",
                "106.50710",
                800,
                "980",
                "待开始",
                "平台服务助手"
        );
        item.order = order("待支付", "980", "待支付", "线上支付", format(now.minusDays(1).withHour(18).withMinute(15)), format(now.plusDays(2).withHour(10).withMinute(0)), "适合训练和复训场景，完成后会同步到学习与消息模块。");
        item.course = course(
                "设备维护与航前检查实操",
                "面向训练场和复训任务的电池、图传和返航设置检查，适合准备阶段学习。",
                "课程内容\n- 电池循环检查\n- 图传链路自检\n- 起飞前返航点与失联策略复核\n\n学习说明\n- 适合训练场复训\n- 完成后可直接对应训练场复检任务",
                "运维保障",
                "OFFLINE",
                "低空驿站设备中心",
                20,
                12,
                96,
                17,
                "299",
                "OPEN",
                false,
                "",
                ""
        );
        item.application = application("训练场复检任务", "重庆照母山训练场", format(now.plusDays(2).withHour(10).withMinute(30)), "训练场设备复检", "待准备", "等待飞手提交设备检查清单", "建议先完成设备维护课程后再提交。", format(now.minusDays(1)));
        item.zone = zone("照母山训练场周边提醒区", "RESTRICTED", "29.65138", "106.52910", 1200, "2026-04-01 00:00", "2026-12-31 23:59", "训练场周边低空提醒", "主要用于训练任务的航线边界提示，不影响白名单训练计划。", true);
        item.weatherAnchor = weather("照母山训练场空域", "CQ-ZMS-TRAIN", "上午能见度较稳，适合训练与复检");

        addMessage(item.pilotMessages, 90041L, "平台服务助手", "系统", false, true, "你的训练场复检任务已生成，建议先完成设备维护课程。", now.minusDays(1).withHour(18).withMinute(25));
        addMessage(item.pilotMessages, 90042L, "陈伶", "飞手", true, true, "收到，我会先检查电池循环和返航点配置。", now.minusDays(1).withHour(18).withMinute(42));
        addMessage(item.pilotMessages, 90043L, "平台服务助手", "系统", false, false, "提醒：完成课程后可直接提交设备检查清单，训练场任务将自动进入待确认。", now.minusHours(2));

        addMessage(item.enterpriseMessages, 91041L, "系统通知", "系统", false, true, "训练场复检任务已派发，待飞手完成设备维护课程后可继续推进。", now.minusDays(1).withHour(18).withMinute(20));
        addMessage(item.enterpriseMessages, 91042L, "陈伶", "飞手", false, true, "我会先完成设备课程，再提交检查清单。", now.minusHours(12));
        addMessage(item.enterpriseMessages, 91043L, "平台服务助手", "系统", true, true, "已记录飞手反馈，后续将自动同步课程完成状态。", now.minusHours(2).minusMinutes(15));
        return item;
    }

    private static AppScenarioModels.Person person(String uid, String name, String roleTitle, String phone) {
        AppScenarioModels.Person item = new AppScenarioModels.Person();
        item.uid = uid;
        item.name = name;
        item.roleTitle = roleTitle;
        item.phone = phone;
        return item;
    }

    private static AppScenarioModels.Company company(String uid, String name, String dispatchTitle, String contactName, String contactPhone) {
        AppScenarioModels.Company item = new AppScenarioModels.Company();
        item.uid = uid;
        item.name = name;
        item.dispatchTitle = dispatchTitle;
        item.contactName = contactName;
        item.contactPhone = contactPhone;
        return item;
    }

    private static AppScenarioModels.Task task(
            String title,
            String taskType,
            String description,
            String location,
            String deadline,
            String latitude,
            String longitude,
            String routeStartLatitude,
            String routeStartLongitude,
            int radius,
            String budget,
            String status,
            String ownerName
    ) {
        AppScenarioModels.Task item = new AppScenarioModels.Task();
        item.title = title;
        item.taskType = taskType;
        item.description = description;
        item.location = location;
        item.deadline = deadline;
        item.latitude = new BigDecimal(latitude);
        item.longitude = new BigDecimal(longitude);
        item.routeStartLatitude = new BigDecimal(routeStartLatitude);
        item.routeStartLongitude = new BigDecimal(routeStartLongitude);
        item.operationRadiusMeters = radius;
        item.budget = new BigDecimal(budget);
        item.status = status;
        item.ownerName = ownerName;
        return item;
    }

    private static AppScenarioModels.Order order(
            String status,
            String amount,
            String paymentStatus,
            String paymentChannel,
            String createdAt,
            String appointmentTime,
            String remark
    ) {
        AppScenarioModels.Order item = new AppScenarioModels.Order();
        item.status = status;
        item.amount = new BigDecimal(amount);
        item.paymentStatus = paymentStatus;
        item.paymentChannel = paymentChannel;
        item.createdAt = createdAt;
        item.appointmentTime = appointmentTime;
        item.remark = remark;
        return item;
    }

    private static AppScenarioModels.Course course(
            String title,
            String summary,
            String content,
            String category,
            String learningMode,
            String institution,
            int seatTotal,
            int seatAvailable,
            int browseCount,
            int enrollCount,
            String price,
            String status,
            boolean enrolled,
            String enrollmentNo,
            String enrollmentStatus
    ) {
        AppScenarioModels.Course item = new AppScenarioModels.Course();
        item.title = title;
        item.summary = summary;
        item.content = content;
        item.category = category;
        item.learningMode = learningMode;
        item.institutionName = institution;
        item.seatTotal = seatTotal;
        item.seatAvailable = seatAvailable;
        item.browseCount = browseCount;
        item.enrollCount = enrollCount;
        item.price = new BigDecimal(price);
        item.status = status;
        item.enrolled = enrolled;
        item.enrollmentNo = enrollmentNo;
        item.enrollmentStatus = enrollmentStatus;
        return item;
    }

    private static AppScenarioModels.FlightApplication application(
            String projectName,
            String location,
            String flightTime,
            String purpose,
            String status,
            String workflowStatus,
            String approvalOpinion,
            String updatedAt
    ) {
        AppScenarioModels.FlightApplication item = new AppScenarioModels.FlightApplication();
        item.projectName = projectName;
        item.location = location;
        item.flightTime = flightTime;
        item.purpose = purpose;
        item.status = status;
        item.workflowStatus = workflowStatus;
        item.approvalOpinion = approvalOpinion;
        item.updatedAt = updatedAt;
        return item;
    }

    private static AppScenarioModels.Zone zone(
            String name,
            String zoneType,
            String lat,
            String lng,
            int radius,
            String effectiveStart,
            String effectiveEnd,
            String reason,
            String description,
            boolean builtIn
    ) {
        AppScenarioModels.Zone item = new AppScenarioModels.Zone();
        item.name = name;
        item.zoneType = zoneType;
        item.centerLat = new BigDecimal(lat);
        item.centerLng = new BigDecimal(lng);
        item.radius = radius;
        item.effectiveStart = effectiveStart;
        item.effectiveEnd = effectiveEnd;
        item.reason = reason;
        item.description = description;
        item.builtIn = builtIn;
        return item;
    }

    private static AppScenarioModels.WeatherAnchor weather(String locationName, String adcode, String seasonHint) {
        AppScenarioModels.WeatherAnchor item = new AppScenarioModels.WeatherAnchor();
        item.locationName = locationName;
        item.adcode = adcode;
        item.seasonHint = seasonHint;
        return item;
    }

    private static void addMessage(List<AppScenarioModels.MessageLine> target, long messageId, String senderName, String senderRole, boolean mine, boolean isRead, String content, LocalDateTime time) {
        AppScenarioModels.MessageLine item = new AppScenarioModels.MessageLine();
        item.messageId = messageId;
        item.senderName = senderName;
        item.senderRole = senderRole;
        item.mine = mine;
        item.isRead = isRead;
        item.content = content;
        item.timeMillis = time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        target.add(item);
    }

    private static String format(LocalDateTime time) {
        return time.format(DATE_TIME);
    }
}
