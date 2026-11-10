package com.jayant.backend.repository;

import com.jayant.backend.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowIdOrderByIdAsc(Long showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.show.id = :showId and s.id in :ids order by s.id")
    List<Seat> lockSeats(@Param("showId") Long showId, @Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.booking.id = :bookingId order by s.id")
    List<Seat> lockByBooking(@Param("bookingId") Long bookingId);
}