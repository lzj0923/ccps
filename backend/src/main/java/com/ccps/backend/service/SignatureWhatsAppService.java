package com.ccps.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import com.ccps.backend.mapper.WhatsAppNotificationMapper;
import com.ccps.backend.mapper.WhatsAppNotificationMapper.NewAttempt;

/** Explicit administrator sends only. Never starts or enables the reminder scheduler. */
@Service
public class SignatureWhatsAppService {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final WhatsAppNotificationMapper attempts;
    private final WhatsAppGraphClient client;
    private final String base;

    @Autowired
    public SignatureWhatsAppService(JdbcTemplate db, PlatformTransactionManager transactions,
            WhatsAppNotificationMapper attempts, RestClient.Builder builder,
            @Value("${WHATSAPP_SIGNATURE_ENABLED:false}") boolean enabled,
            @Value("${ccps.whatsapp.graph-api-version:v26.0}") String version,
            @Value("${ccps.whatsapp.phone-number-id:}") String phoneId,
            @Value("${ccps.whatsapp.access-token:}") String token,
            @Value("${ccps.whatsapp.proxy-url:}") String proxy,
            @Value("${ccps.signing.frontend-base:}") String base) {
        this(db, new TransactionTemplate(transactions), attempts,
                new WhatsAppGraphClient(builder, enabled, version, phoneId, token, proxy), base);
    }

    SignatureWhatsAppService(JdbcTemplate db, TransactionTemplate tx, WhatsAppNotificationMapper attempts,
            WhatsAppGraphClient client, String base) {
        this.db=db; this.tx=tx; this.attempts=attempts; this.client=client;
        this.base=Objects.toString(base, "").replaceAll("/+$", "");
    }

    public record State(String phone, String status, boolean available) {}
    record Prepared(Long deliveryId, Long attemptId, String destination, List<String> parameters, State existing) {}

    public State state(Long id) {
        Map<String,Object> row = request(id, null, false);
        State existing = existing(id);
        if (existing != null) return existing;
        return new State(recipient(row), "ready", client.configured());
    }

