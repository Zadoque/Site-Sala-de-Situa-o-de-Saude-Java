local generation = redis.call('GET', KEYS[2]) or '0'
local active = redis.call('GET', KEYS[3])
if active and ARGV[9] ~= '1' then return 2 end
if ARGV[9] == '1' then
  generation = tostring(redis.call('INCR', KEYS[2]))
  redis.call('PERSIST', KEYS[2])
elseif generation ~= ARGV[6] then return 0 end
if redis.call('EXISTS', KEYS[1]) ~= 0 then return 0 end
redis.call('HSET', KEYS[1], 'id', ARGV[1], 'userId', ARGV[2], 'createdAt', ARGV[3],
  'expiresAt', ARGV[4], 'generation', generation, 'family', ARGV[7], 'credential', ARGV[8], 'revokedAt', '', 'replacedBy', '')
redis.call('PEXPIRE', KEYS[1], ARGV[5])
redis.call('SET', KEYS[3], generation, 'PX', ARGV[5])
return 1
