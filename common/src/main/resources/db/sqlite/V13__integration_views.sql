CREATE INDEX IF NOT EXISTS idx_permission_user_groups_group_expiry ON permission_user_groups(group_name, expires_at_ms);

DROP VIEW IF EXISTS paradigm_v_players;
CREATE VIEW paradigm_v_players AS
SELECT uuid AS player_uuid, name AS player_name, first_seen_ms, last_seen_ms, playtime_ms
FROM players;

DROP VIEW IF EXISTS paradigm_v_player_groups;
CREATE VIEW paradigm_v_player_groups AS
SELECT pug.uuid AS player_uuid,
  COALESCE(NULLIF(p.name, ''), NULLIF(pu.name, '')) AS player_name,
  pug.group_name,
  pg.description AS group_description,
  pg.prefix AS group_prefix,
  pg.suffix AS group_suffix,
  pg.weight AS group_weight,
  pug.assignment_id,
  pug.assigned_by,
  pug.assigned_at_ms,
  pug.expires_at_ms,
  CASE WHEN pug.expires_at_ms IS NULL THEN 0 ELSE 1 END AS is_temporary,
  CASE WHEN pug.expires_at_ms IS NULL
            OR pug.expires_at_ms > CAST(ROUND((julianday('now') - 2440587.5) * 86400000) AS INTEGER)
       THEN 'ACTIVE' ELSE 'EXPIRED' END AS status,
  pug.contexts AS contexts_json,
  pug.context_hash
FROM permission_user_groups pug
LEFT JOIN players p ON p.uuid = pug.uuid
LEFT JOIN permission_users pu ON pu.uuid = pug.uuid
LEFT JOIN permission_groups pg ON pg.name = pug.group_name;

DROP VIEW IF EXISTS paradigm_v_active_player_groups;
CREATE VIEW paradigm_v_active_player_groups AS
SELECT player_uuid, player_name, group_name, group_description, group_prefix, group_suffix, group_weight,
  assignment_id, assigned_by, assigned_at_ms, expires_at_ms, is_temporary, status, contexts_json, context_hash
FROM paradigm_v_player_groups
WHERE expires_at_ms IS NULL
   OR expires_at_ms > CAST(ROUND((julianday('now') - 2440587.5) * 86400000) AS INTEGER);

DROP VIEW IF EXISTS paradigm_v_group_members;
CREATE VIEW paradigm_v_group_members AS
SELECT player_uuid, player_name, group_name, group_description, group_prefix, group_suffix, group_weight,
  assignment_id, assigned_by, assigned_at_ms, expires_at_ms, is_temporary, status, contexts_json, context_hash
FROM paradigm_v_active_player_groups;

DROP VIEW IF EXISTS paradigm_v_permission_tracks;
CREATE VIEW paradigm_v_permission_tracks AS
SELECT ptm.track_name, ptm.position, ptm.group_name,
  pg.description AS group_description, pg.prefix AS group_prefix, pg.suffix AS group_suffix, pg.weight AS group_weight
FROM permission_track_members ptm
LEFT JOIN permission_groups pg ON pg.name = ptm.group_name;

DROP VIEW IF EXISTS paradigm_v_player_tracks;
CREATE VIEW paradigm_v_player_tracks AS
SELECT apg.player_uuid, apg.player_name, ptm.track_name, apg.group_name, ptm.position, sizes.track_size,
  apg.expires_at_ms, apg.is_temporary, apg.assigned_at_ms, apg.assigned_by, apg.contexts_json, apg.context_hash
FROM paradigm_v_active_player_groups apg
JOIN permission_track_members ptm ON ptm.group_name = apg.group_name
JOIN (
  SELECT track_name, COUNT(*) AS track_size
  FROM permission_track_members
  GROUP BY track_name
) sizes ON sizes.track_name = ptm.track_name;

