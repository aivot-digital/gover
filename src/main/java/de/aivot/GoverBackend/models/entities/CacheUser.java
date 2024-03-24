package de.aivot.GoverBackend.models.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@RedisHash(value = "CacheUser", timeToLive = 30) // Expire after 30 seconds
public class CacheUser implements Serializable {
    @Id
    private String id;
    private String json;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
