package com.yas.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.customer.model.UserAddress;
import com.yas.customer.repository.UserAddressRepository;
import com.yas.customer.viewmodel.address.ActiveAddressVm;
import com.yas.customer.viewmodel.address.AddressDetailVm;
import com.yas.customer.viewmodel.address.AddressPostVm;
import com.yas.customer.viewmodel.address.AddressVm;
import com.yas.customer.viewmodel.useraddress.UserAddressVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private UserAddressService userAddressService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserAddressList_whenAnonymous_shouldThrowAccessDeniedException() {
        mockCurrentUser("anonymousUser");

        assertThrows(AccessDeniedException.class, () -> userAddressService.getUserAddressList());
    }

    @Test
    void getUserAddressList_whenValidUser_shouldMapAndSortByActiveDesc() {
        mockCurrentUser("u1");

        UserAddress active = UserAddress.builder().id(1L).userId("u1").addressId(10L).isActive(true).build();
        UserAddress inactive = UserAddress.builder().id(2L).userId("u1").addressId(20L).isActive(false).build();
        when(userAddressRepository.findAllByUserId("u1")).thenReturn(List.of(inactive, active));
        when(locationService.getAddressesByIdList(List.of(20L, 10L))).thenReturn(List.of(
            AddressDetailVm.builder().id(10L).contactName("A").phone("1").addressLine1("L1").city("C")
                .zipCode("Z").districtId(1L).districtName("D").stateOrProvinceId(1L)
                .stateOrProvinceName("S").countryId(1L).countryName("VN").build(),
            AddressDetailVm.builder().id(20L).contactName("B").phone("2").addressLine1("L2").city("C")
                .zipCode("Z").districtId(1L).districtName("D").stateOrProvinceId(1L)
                .stateOrProvinceName("S").countryId(1L).countryName("VN").build()
        ));

        List<ActiveAddressVm> result = userAddressService.getUserAddressList();

        assertThat(result).hasSize(2);
        assertTrue(result.getFirst().isActive());
        assertFalse(result.getLast().isActive());
    }

    @Test
    void getAddressDefault_whenNoActiveAddress_shouldThrowNotFoundException() {
        mockCurrentUser("u1");
        when(userAddressRepository.findByUserIdAndIsActiveTrue("u1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userAddressService.getAddressDefault());
    }

    @Test
    void getAddressDefault_whenFound_shouldReturnAddressDetail() {
        mockCurrentUser("u1");
        when(userAddressRepository.findByUserIdAndIsActiveTrue("u1"))
            .thenReturn(Optional.of(UserAddress.builder().userId("u1").addressId(10L).isActive(true).build()));
        AddressDetailVm expected = AddressDetailVm.builder().id(10L).contactName("A").build();
        when(locationService.getAddressById(10L)).thenReturn(expected);

        AddressDetailVm result = userAddressService.getAddressDefault();

        assertEquals(10L, result.id());
    }

    @Test
    void createAddress_whenFirstAddress_shouldCreateActiveUserAddress() {
        mockCurrentUser("u1");
        AddressPostVm postVm = new AddressPostVm("A", "1", "L1", "C", "Z", 1L, 1L, 1L);
        when(userAddressRepository.findAllByUserId("u1")).thenReturn(List.of());
        when(locationService.createAddress(postVm)).thenReturn(AddressVm.builder().id(10L).contactName("A").build());
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress ua = invocation.getArgument(0);
            ua.setId(100L);
            return ua;
        });

        UserAddressVm result = userAddressService.createAddress(postVm);

        assertEquals("u1", result.userId());
        assertTrue(result.isActive());
        assertEquals(10L, result.addressGetVm().id());
    }

    @Test
    void createAddress_whenNotFirstAddress_shouldCreateInactiveUserAddress() {
        mockCurrentUser("u1");
        AddressPostVm postVm = new AddressPostVm("A", "1", "L1", "C", "Z", 1L, 1L, 1L);
        when(userAddressRepository.findAllByUserId("u1"))
            .thenReturn(List.of(UserAddress.builder().id(1L).userId("u1").addressId(9L).isActive(true).build()));
        when(locationService.createAddress(postVm)).thenReturn(AddressVm.builder().id(10L).contactName("A").build());
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAddressVm result = userAddressService.createAddress(postVm);

        assertFalse(result.isActive());
    }

    @Test
    void deleteAddress_whenAddressNotFound_shouldThrowNotFoundException() {
        mockCurrentUser("u1");
        when(userAddressRepository.findOneByUserIdAndAddressId("u1", 10L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> userAddressService.deleteAddress(10L));
    }

    @Test
    void deleteAddress_whenAddressFound_shouldDelete() {
        mockCurrentUser("u1");
        UserAddress userAddress = UserAddress.builder().id(1L).userId("u1").addressId(10L).isActive(false).build();
        when(userAddressRepository.findOneByUserIdAndAddressId("u1", 10L)).thenReturn(userAddress);

        userAddressService.deleteAddress(10L);

        verify(userAddressRepository).delete(userAddress);
    }

    @Test
    void chooseDefaultAddress_shouldToggleActiveFlagAndSaveAll() {
        mockCurrentUser("u1");
        UserAddress a1 = UserAddress.builder().id(1L).userId("u1").addressId(10L).isActive(true).build();
        UserAddress a2 = UserAddress.builder().id(2L).userId("u1").addressId(20L).isActive(false).build();
        when(userAddressRepository.findAllByUserId("u1")).thenReturn(List.of(a1, a2));

        userAddressService.chooseDefaultAddress(20L);

        assertFalse(a1.getIsActive());
        assertTrue(a2.getIsActive());
        verify(userAddressRepository).saveAll(List.of(a1, a2));
    }

    private void mockCurrentUser(String userName) {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        when(authentication.getName()).thenReturn(userName);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}