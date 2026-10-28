package com.glb.campulsebackend.repository;

import com.glb.campulsebackend.entity.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

	@Query("""
			select e from Event e, SavedEvent s
			where s.id.eventId = e.id and s.id.userId = :userId
			order by s.savedAt desc, e.id desc
			""")
	List<Event> findSavedBy(@Param("userId") Long userId, Pageable pageable);
}
