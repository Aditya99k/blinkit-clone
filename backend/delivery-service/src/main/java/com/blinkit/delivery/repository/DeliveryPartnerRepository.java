package com.blinkit.delivery.repository;

import com.blinkit.delivery.entity.DeliveryPartner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeliveryPartnerRepository extends MongoRepository<DeliveryPartner, String> {

    Optional<DeliveryPartner> findByPartnerId(String partnerId);

    boolean existsByPartnerId(String partnerId);

    Page<DeliveryPartner> findAll(Pageable pageable);

}
