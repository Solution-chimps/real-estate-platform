package com.br.real_estate_platform.repository;

import com.br.real_estate_platform.entity.AppUser;
import com.br.real_estate_platform.entity.UserSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

	@EntityGraph(attributePaths = "user")
	Optional<UserSession> findByTokenHash(String tokenHash);

	List<UserSession> findByUserAndRevokedAtIsNullAndExpiresAtAfterOrderByLastSeenAtAsc(AppUser user, Instant now);

	@Modifying
	@Query("update UserSession s set s.revokedAt = :now where s.user = :user and s.revokedAt is null")
	int revokeAllForUser(@Param("user") AppUser user, @Param("now") Instant now);

	@Modifying
	@Query("delete from UserSession s where s.expiresAt < :threshold or s.revokedAt < :threshold")
	int deleteStale(@Param("threshold") Instant threshold);
}
