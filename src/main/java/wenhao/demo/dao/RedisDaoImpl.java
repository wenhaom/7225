package wenhao.demo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;


@Repository
public class RedisDaoImpl implements RedisDao{



    private RedisTemplate<String, Object> redisTemplate;
    private ChannelTopic topic;
    private HashOperations hashOperations;
    private SetOperations setOperations;



    @Autowired
    public RedisDaoImpl(RedisTemplate<String, Object>  redisTemplate, ChannelTopic topic){
        this.redisTemplate = redisTemplate;
        this.topic = topic;
    }

    @PostConstruct
    private void init(){
        //use hash to store simple property
        hashOperations = redisTemplate.opsForHash();
        //use set to store edge key
        setOperations = redisTemplate.opsForSet();
    }

    @Override
    public boolean insertSet(String key, Object value) {
        if(setOperations.isMember(key, value))
            return false;
        else {
            setOperations.add(key, value);
            return true;
        }
    }

    @Override
    public boolean insertMap(String key, String id, Object value) {
        return hashOperations.putIfAbsent(key, id, value);
    }

    @Override
    public void deleteSet(String key, Object value) {
        setOperations.remove(key, value);
    }

    @Override
    public void deleteMap(String key, String hashKey) {
        hashOperations.delete(key, hashKey);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void enqueue(String input) {
        redisTemplate.convertAndSend(topic.getTopic(), input);
    }

    @Override
    public Map<Object, Object> findMap(String key) {
        return hashOperations.entries(key);
    }

    @Override
    public Set findSet(String key) {
        return setOperations.members(key);
    }

    @Override
    public Set<String> getKeys(String pattern) {
        return redisTemplate.keys(pattern);
    }
}
