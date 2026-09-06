package com.example.delivery.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    @Query(
            "select d from Delivery d where (:zone is null or d.zone = :zone) and (:status is null"
                + " or d.status = :status) order by d.createdAt desc, d.id desc")
    List<Delivery> findFiltered(@Param("zone") Zone zone, @Param("status") DeliveryStatus status);
}
