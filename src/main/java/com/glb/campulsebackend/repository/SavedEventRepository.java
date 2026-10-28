package com.glb.campulsebackend.repository;

import com.glb.campulsebackend.entity.SavedEvent;
import com.glb.campulsebackend.entity.SavedEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface SavedEventRepository extends JpaRepository<SavedEvent, SavedEventId> {

	/** Which of the given events has this user saved? Callers must not pass an empty collection. */
	@Query("select s.id.eventId from SavedEvent s where s.id.userId = :userId and s.id.eventId in :eventIds")
	Set<Long> findSavedEventIds(@Param("userId") Long userId, @Param("eventIds") Collection<Long> eventIds);
}
