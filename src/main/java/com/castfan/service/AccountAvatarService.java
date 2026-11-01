package com.castfan.service;

import java.util.Optional;

import com.castfan.entity.AccountAvatar;
import com.castfan.repository.AccountAvatarRepository;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.UserAccountRepository;
import com.castfan.entity.UserStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AccountAvatarService {

	private final AccountAvatarRepository accountAvatarRepository;
	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final CastProfileRepository castProfileRepository;

	public AccountAvatarService(AccountAvatarRepository accountAvatarRepository,
		UserAccountRepository userAccountRepository, MemberProfileRepository memberProfileRepository,
		CastProfileRepository castProfileRepository) {
		this.accountAvatarRepository = accountAvatarRepository;
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.castProfileRepository = castProfileRepository;
	}

	@Transactional(readOnly = true)
	public boolean hasAvatar(String email) {
		return accountAvatarRepository.existsByUserAccountEmailIgnoreCase(email);
	}

	@Transactional(readOnly = true)
	public Optional<AvatarData> load(String email) {
		return accountAvatarRepository.findByUserAccountEmailIgnoreCase(email)
			.map(avatar -> new AvatarData(avatar.getContentType(), avatar.getImageData()));
	}

	@Transactional(readOnly = true)
	public Optional<AvatarData> loadPublic(Long accountId) {
		return userAccountRepository.findById(accountId)
			.filter(account -> account.getStatus() == UserStatus.ACTIVE)
			.flatMap(account -> accountAvatarRepository.findById(account.getId()))
			.map(avatar -> new AvatarData(avatar.getContentType(), avatar.getImageData()));
	}

	@Transactional
	public void upload(String email, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new DomainRuleViolationException("Choose an image to upload.");
		}
		var upload = ImageUploadSupport.read(file, "Profile");
		var account = userAccountRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new UsernameNotFoundException("Account not found"));
		var existingAvatar = accountAvatarRepository.findById(account.getId());
		var avatar = existingAvatar.orElseGet(() -> new AccountAvatar(account, upload.contentType(), upload.data()));
		if (existingAvatar.isPresent()) {
			avatar.update(upload.contentType(), upload.data());
		}
		accountAvatarRepository.save(avatar);
		syncProfileUrl(account.getId(), "/avatars/" + account.getId());
	}

	@Transactional
	public void remove(String email) {
		var account = userAccountRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new UsernameNotFoundException("Account not found"));
		accountAvatarRepository.deleteById(account.getId());
		syncProfileUrl(account.getId(), null);
	}

	private void syncProfileUrl(Long accountId, String avatarUrl) {
		memberProfileRepository.findByUserAccountId(accountId).ifPresent(profile -> {
			profile.updateDetails(profile.getBio(), avatarUrl, profile.isCastUpdateNotifications(), profile.isEventNotifications());
			memberProfileRepository.save(profile);
		});
		castProfileRepository.findByUserAccountId(accountId).ifPresent(profile -> {
			profile.updatePublicDetails(profile.getStageName(), profile.getStageNameKey(), profile.getBio(), avatarUrl);
			castProfileRepository.save(profile);
		});
	}

	public record AvatarData(String contentType, byte[] imageData) {
	}
}
