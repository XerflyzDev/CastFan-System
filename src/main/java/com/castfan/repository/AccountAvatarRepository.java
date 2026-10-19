package com.castfan.repository;

import java.util.Optional;

import com.castfan.entity.AccountAvatar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountAvatarRepository extends JpaRepository<AccountAvatar, Long> {

	Optional<AccountAvatar> findByUserAccountEmailIgnoreCase(String email);

	boolean existsByUserAccountEmailIgnoreCase(String email);

	void deleteByUserAccountEmailIgnoreCase(String email);
}
