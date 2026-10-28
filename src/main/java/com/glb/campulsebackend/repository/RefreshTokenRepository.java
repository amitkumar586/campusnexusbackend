package com.glb.campulsebackend.repository;

import com.glb.campulsebackend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/** Atomically revokes a token; returns 0 if it was already revoked (single-use guarantee). */
	@Modifying
	@Query("update RefreshToken t set t.revoked = true where t.id = :id and t.revoked = false")
	int revokeIfActive(@Param("id") Long id);

	@Modifying
	@Query("update RefreshToken t set t.revoked = true where t.user.id = :userId and t.revoked = false")
	int revokeAllForUser(@Param("userId") Long userId);
}
