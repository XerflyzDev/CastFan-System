INSERT INTO member_profiles (
    user_account_id,
    bio,
    avatar_url,
    cast_update_notifications,
    event_notifications,
    created_at,
    updated_at
)
SELECT
    account.id,
    NULL,
    NULL,
    TRUE,
    TRUE,
    account.created_at,
    account.updated_at
FROM user_accounts account
WHERE account.role = 'CAST'
  AND NOT EXISTS (
      SELECT 1
      FROM member_profiles profile
      WHERE profile.user_account_id = account.id
  );
