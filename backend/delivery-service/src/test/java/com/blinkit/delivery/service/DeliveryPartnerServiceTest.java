package com.blinkit.delivery.service;

import com.blinkit.delivery.dto.request.RegisterPartnerRequest;
import com.blinkit.delivery.dto.request.UpdateLocationRequest;
import com.blinkit.delivery.dto.response.DeliveryPartnerResponse;
import com.blinkit.delivery.entity.DeliveryPartner;
import com.blinkit.delivery.repository.DeliveryPartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryPartnerServiceTest {

    @Mock DeliveryPartnerRepository partnerRepository;
    @InjectMocks DeliveryPartnerService partnerService;

    private static final String PARTNER_ID = "partner-uuid-001";
    private static final String EMAIL = "agent@blinkit.com";

    private DeliveryPartner partner;
    private RegisterPartnerRequest registerReq;

    @BeforeEach
    void setUp() {
        partner = DeliveryPartner.builder()
                .partnerId(PARTNER_ID)
                .email(EMAIL)
                .name("Ravi Kumar")
                .phone("9876543210")
                .vehicleType("MOTORCYCLE")
                .vehicleNumber("KA01AB1234")
                .isAvailable(true)
                .isActive(true)
                .avgRating(5.0)
                .totalDeliveries(0)
                .build();

        registerReq = new RegisterPartnerRequest();
        registerReq.setName("Ravi Kumar");
        registerReq.setPhone("9876543210");
        registerReq.setVehicleType("MOTORCYCLE");
        registerReq.setVehicleNumber("KA01AB1234");
    }

    // ── register ──────────────────────────────────────────────────

    @Test
    @DisplayName("register — success creates and returns partner profile")
    void register_success() {
        when(partnerRepository.existsByPartnerId(PARTNER_ID)).thenReturn(false);
        when(partnerRepository.save(any(DeliveryPartner.class))).thenReturn(partner);

        DeliveryPartnerResponse result = partnerService.register(PARTNER_ID, EMAIL, registerReq);

        assertThat(result.getPartnerId()).isEqualTo(PARTNER_ID);
        assertThat(result.getName()).isEqualTo("Ravi Kumar");
        assertThat(result.getVehicleType()).isEqualTo("MOTORCYCLE");
        assertThat(result.getIsAvailable()).isTrue();
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getAvgRating()).isEqualTo(5.0);
        verify(partnerRepository).save(any(DeliveryPartner.class));
    }

    @Test
    @DisplayName("register — throws 409 when profile already exists")
    void register_conflict() {
        when(partnerRepository.existsByPartnerId(PARTNER_ID)).thenReturn(true);

        assertThatThrownBy(() -> partnerService.register(PARTNER_ID, EMAIL, registerReq))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(partnerRepository, never()).save(any());
    }

    // ── getMyProfile ──────────────────────────────────────────────

    @Test
    @DisplayName("getMyProfile — returns profile when found")
    void getMyProfile_found() {
        when(partnerRepository.findByPartnerId(PARTNER_ID)).thenReturn(Optional.of(partner));

        DeliveryPartnerResponse result = partnerService.getMyProfile(PARTNER_ID);

        assertThat(result.getPartnerId()).isEqualTo(PARTNER_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("getMyProfile — throws 404 when partner not found")
    void getMyProfile_notFound() {
        when(partnerRepository.findByPartnerId(PARTNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> partnerService.getMyProfile(PARTNER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── updateProfile ─────────────────────────────────────────────

    @Test
    @DisplayName("updateProfile — updates fields and saves")
    void updateProfile_success() {
        RegisterPartnerRequest updateReq = new RegisterPartnerRequest();
        updateReq.setName("Ravi Updated");
        updateReq.setPhone("9123456789");
        updateReq.setVehicleType("SCOOTER");
        updateReq.setVehicleNumber("KA01CD5678");

        when(partnerRepository.findByPartnerId(PARTNER_ID)).thenReturn(Optional.of(partner));
        when(partnerRepository.save(any(DeliveryPartner.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryPartnerResponse result = partnerService.updateProfile(PARTNER_ID, updateReq);

        assertThat(result.getName()).isEqualTo("Ravi Updated");
        assertThat(result.getVehicleType()).isEqualTo("SCOOTER");
        verify(partnerRepository).save(partner);
    }

    // ── toggleAvailability ────────────────────────────────────────

    @Test
    @DisplayName("toggleAvailability — flips available from true to false")
    void toggleAvailability_trueToFalse() {
        partner.setIsAvailable(true);
        when(partnerRepository.findByPartnerId(PARTNER_ID)).thenReturn(Optional.of(partner));
        when(partnerRepository.save(any(DeliveryPartner.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryPartnerResponse result = partnerService.toggleAvailability(PARTNER_ID);

        assertThat(result.getIsAvailable()).isFalse();
    }

    @Test
    @DisplayName("toggleAvailability — flips available from false to true")
    void toggleAvailability_falseToTrue() {
        partner.setIsAvailable(false);
        when(partnerRepository.findByPartnerId(PARTNER_ID)).thenReturn(Optional.of(partner));
        when(partnerRepository.save(any(DeliveryPartner.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryPartnerResponse result = partnerService.toggleAvailability(PARTNER_ID);

        assertThat(result.getIsAvailable()).isTrue();
    }

    // ── updateLocation ────────────────────────────────────────────

    @Test
    @DisplayName("updateLocation — sets lat/lng and saves")
    void updateLocation_success() {
        UpdateLocationRequest locReq = new UpdateLocationRequest();
        locReq.setLat(12.9352);
        locReq.setLng(77.6245);

        when(partnerRepository.findByPartnerId(PARTNER_ID)).thenReturn(Optional.of(partner));
        when(partnerRepository.save(any(DeliveryPartner.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryPartnerResponse result = partnerService.updateLocation(PARTNER_ID, locReq);

        assertThat(result.getCurrentLat()).isEqualTo(12.9352);
        assertThat(result.getCurrentLng()).isEqualTo(77.6245);
        assertThat(result.getLastLocationUpdatedAt()).isNotNull();
    }

    // ── togglePartnerActive (admin) ───────────────────────────────

    @Test
    @DisplayName("togglePartnerActive — flips isActive from true to false")
    void togglePartnerActive_deactivate() {
        partner.setIsActive(true);
        when(partnerRepository.findByPartnerId(PARTNER_ID)).thenReturn(Optional.of(partner));
        when(partnerRepository.save(any(DeliveryPartner.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryPartnerResponse result = partnerService.togglePartnerActive(PARTNER_ID);

        assertThat(result.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("togglePartnerActive — flips isActive from false to true")
    void togglePartnerActive_activate() {
        partner.setIsActive(false);
        when(partnerRepository.findByPartnerId(PARTNER_ID)).thenReturn(Optional.of(partner));
        when(partnerRepository.save(any(DeliveryPartner.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryPartnerResponse result = partnerService.togglePartnerActive(PARTNER_ID);

        assertThat(result.getIsActive()).isTrue();
    }

    // ── getAllPartners (admin) ─────────────────────────────────────

    @Test
    @DisplayName("getAllPartners — returns paginated list")
    void getAllPartners_paged() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<DeliveryPartner> page = new PageImpl<>(List.of(partner), pageable, 1);
        when(partnerRepository.findAll(pageable)).thenReturn(page);

        Page<DeliveryPartnerResponse> result = partnerService.getAllPartners(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPartnerId()).isEqualTo(PARTNER_ID);
    }
}
