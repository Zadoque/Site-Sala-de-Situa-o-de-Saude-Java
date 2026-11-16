redis.call('INCR', KEYS[1])
redis.call('PERSIST', KEYS[1])
redis.call('DEL', KEYS[2])
return 1
