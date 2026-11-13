package com.castfan.repository;

import java.util.Optional;

import com.castfan.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

	Optional<UserAccount> findByEmailIgnoreCase(String email);

	Optional<UserAccount> findByLoginIdentifierIgnoreCase(String loginIdentifier);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByLoginIdentifierIgnoreCase(String loginIdentifier);
}
