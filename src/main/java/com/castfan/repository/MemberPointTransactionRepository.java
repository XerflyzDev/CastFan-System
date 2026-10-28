package com.castfan.repository;

import java.util.List;
import java.util.Optional;

import com.castfan.entity.MemberPointTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberPointTransactionRepository extends JpaRepository<MemberPointTransaction, Long> {

	boolean existsByReceiptReferenceKey(String receiptReferenceKey);

	boolean existsByOriginalTransactionId(Long originalTransactionId);

	List<MemberPointTransaction> findAllByMemberProfileIdOrderByCreatedAtDescIdDesc(Long memberProfileId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select transaction from MemberPointTransaction transaction where transaction.id = :id")
	Optional<MemberPointTransaction> findByIdForUpdate(@Param("id") Long id);
}
