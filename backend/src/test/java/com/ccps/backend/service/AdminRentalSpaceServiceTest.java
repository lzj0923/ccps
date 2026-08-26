package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminRentalModeRequest;
import com.ccps.backend.dto.AdminRentalSpaceRequest;
import com.ccps.backend.dto.AdminRentalSpaceResponse;
import com.ccps.backend.mapper.AdminRentalSpaceMapper;
import com.ccps.backend.mapper.AdminRentalSpaceMapper.Room;

@ExtendWith(MockitoExtension.class)
class AdminRentalSpaceServiceTest {
    @Mock AdminRentalSpaceMapper mapper;
    AdminRentalSpaceService service;
    @BeforeEach void setUp(){service=new AdminRentalSpaceService(mapper);}

    @Test void repairsMissingWholeUnitOptionWhenRentalSpacesAreLoaded(){
        AdminRentalSpaceResponse whole = new AdminRentalSpaceResponse(
                70L, 7L, "WHOLE", "整套房产", "whole_unit", 1,
                new BigDecimal("88.5"), null, "active", null, null, null, null, null);
        when(mapper.countUnit(7L)).thenReturn(1);
        when(mapper.findByUnit(7L)).thenReturn(List.of(whole));

        List<AdminRentalSpaceResponse> result = service.find(7L);

        verify(mapper).ensureWholeUnitSpace(7L);
        assertThat(result).extracting(AdminRentalSpaceResponse::spaceType).containsExactly("whole_unit");
    }

    @Test void addingRoomTurnsUnitIntoSharedMode(){
        when(mapper.countUnit(7L)).thenReturn(1);
        when(mapper.countOpenLeasesByType(7L,"whole_unit")).thenReturn(0);
        when(mapper.countCode(7L,"ROOM-A",null)).thenReturn(0);
        when(mapper.insert(any(Room.class))).thenAnswer(invocation->{Room room=invocation.getArgument(0);room.setId(70L);return 1;});
        Long id=service.create(7L,new AdminRentalSpaceRequest("room-a","A 房",1,
                new BigDecimal("18.5"),new BigDecimal("1200"),"active"));
        assertThat(id).isEqualTo(70L);
        verify(mapper).updateMode(7L,"shared");
    }

    @Test void cannotReturnToWholeUnitWhileRoomLeaseIsOpen(){
        when(mapper.countUnit(7L)).thenReturn(1);
        when(mapper.countOpenLeasesByType(7L,"room")).thenReturn(1);
        assertThatThrownBy(()->service.changeMode(7L,new AdminRentalModeRequest("whole_unit")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("仍有房间租约");
    }
}
