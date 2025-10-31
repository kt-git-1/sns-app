-- posts: ユーザー別のタイムライン取得を高速化
CREATE INDEX IF NOT EXISTS idx_posts_user_id_created_at
	ON posts (user_id, created_at DESC);
	
-- posts: 新着順にタイムライン取得
CREATE INDEX IF NOT EXISTS idx_posts_created_at
	ON posts (created_at DESC);
	
-- follows: 逆方向（フォロワー一覧）を高速化
CREATE INDEX IF NOT EXISTS idx_follows_followee_id
	ON follows (followee_id);