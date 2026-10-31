CREATE TABLE processed_matches (
    match_id     UUID        PRIMARY KEY,
    game_id      VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE player_scores (
    game_id        VARCHAR(64) NOT NULL,
    player_id      VARCHAR(64) NOT NULL,
    total_points   BIGINT      NOT NULL CHECK (total_points >= 0),
    matches_played INTEGER     NOT NULL CHECK (matches_played >= 0),
    updated_at     TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (game_id, player_id)
);

-- Supports rebuilding a game's leaderboard from the database, highest score first.
CREATE INDEX idx_player_scores_game_points ON player_scores (game_id, total_points DESC);
