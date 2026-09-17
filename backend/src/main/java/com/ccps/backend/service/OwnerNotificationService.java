package com.ccps.backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.OwnerNotificationResponse;
import com.ccps.backend.dto.OwnerNotificationResponse.Category;
import com.ccps.backend.dto.OwnerNotificationResponse.Channel;
import com.ccps.backend.dto.OwnerNotificationResponse.NotificationItem;
import com.ccps.backend.dto.OwnerNotificationResponse.PendingTask;
import com.ccps.backend.dto.OwnerNotificationResponse.Summary;
import com.ccps.backend.mapper.OwnerNotificationMapper;
import com.ccps.backend.mapper.OwnerNotificationMapper.NotificationRow;
import com.ccps.backend.mapper.OwnerNotificationMapper.EmailSubscriptionRow;
import com.ccps.backend.mapper.OwnerNotificationMapper.UnitReference;

@Service
public class OwnerNotificationService {
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final OwnerNotificationMapper mapper;

    public OwnerNotificationService(OwnerNotificationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public OwnerNotificationResponse getNotifications(Long userId) {
        List<NotificationRow> rows = mapper.findNotifications(userId);
        List<UnitReference> units = mapper.findOwnerUnits(userId);
        List<NotificationItem> items = rows.stream()
                .map(row -> toItem(row, units))
                .toList();

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("all", items.size());
        items.forEach(item -> counts.merge(item.category(), 1, Integer::sum));
        List<Category> categories = List.of(
                new Category("all", "全部通知", counts.getOrDefault("all", 0)),
                new Category("payment", "房款提醒", counts.getOrDefault("payment", 0)),
                new Category("rent", "租金通知", counts.getOrDefault("rent", 0)),
                new Category("reserve", "預備金提醒", counts.getOrDefault("reserve", 0)),
                new Category("maintenance", "維修通知", counts.getOrDefault("maintenance", 0)),
                new Category("document", "文件到期", counts.getOrDefault("document", 0)),
                new Category("system", "系統公告", counts.getOrDefault("system", 0)));

        int unread = (int) items.stream().filter(item -> !"read".equalsIgnoreCase(item.status())).count();
        int important = (int) items.stream()
                .filter(item -> "high".equalsIgnoreCase(item.priority())
                        || "urgent".equalsIgnoreCase(item.priority()))
                .count();
        int monthSystem = (int) items.stream()
                .filter(item -> "system".equals(item.category()) && item.createdAt() != null
                        && item.createdAt().toLocalDate().getMonth() == LocalDate.now().getMonth()
                        && item.createdAt().toLocalDate().getYear() == LocalDate.now().getYear())
                .count();
        List<PendingTask> tasks = items.stream()
                .filter(item -> !"read".equalsIgnoreCase(item.status()))
                .limit(5)
                .map(item -> new PendingTask(
                        item.category(), item.title(), taskDetail(item), 1, item.priority()))
                .toList();

        EmailSubscriptionRow emailSubscription = mapper.findEmailSubscription(userId);
        boolean emailVerified = emailSubscription != null && emailSubscription.getVerifiedAt() != null;
        String emailStatus = emailSubscription == null ? "unbound"
                : !emailVerified ? "pending"
                : Boolean.TRUE.equals(emailSubscription.getEnabled()) ? "enabled" : "disabled";

        return new OwnerNotificationResponse(
                new Summary(unread, tasks.size(), monthSystem, important, items.size()),
                categories,
                items,
                tasks,
                List.of(new Channel("in_app", "站內通知", "enabled", null, true),
                        new Channel("email", "郵件", emailStatus,
                                emailSubscription == null ? null : emailSubscription.getDestination(), emailVerified)));
    }

    @Transactional(readOnly = true)
    public List<NotificationItem> getPropertyNotifications(Long userId, Long ownerUnitId) {
        UnitReference property = mapper.findOwnerUnit(userId, ownerUnitId);
        if (property == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }
        return getNotifications(userId).notifications().stream()
                .filter(item -> Objects.equals(property.getProjectName(), item.projectName())
                        && Objects.equals(property.getUnitNo(), item.unitNo()))
                .toList();
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        if (mapper.markRead(userId, notificationId) == 0) {
            throw new IllegalArgumentException("Notification not found");
        }
    }

    @Transactional
    public int markAllRead(Long userId) {
        return mapper.markAllRead(userId);
    }

    private NotificationItem toItem(NotificationRow row, List<UnitReference> units) {
        String text = ((Objects.toString(row.getTitle(), "") + " " + Objects.toString(row.getBody(), ""))).toLowerCase(Locale.ROOT);
        String category = categoryOf(row.getRelatedType(), text);
        String project = row.getProjectName();
        String unit = row.getUnitNo();
        String city = row.getCity();
        if (unit == null || project == null) {
            UnitReference reference = inferReference(text, units, project, unit);
            if (reference != null) {
                project = firstNonBlank(project, reference.getProjectName());
                unit = firstNonBlank(unit, reference.getUnitNo());
                city = firstNonBlank(city, reference.getCity());
            }
        }
        String body = Objects.toString(row.getBody(), "");
        String detail = body.isBlank() ? row.getTitle() : body;
        return new NotificationItem(
                row.getId(), category, row.getTitle(), body, priority(row.getPriority()),
                row.getStatus(), row.getCreatedAt(), project, unit, city,
                number(row), null, null, detail, body);
    }

    private UnitReference inferReference(String text, List<UnitReference> units, String project, String unit) {
        List<UnitReference> candidates = units.stream()
                .filter(ref -> project == null || project.equalsIgnoreCase(ref.getProjectName()))
                .filter(ref -> unit == null || unit.equalsIgnoreCase(ref.getUnitNo()))
                .filter(ref -> {
                    String full = Objects.toString(ref.getUnitNo(), "").toLowerCase(Locale.ROOT);
                    String shortened = full.contains("-") ? full.substring(full.indexOf('-') + 1) : full;
                    return containsUnit(text, full) || containsUnit(text, shortened);
                }).toList();
        List<UnitReference> named = candidates.stream().filter(ref -> {
            String name = Objects.toString(ref.getProjectName(), "").toLowerCase(Locale.ROOT);
            return !name.isBlank() && text.contains(name);
        }).toList();
        if (named.size() == 1) return named.get(0);
        // Legacy notices may omit project names; infer only when the unit is unambiguous.
        return named.isEmpty() && candidates.size() == 1 ? candidates.get(0) : null;
    }

    private boolean containsUnit(String text, String unit) {
        return !unit.isBlank() && java.util.regex.Pattern.compile(
                "(?<![a-z0-9.\\-])" + java.util.regex.Pattern.quote(unit) + "(?![a-z0-9.\\-])")
                .matcher(text).find();
    }

    private String categoryOf(String relatedType, String text) {
        String type = Objects.toString(relatedType, "").toLowerCase(Locale.ROOT);
        if (type.contains("payment") || type.contains("finance")) return "payment";
        if (type.contains("rent")) return "rent";
        if (type.contains("reserve")) return "reserve";
        if (type.contains("maintenance") || type.contains("work_order")) return "maintenance";
        if (type.contains("document") || type.contains("contract")) return "document";
        if (type.contains("system")) return "system";
        if (containsAny(text, "房款", "付款", "繳費", "payment")) return "payment";
        if (containsAny(text, "租金", "rent")) return "rent";
        if (containsAny(text, "預備金", "reserve")) return "reserve";
        if (containsAny(text, "維修", "maintenance")) return "maintenance";
        if (containsAny(text, "文件", "合約", "租賃協議", "document")) return "document";
        return "system";
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    private String priority(String value) {
        if (value == null || value.isBlank()) return "normal";
        return value;
    }

    private String number(NotificationRow row) {
        String date = row.getCreatedAt() == null ? LocalDate.now().format(NUMBER_DATE)
                : row.getCreatedAt().toLocalDate().format(NUMBER_DATE);
        return "NT-" + date + "-" + String.format("%04d", row.getId());
    }

    private String taskDetail(NotificationItem item) {
        List<String> values = new ArrayList<>();
        if (item.projectName() != null) values.add(item.projectName());
        if (item.unitNo() != null) values.add(item.unitNo());
        return values.isEmpty() ? item.body() : String.join(" · ", values);
    }

    private String firstNonBlank(String original, String fallback) {
        return original == null || original.isBlank() ? fallback : original;
    }
}
