package com.ccps.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.ccps.backend.dto.AdminPropertyHandoverChecklistItemRequest;
import com.ccps.backend.dto.AdminPropertyHandoverChecklistItemResponse;
import com.ccps.backend.mapper.AdminPropertyHandoverChecklistMapper;
import com.ccps.backend.mapper.AdminPropertyHandoverChecklistMapper.Row;

@Service
public class AdminPropertyHandoverChecklistService {
    private final AdminPropertyHandoverChecklistMapper mapper;
    private static final List<DefaultItem> STANDARD_ITEMS = List.of(
        items("鑰匙 Keys", "鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)", "大門鑰匙 / Main Door Entrance Key", "主臥室鑰匙 / Master Bedroom Door Key", "臥室2鑰匙 / Bedroom 2 Door Key", "臥室3鑰匙 / Bedroom 3 Door Key", "臥室4鑰匙 / Bedroom 4 Door Key", "書房鑰匙 / Study Room Door Key", "儲存室鑰匙 / Store Room Key", "陽台鑰匙 / Balcony Key", "廚房鑰匙 / Kitchen Door Key", "後門鑰匙 / Yard Key", "信箱鑰匙 / Mailbox Key", "傭人房鑰匙 / Maid Room Door Key"),
        items("門禁卡 Access Card", "大門通行卡 / Main Door Access Card", "停車場通行卡 / Parking Access Card", "電梯通行卡 / Lift Access Card", "電梯與停車場通行卡 / Lift with Parking Access Card", "設施通行卡 / Facility Access Card"),
        items("遙控器 Remote Control", "空調遙控 / Air Con Remote", "電視遙控 / TV Remote", "風扇遙控 / Fan Remote", "電燈遙控 / Light Remote"),
        items("客廳 Living Room", "窗簾/卷簾 / Curtain/Roller Blind", "空調 / Air Cond", "風扇 / Ceiling Fan", "沙發 / Sofa", "咖啡桌 / Coffee Table", "地毯 / Rug", "電視 / TV", "電視櫃 / TV Cabinet", "站立式燈 / Stand Lamp", "擺架 / Shelf", "畫 / Wall Painting", "時鐘 / Clock", "沙發床/床褥 / Sofa Bed/Mattress", "書架 / Bookshelf", "鞋架 / Shoes Rack", "吊燈 / Pendant Lamp", "凳子 / Stool"),
        items("飯廳 Dining Room", "吊燈 / Pendant Lamp", "餐桌 / Dining Table", "餐椅 / Dining Chair", "畫 / Wall Painting", "電視 / TV", "電視櫃 / TV Cabinet", "鞋架 / Shoes Rack"),
        items("廚房 Kitchen", "櫥櫃 / Kitchen Cabinet", "洗衣機 / Washer Machine", "烘乾機 / Dryer Machine", "2合1洗衣烘乾機 / 2in1 Washer Dryer Machine", "冰箱 / Refrigerator", "微波爐 / Microwave", "烤箱 / Oven", "電子爐 / Electric Stove", "煤氣爐 / Gas Stove", "抽油煙機 / Gas Exhaustion", "洗碗機 / Dishwasher", "熱水壺 / Kettle", "濾水器 / Water Filter", "窗簾/卷簾 / Curtain/Roller Blind"),
        items("主臥室 Master Bedroom", "空調 / Air Cond", "窗簾/卷簾 / Curtain/Roller Blind", "風扇 / Fan", "衣櫃 / Wardrobe", "床頭櫃 / Bedside Table", "床架 / Bedframe", "床褥 / Mattress", "梳妝台 / Dressing Table", "梳妝台椅子 / Dressing Chair", "畫 / Wall Painting", "地毯 / Rug", "桌燈 / Table Lamp", "衣架 / Hanger", "書桌 / Study Table", "書椅 / Study Chair", "吊燈 / Pendant Lamp", "擺架 / Shelf"),
        items("主浴室 Master Bathroom", "浴缸 / Bathtub", "鏡子 / Mirror", "花灑 / Shower Hose", "洗手盆 / Basin", "馬桶 / Toilet Bowl", "浴室噴槍 / Bidet Spray", "衛生紙架 / Toilet Paper Holder", "窗簾/卷簾 / Curtain/Roller Blind", "熱水器 / Water Heater")
    );
    private static final Set<String> STANDARD_CATEGORIES = STANDARD_ITEMS.stream().map(DefaultItem::category).collect(java.util.stream.Collectors.toUnmodifiableSet());
    public AdminPropertyHandoverChecklistService(AdminPropertyHandoverChecklistMapper mapper){this.mapper=mapper;}

    @Transactional
    public List<AdminPropertyHandoverChecklistItemResponse> list(Long ownerId,Long ownerUnitId){
        requireProperty(ownerId,ownerUnitId);
        List<Row> rows = mapper.list(ownerUnitId);
        if (rows.isEmpty()) rows = seedStandardItems(ownerUnitId);
        return rows.stream().map(this::response).toList();
    }

    @Transactional
    public AdminPropertyHandoverChecklistItemResponse create(Long ownerId,Long ownerUnitId,AdminPropertyHandoverChecklistItemRequest request){requireProperty(ownerId,ownerUnitId);Row row=from(ownerUnitId,request);if(mapper.insert(row)!=1)throw conflict("Unable to create checklist item");return response(row);}

