-- ========== テーブル: users ==========
CREATE TABLE users (
	id BIGSERIAL PRIMARY KEY,
	username VARCHAR(50) NOT NULL,
	email VARCHAR(100) NOT NULL,
	password_hash TEXT NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	
	-- UNIQUE(username)
	CONSTRAINT uq_users_username UNIQUE (username),
	
	-- UNIQUE(email)
	CONSTRAINT uq_users_email UNIQUE (email)
);

-- ========== テーブル: posts ==========
CREATE TABLE posts (
	id BIGSERIAL PRIMARY KEY,
	content TEXT NOT NULL,
	user_id BIGINT NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	
	-- FK(user_id, users(id))
	CONSTRAINT fk_posts_user
		FOREIGN KEY (user_id) REFERENCES users(id)
		ON DELETE CASCADE
);

-- ========== テーブル: follows ==========
CREATE TABLE follows (
	follower_id BIGINT NOT NULL,
	followee_id BIGINT NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	
	-- FK(follower_id, users(id))
	CONSTRAINT fk_follows_follower
		FOREIGN KEY (follower_id) REFERENCES users(id)
		ON DELETE CASCADE,
		
	-- FK(followee_id, users(id))
	CONSTRAINT fk_follows_followee
		FOREIGN KEY (followee_id) REFERENCES users(id)
		ON DELETE CASCADE,
		
	-- 同一組み合わせの重複フォロー禁止
  CONSTRAINT uq_follows_pair UNIQUE (follower_id, followee_id),
  
  -- 自分自身をフォロー禁止
  CONSTRAINT chk_follows_no_self CHECK (follower_id <> followee_id)
);