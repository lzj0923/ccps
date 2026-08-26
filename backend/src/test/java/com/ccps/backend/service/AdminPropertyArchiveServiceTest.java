package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.AdminPropertyArchiveMapper;
import com.ccps.backend.mapper.AdminPropertyArchiveMapper.UnitState;

@ExtendWith(MockitoExtension.class)
class AdminPropertyArchiveServiceTest {
    @Mock private AdminPropertyArchiveMapper mapper;
    @Mock private AdminAuditService auditService;
    private AdminPropertyArchiveService service;
    @BeforeEach void setUp(){service=new AdminPropertyArchiveService(mapper,auditService);}

    @Test void offMarketPreservesRentalStatusAndWritesHistory(){
        UnitState state=state("listed");when(mapper.lockUnit(8L)).thenReturn(state);when(mapper.countActiveRentalServices(8L)).thenReturn(1);
        when(mapper.markOffMarket(8L,"OWNER_REQUEST","业主暂缓出租",9L)).thenReturn(1);
        service.offMarket(8L,"OWNER_REQUEST"," 业主暂缓出租 ",9L);
        verify(mapper).markOffMarket(8L,"OWNER_REQUEST","业主暂缓出租",9L);
        verify(mapper).insertHistory(8L,"off_market","OWNER_REQUEST","业主暂缓出租",9L);
    }

    @Test void missingRentalServiceBlocksOffMarket(){
        UnitState state=state("listed");when(mapper.lockUnit(8L)).thenReturn(state);when(mapper.countActiveRentalServices(8L)).thenReturn(0);
        ResponseStatusException error=assertThrows(ResponseStatusException.class,()->service.offMarket(8L,"OWNER_REQUEST",null,9L));
        assertEquals(409,error.getStatusCode().value());
        verify(mapper,never()).markOffMarket(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
    }

    @Test void relistWritesHistory(){
        UnitState state=state("off_market");when(mapper.lockUnit(8L)).thenReturn(state);when(mapper.countRelistableOwnerships(8L)).thenReturn(1);when(mapper.countActiveRentalServices(8L)).thenReturn(1);when(mapper.countActiveLeases(8L)).thenReturn(0);when(mapper.relist(8L)).thenReturn(1);
        service.relist(8L,"恢复招租",9L);
        verify(mapper).relist(8L);verify(mapper).insertHistory(8L,"relisted",null,"恢复招租",9L);
    }

    @Test void occupiedRentalListingCannotBeRelisted(){
        UnitState state=state("off_market");when(mapper.lockUnit(8L)).thenReturn(state);when(mapper.countRelistableOwnerships(8L)).thenReturn(1);when(mapper.countActiveRentalServices(8L)).thenReturn(1);when(mapper.countActiveLeases(8L)).thenReturn(1);
        ResponseStatusException error=assertThrows(ResponseStatusException.class,()->service.relist(8L,null,9L));
        assertEquals(409,error.getStatusCode().value());
        verify(mapper,never()).relist(8L);
    }

    private UnitState state(String listing){UnitState value=mock(UnitState.class);when(value.getRentalListingStatus()).thenReturn(listing);return value;}
}