DROP VIEW IF EXISTS paradigm_v_player_punishment_summary;
CREATE VIEW paradigm_v_player_punishment_summary AS
SELECT p.uuid AS player_uuid, p.name AS player_name,
  COALESCE(SUM(CASE WHEN l.punishment_type = 'BAN' AND l.revoked_at_ms IS NULL
                         AND l.starts_at_ms <= CAST(ROUND((julianday('now') - 2440587.5) * 86400000) AS INTEGER)
                         AND (l.expires_at_ms IS NULL OR l.expires_at_ms > CAST(ROUND((julianday('now') - 2440587.5) * 86400000) AS INTEGER))
                    THEN 1 ELSE 0 END), 0) AS active_bans,
  COALESCE(SUM(CASE WHEN l.punishment_type = 'MUTE' AND l.revoked_at_ms IS NULL
                         AND l.starts_at_ms <= CAST(ROUND((julianday('now') - 2440587.5) * 86400000) AS INTEGER)
                         AND (l.expires_at_ms IS NULL OR l.expires_at_ms > CAST(ROUND((julianday('now') - 2440587.5) * 86400000) AS INTEGER))
                    THEN 1 ELSE 0 END), 0) AS active_mutes,
  COALESCE(SUM(CASE WHEN l.punishment_type = 'JAIL' AND l.revoked_at_ms IS NULL
                         AND l.starts_at_ms <= CAST(ROUND((julianday('now') - 2440587.5) * 86400000) AS INTEGER)
                         AND (l.expires_at_ms IS NULL OR l.expires_at_ms > CAST(ROUND((julianday('now') - 2440587.5) * 86400000) AS INTEGER))
                    THEN 1 ELSE 0 END), 0) AS active_jails,
  COALESCE(SUM(CASE WHEN l.punishment_type = 'WARN' THEN 1 ELSE 0 END), 0) AS warning_count,
  COALESCE(SUM(CASE WHEN l.revoked_at_ms IS NULL
                         AND l.starts_at_ms <= CAST(ROUND((julianday('now') - 2440587.5) * 86400000) AS INTEGER)
                         AND (l.expires_at_ms IS NULL OR l.expires_at_ms > CAST(ROUND((julianday('now') - 2440587.5) * 86400000) AS INTEGER))
                    THEN 1 ELSE 0 END), 0) AS active_punishment_count,
  COUNT(l.punishment_id) AS total_punishment_count,
  MAX(l.created_at_ms) AS last_punishment_at_ms
FROM players p
LEFT JOIN moderation_punishment_ledger l ON l.subject_uuid = p.uuid
GROUP BY p.uuid, p.name;

DROP VIEW IF EXISTS paradigm_v_player_ticket_summary;
CREATE VIEW paradigm_v_player_ticket_summary AS
SELECT t.network_id, t.creator_uuid AS player_uuid,
  COALESCE(NULLIF(p.name, ''), NULLIF(t.creator_name, '')) AS player_name,
  SUM(CASE WHEN t.status IN ('OPEN', 'IN_PROGRESS', 'WAITING_PLAYER', 'WAITING_STAFF') THEN 1 ELSE 0 END) AS open_ticket_count,
  SUM(CASE WHEN t.status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolved_ticket_count,
  SUM(CASE WHEN t.status = 'CLOSED' THEN 1 ELSE 0 END) AS closed_ticket_count,
  COUNT(*) AS total_ticket_count,
  MAX(t.created_at_ms) AS last_ticket_at_ms,
  MAX(t.last_activity_at_ms) AS last_ticket_activity_at_ms
FROM tickets t
LEFT JOIN players p ON p.uuid = t.creator_uuid
WHERE t.creator_uuid IS NOT NULL
GROUP BY t.network_id, t.creator_uuid, COALESCE(NULLIF(p.name, ''), NULLIF(t.creator_name, ''));

DROP VIEW IF EXISTS paradigm_v_servers;
CREATE VIEW paradigm_v_servers AS
SELECT server_id, network_id, server_name, created_at_ms, last_seen_ms
FROM server_instances;

DROP VIEW IF EXISTS paradigm_v_player_home_summary;
CREATE VIEW paradigm_v_player_home_summary AS
SELECT h.uuid AS player_uuid, p.name AS player_name, h.server_id, COUNT(*) AS home_count
FROM player_homes h
LEFT JOIN players p ON p.uuid = h.uuid
GROUP BY h.uuid, p.name, h.server_id;

DROP VIEW IF EXISTS paradigm_v_warps_public;
CREATE VIEW paradigm_v_warps_public AS
SELECT server_id, name, world_id, permission, description, created_by, created_at_ms, updated_at_ms
FROM warps;