    public State send(Long actor, Long id, String token, String suppliedPhone) {
        if (!client.configured() || !base.startsWith("https://"))
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "WhatsApp签署发送尚未配置，请联系管理员");
        Prepared prepared = tx.execute(status -> prepare(actor, id, token, suppliedPhone));
        if (prepared.existing()!=null) return prepared.existing();
        // Durable 'sending' trace is committed BEFORE the external request. A repeat never resends.
        try {
            var result = client.sendTemplate(prepared.destination(), "ccps_signature_invitation", "zh_CN", prepared.parameters());
            attempts.markAttemptAccepted(prepared.attemptId(), result.messageId(), result.waId());
            attempts.markDeliveryAccepted(prepared.deliveryId());
        } catch (WhatsAppGraphClient.WhatsAppGraphException e) {
            String state = e.uncertain() ? "unknown" : "failed";
            attempts.markAttemptFailed(prepared.attemptId(), state, e.code(), e.subcode(),
                    "WhatsApp signature send " + state, e.traceId());
            attempts.markDeliveryFailed(prepared.deliveryId(), state, "WhatsApp签署发送未完成，请联系管理员核查，勿重复发送");
        } catch (RuntimeException e) {
            // Could have been accepted before a database/transport failure. Never automatically retry.
            attempts.markAttemptFailed(prepared.attemptId(), "unknown", null, null, "Signature send outcome unknown", null);
            attempts.markDeliveryFailed(prepared.deliveryId(), "unknown", "发送结果待核实，请勿重复发送");
        }
        return existing(id);
    }

    Prepared prepare(Long actor, Long id, String token, String suppliedPhone) {
        Map<String,Object> row = request(id, token, true);
        State previous = existing(id);
        if (previous != null) return new Prepared(null,null,null,null,previous);
        String automatic = recipient(row); // Also checks tenant opt-out even for explicit destinations.
        String phone = strictPhone(suppliedPhone == null || suppliedPhone.isBlank() ? automatic : suppliedPhone);
        if (phone.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "请填写该签署人的WhatsApp国际号码（如 +60123456789），系统不会猜测收件人");
        long notification = insert("INSERT INTO notifications(title,body,related_type,related_id,status,read_at,recipient_user_id) VALUES(?,?,'signature_whatsapp',?,'read',NOW(),?)",
                "WhatsApp签署邀请", "签署邀请发送记录（不保存签署密钥）", id, actor);
        long delivery = insert("INSERT INTO notification_deliveries(notification_id,channel,destination,status,attempt_count) VALUES(?,'whatsapp',?,'sending',1)", notification,phone.substring(1));
        NewAttempt attempt = new NewAttempt();
        if (attempts.insertAttempt(delivery,"ccps_signature_invitation","zh_CN",attempt)!=1 || attempt.getId()==null)
            throw error(HttpStatus.CONFLICT,"无法创建发送记录");
        return new Prepared(delivery,attempt.getId(),phone.substring(1),List.of(
                text(row,"signer_name"), text(row,"original_name"),
                expiry(row.get("expires_at")),
                base+"/sign/"+token),null);
    }

    Map<String,Object> request(Long id, String token, boolean lock) {
        if (lock && (token == null || !token.matches("[A-Za-z0-9_-]{20,200}")))
            throw error(HttpStatus.BAD_REQUEST,"签署链接无效");
        var rows = db.queryForList("""
                SELECT sr.id,sr.entity_type,sr.entity_id,sr.signer_role,sr.signer_name,sr.expires_at,
                       d.original_name
                FROM electronic_signature_requests sr
                JOIN documents d ON d.id=sr.source_document_id
                JOIN documents root ON root.id=COALESCE(sr.root_document_id,sr.source_document_id)
                WHERE sr.id=? AND sr.status='pending' AND sr.expires_at>NOW()
                  AND d.status NOT IN ('voided','superseded') AND root.status NOT IN ('voided','superseded')
                """ + (lock ? " AND sr.access_token_hash=? FOR UPDATE" : ""),
                lock ? new Object[]{id,hash(token)} : new Object[]{id});
        if (rows.size()!=1) throw error(HttpStatus.CONFLICT,"签署任务或链接已失效，请刷新后重新发起");
        return rows.get(0);
    }

    String recipient(Map<String,Object> row) {
        String role=text(row,"signer_role"), entity=text(row,"entity_type");
        Object id=row.get("entity_id");
        List<Map<String,Object>> candidates=List.of();
        if (List.of("owner","second_owner").contains(role)) {
            candidates=db.queryForList("""
                SELECT DISTINCT o.id,o.full_name,COALESCE(NULLIF(TRIM(o.mobile_phone),''),o.phone) AS phone
                FROM owners o JOIN owner_units ou ON ou.owner_id=o.id
                WHERE o.status='active' AND ou.status='active' AND
                ((?='lease' AND EXISTS(SELECT 1 FROM leases l WHERE l.id=? AND l.unit_id=ou.unit_id)) OR
                 (?='rental_mandate' AND EXISTS(SELECT 1 FROM rental_mandates rm WHERE rm.id=? AND rm.owner_unit_id=ou.id)))
                """, entity,id,entity,id);
        } else if ("tenant".equals(role) && List.of("lease","rental_mandate").contains(entity)) {
            candidates=db.queryForList("""
                SELECT DISTINCT t.id,t.full_name,t.phone,ws.opted_out_at FROM leases l
                JOIN tenants t ON t.id=l.tenant_id LEFT JOIN tenant_whatsapp_subscriptions ws ON ws.tenant_id=t.id
                WHERE ((?='lease' AND l.id=?) OR (?='rental_mandate' AND l.rental_mandate_id=?))
                  AND t.status='active'
                """,entity,id,entity,id);
            if (candidates.stream().anyMatch(r->name(text(r,"full_name")).equals(name(text(row,"signer_name"))) && r.get("opted_out_at")!=null))
                throw error(HttpStatus.CONFLICT,"该租户已退订WhatsApp通知，请使用其他发送方式");
        }
        var matches=candidates.stream().filter(r->name(text(r,"full_name")).equals(name(text(row,"signer_name")))).toList();
        if (matches.size()!=1) return "";
        String raw=text(matches.get(0),"phone").trim();
        String canonical=strictPhone(raw);
        // Keep an existing local number for country selection; prepare() still requires E.164.
        return canonical.isEmpty()?raw:canonical;
    }

    State existing(Long id) {
        var rows=db.queryForList("""
            SELECT d.destination,d.status FROM notifications n JOIN notification_deliveries d ON d.notification_id=n.id
            WHERE n.related_type='signature_whatsapp' AND n.related_id=? AND d.channel='whatsapp' ORDER BY d.id DESC LIMIT 1
            """,id);
        return rows.isEmpty()?null:new State("+"+text(rows.get(0),"destination"),text(rows.get(0),"status"),client.configured());
    }
    private long insert(String sql,Object... values) {
        GeneratedKeyHolder key=new GeneratedKeyHolder();
        db.update(c->{var p=c.prepareStatement(sql,java.sql.Statement.RETURN_GENERATED_KEYS);
            for(int i=0;i<values.length;i++) p.setObject(i+1,values[i]); return p;},key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    static String strictPhone(String phone) {
        String value=Objects.toString(phone,"").trim().replaceAll("[ ()-]","");
        return value.matches("\\+[1-9][0-9]{7,14}")?value:"";
    }
    static String expiry(Object value) {
        if(value instanceof LocalDateTime date) return date.toLocalDate().toString();
        if(value instanceof java.sql.Timestamp date) return date.toLocalDateTime().toLocalDate().toString();
        throw new IllegalStateException("Unsupported signature expiry");
    }
    private static String name(String value) {return value.trim().replaceAll("\\s+"," ").toLowerCase(java.util.Locale.ROOT);}
    private static String text(Map<String,Object> row,String key){return Objects.toString(row.get(key),"");}
    private static String hash(String token) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
