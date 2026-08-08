package com.ccps.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.ccps.backend.mapper.PropertyExpensePostingMapper;
import com.ccps.backend.mapper.PropertyExpensePostingMapper.FinanceWrite;
import com.ccps.backend.mapper.PropertyExpensePostingMapper.MandateFeeRow;
import com.ccps.backend.mapper.PropertyExpensePostingMapper.PostingRow;
import com.ccps.backend.mapper.PropertyExpensePostingMapper.ProfileRow;
import com.ccps.backend.mapper.PropertyExpensePostingMapper.PropertyContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PropertyExpensePostingService {
    private final PropertyExpensePostingMapper mapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public PropertyExpensePostingService(PropertyExpensePostingMapper mapper,ObjectMapper objectMapper) {
        this(mapper,objectMapper,Clock.systemDefaultZone());
    }

    PropertyExpensePostingService(PropertyExpensePostingMapper mapper,ObjectMapper objectMapper,Clock clock) {
        this.mapper=mapper; this.objectMapper=objectMapper; this.clock=clock;
    }

    @Transactional
    public void syncCurrent(Long ownerUnitId,Map<String,Object> profile,Long actorId) {
        LocalDate today=LocalDate.now(clock);
        for (Charge charge : charges(profile,today)) post(ownerUnitId,charge,actorId,today);
    }

    @Transactional
    public void syncMandateFee(Long ownerUnitId,Long mandateId,BigDecimal amount,Long actorId,LocalDate occurredOn) {
        if (ownerUnitId==null||mandateId==null||amount==null||occurredOn==null) return;
        post(ownerUnitId,new Charge("rental-mandate-"+mandateId,"代管服务费",money(amount),"service_fee",
                YearMonth.from(occurredOn).toString()),actorId,occurredOn);
    }

    @Scheduled(cron="${ccps.property-expenses.cron:0 20 0 * * *}")
    @Transactional
    public void postDueExpenses() {
        postAllDueExpenses();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void postDueExpensesOnStartup() {
        postAllDueExpenses();
    }

    private void postAllDueExpenses() {
        LocalDate today=LocalDate.now(clock);
        for (ProfileRow row : mapper.findProfiles()) {
            try {
                Map<String,Object> profile=objectMapper.readValue(row.getProfileJson(),new TypeReference<>(){});
                syncCurrent(row.getOwnerUnitId(),profile,null);
            } catch (Exception ignored) {
                // One malformed legacy profile must not stop the remaining properties.
            }
        }
        List<MandateFeeRow> mandateFees=mapper.findActiveMandateFees(today);
        if (mandateFees!=null) for (MandateFeeRow row : mandateFees) {
            syncMandateFee(row.getOwnerUnitId(),row.getMandateId(),row.getManagementFee(),null,today);
        }
    }

    private void post(Long ownerUnitId,Charge charge,Long actorId,LocalDate occurredOn) {
        if (charge.amount().signum()<=0) return;
        PostingRow existing=mapper.lockPosting(ownerUnitId,charge.key(),charge.period());
        String description=charge.name()+"（帳期 "+charge.period()+"）";
        if (existing!=null) {
            if ("pending".equals(existing.getConfirmationStatus())) {
                mapper.updatePendingFinance(existing.getFinanceRecordId(),charge.amount(),occurredOn);
                mapper.updateCashflow(existing.getFinanceRecordId(),charge.category(),description,occurredOn);
            }
            return;
        }
        PropertyContext context=mapper.findContext(ownerUnitId);
        if (context==null) return;
        FinanceWrite finance=new FinanceWrite();
        finance.setTransactionNo("AUTO-EXP-"+occurredOn.toString().replace("-","")+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase());
        finance.setUnitId(context.getUnitId()); finance.setOwnerId(context.getOwnerId()); finance.setActorId(actorId);
        finance.setAmount(charge.amount()); finance.setOccurredOn(occurredOn);
        mapper.insertFinance(finance);
        mapper.insertCashflow(finance.getId(),context.getUnitId(),context.getOwnerId(),charge.category(),description,occurredOn);
        mapper.insertPosting(ownerUnitId,charge.key(),charge.name(),charge.period(),finance.getId());
    }

    private List<Charge> charges(Map<String,Object> profile,LocalDate today) {
        List<Charge> result=new ArrayList<>();
        List<Integer> allMonths=allMonths();
        if (enabled(profile.get("rentalServiceEnabled"))) {
            addConfigured(result,"management-service","租管服務費",money(profile.get("managementFeeAmount")),"management",
                    profile.getOrDefault("managementFeeBillingMode","months"),profile.get("managementFeeBillingMonths"),allMonths,today);
        }
        Object legacyManagementMonths=profile.containsKey("managementFeeBillingMonths")
                ? profile.get("managementFeeBillingMonths") : profile.get("managementFeeMonths");
        addConfigured(result,"building-management","大樓管理費",money(profile.get("buildingManagementFee")),"management",
                profile.getOrDefault("buildingManagementBillingMode","months"),legacyManagementMonths,allMonths,today);
        addConfigured(result,"sales-service","銷售服務費",money(profile.get("salesServiceFee")),"service_fee",
                profile.get("salesServiceBillingMode"),profile.get("salesServiceBillingMonths"),List.of(),today);
        addConfigured(result,"general-service","一般服務費",money(profile.get("generalServiceFee")),"service_fee",
                profile.get("generalServiceBillingMode"),profile.get("generalServiceBillingMonths"),List.of(),today);
        Object services=profile.get("rentalServiceFees");
        if (services instanceof List<?> list) for (Object value:list) if (value instanceof Map<?,?> item) {
            String id=text(item.get("id")); String name=text(item.get("name"));
            if (id==null) id="legacy-"+Integer.toHexString((name+money(item.get("amount"))).hashCode());
            addConfigured(result,"rental-service-"+id,name==null?"租管服務":name,money(item.get("amount")),"service_fee",
                    item.get("billingMode"),item.get("billingMonths"),List.of(),today);
        }
        addConfigured(result,"fire-insurance","火險",money(profile.get("fireInsuranceFee")),"insurance",
                profile.getOrDefault("fireInsuranceBillingMode","months"),profile.get("fireInsuranceBillingMonths"),
                List.of(month(profile.get("fireInsuranceMonth"),today)),today);
        addConfigured(result,"land-tax","地稅",money(profile.get("landTaxFee")),"tax",
                profile.getOrDefault("landTaxBillingMode","months"),profile.get("landTaxBillingMonths"),
                List.of(month(profile.get("landTaxMonth"),today)),today);
        addConfigured(result,"assessment-tax","門牌稅",money(profile.get("assessmentTaxFee")),"tax",
                profile.getOrDefault("assessmentTaxBillingMode","months"),profile.get("assessmentTaxBillingMonths"),
                List.of(month(profile.get("assessmentTaxMonth"),today)),today);
        return result;
    }

    private void addConfigured(List<Charge> target,String key,String name,BigDecimal amount,String category,
            Object modeValue,Object monthsValue,List<Integer> legacyMonths,LocalDate today) {
        String mode=text(modeValue);
        boolean recurring="months".equalsIgnoreCase(mode)||"recurring".equalsIgnoreCase(mode);
        List<Integer> billingMonths=months(monthsValue);
        if (billingMonths.isEmpty()) billingMonths=new ArrayList<>(legacyMonths);
        if (recurring && billingMonths.isEmpty()) billingMonths=allMonths();
        if (!billingMonths.isEmpty()&&!billingMonths.contains(today.getMonthValue())) return;
        add(target,key,name,amount,category,recurring?YearMonth.from(today).toString():"ONCE");
    }

    private List<Integer> allMonths(){return java.util.stream.IntStream.rangeClosed(1,12).boxed().toList();}

    private void add(List<Charge> target,String key,String name,BigDecimal amount,String category,String period){if(amount.signum()>0)target.add(new Charge(key,name,amount,category,period));}
    private BigDecimal money(Object value){try{return new BigDecimal(String.valueOf(value==null?0:value)).setScale(2,RoundingMode.HALF_UP);}catch(Exception e){return BigDecimal.ZERO;}}
    private int month(Object value,LocalDate today){try{return Math.max(1,Math.min(12,Integer.parseInt(String.valueOf(value))));}catch(Exception e){return today.getMonthValue();}}
    private List<Integer> months(Object value){List<Integer> result=new ArrayList<>();if(value instanceof List<?> list)for(Object item:list)try{result.add(Integer.parseInt(String.valueOf(item)));}catch(Exception ignored){}return result;}
    private boolean enabled(Object value){return value instanceof Boolean b?b:Boolean.parseBoolean(String.valueOf(value));}
    private String text(Object value){String s=value==null?null:String.valueOf(value).trim();return s==null||s.isBlank()?null:s;}
    private record Charge(String key,String name,BigDecimal amount,String category,String period){}
}