    @Transactional
    public AdminPropertyHandoverChecklistItemResponse update(Long ownerId,Long ownerUnitId,Long id,AdminPropertyHandoverChecklistItemRequest request){requireProperty(ownerId,ownerUnitId);Row row=mapper.find(ownerUnitId,id);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Checklist item not found");Row next=from(ownerUnitId,request);next.setId(id);if(mapper.update(next)!=1)throw conflict("Checklist item was changed by another request");return response(mapper.find(ownerUnitId,id));}

    @Transactional
    public void delete(Long ownerId,Long ownerUnitId,Long id){requireProperty(ownerId,ownerUnitId);if(mapper.delete(ownerUnitId,id)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Checklist item not found");}

    @Transactional(readOnly=true)
    public List<Row> rowsForOwnerUnit(Long ownerUnitId){return mapper.list(ownerUnitId).stream().filter(Row::isEnabled).toList();}

    @Transactional
    public List<Row> rowsForLease(Long leaseId){
        if (leaseId == null) return List.of();
        AdminPropertyHandoverChecklistMapper.LeaseOwnerUnitRow ownerUnit = mapper.findOwnerUnitForLease(leaseId);
        if (ownerUnit == null || ownerUnit.getOwnerUnitId() == null) return List.of();
        List<Row> rows = mapper.list(ownerUnit.getOwnerUnitId());
        if (rows.isEmpty()) rows = seedStandardItems(ownerUnit.getOwnerUnitId());
        Map<String, Row> unique = new LinkedHashMap<>();
        for (Row row : rows) {
            if (!row.isEnabled() || row.getCategory() == null || row.getItemName() == null) continue;
            unique.putIfAbsent(row.getCategory().trim() + "\u0000" + row.getItemName().trim(), row);
        }
        return new ArrayList<>(unique.values());
    }

    @Transactional
    public void syncCompletedReport(Long ownerUnitId,List<SyncItem> items){
        List<Row> existing=mapper.list(ownerUnitId); java.util.Set<Long> retained=new java.util.HashSet<>(); int order=0;
        for(SyncItem item:items){if(item==null||item.itemName()==null||item.itemName().isBlank())continue; String category=blank(item.category(),""); if(!STANDARD_CATEGORIES.contains(category))continue; Row match=existing.stream().filter(row->row.getCategory().equals(category)&&row.getItemName().equals(item.itemName().trim())).findFirst().orElse(null); if(match==null){Row row=new Row();row.setOwnerUnitId(ownerUnitId);row.setCategory(category);row.setItemName(item.itemName().trim());row.setDefaultQuantity(blank(item.quantity(),null));row.setSortOrder(order++);row.setEnabled(true);mapper.insert(row);}else{match.setDefaultQuantity(blank(item.quantity(),null));match.setSortOrder(order++);match.setEnabled(true);mapper.update(match);retained.add(match.getId());}}
        for(Row row:existing)if(!retained.contains(row.getId())&&!items.stream().anyMatch(item->item!=null&&item.itemName()!=null&&item.itemName().trim().equals(row.getItemName())&&blank(item.category(),"其他").equals(row.getCategory()))){row.setEnabled(false);mapper.update(row);}
    }

    private Row from(Long ownerUnitId,AdminPropertyHandoverChecklistItemRequest request){String category=request.category().trim();if(!STANDARD_CATEGORIES.contains(category))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Category must match the handover template");Row row=new Row();row.setOwnerUnitId(ownerUnitId);row.setCategory(category);row.setItemName(request.itemName().trim());row.setDefaultQuantity(blank(request.defaultQuantity(),null));row.setNotes(blank(request.notes(),null));row.setSortOrder(request.sortOrder()==null?0:Math.max(0,request.sortOrder()));row.setEnabled(request.enabled()==null||request.enabled());return row;}
    private AdminPropertyHandoverChecklistItemResponse response(Row row){return new AdminPropertyHandoverChecklistItemResponse(row.getId(),row.getOwnerUnitId(),row.getCategory(),row.getItemName(),row.getDefaultQuantity(),row.getNotes(),row.getSortOrder(),row.isEnabled(),row.getCreatedAt(),row.getUpdatedAt());}
    private void requireProperty(Long ownerId,Long ownerUnitId){if(mapper.ownsProperty(ownerId,ownerUnitId)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found");}
    private String blank(String value,String fallback){return value==null||value.isBlank()?fallback:value.trim();}
    private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private List<Row> seedStandardItems(Long ownerUnitId){
        List<Row> seeded = new ArrayList<>(); int order = 0;
        for (DefaultItem group : STANDARD_ITEMS) for (String name : group.items()) {
            Row row = new Row(); row.setOwnerUnitId(ownerUnitId); row.setCategory(group.category()); row.setItemName(name); row.setSortOrder(order++); row.setEnabled(true); row.setDefaultQuantity(null);
            if (mapper.insert(row) != 1) throw conflict("Unable to initialize handover checklist");
            seeded.add(row);
        }
        return seeded;
    }
    private static DefaultItem items(String category,String... names){return new DefaultItem(category,List.of(names));}
    private record DefaultItem(String category,List<String> items){}
    public record SyncItem(String category,String itemName,String quantity){}
}
