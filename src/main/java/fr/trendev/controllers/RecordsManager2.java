/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.trendev.controllers;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ReplicatedMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 *
 * @author jsie
 */
@HazelcastReplicatedMap
@ApplicationScoped
public class RecordsManager2 implements RecordsManager {

    private ReplicatedMap<String, LinkedList<String>> map;

    private int maxSize;

    private final String key = "records";

    private static final Logger LOG = Logger.getLogger(RecordsManager2.class.getName());

    public RecordsManager2() {
        List<HazelcastInstance> hzInstances
                = new ArrayList<>(Hazelcast.getAllHazelcastInstances());

        if (hzInstances.isEmpty()) {
            throw new IllegalStateException("No Hazelcast instance available");
        } else {
            // get the first one
            HazelcastInstance hz = hzInstances.get(0);
            this.map = hz.getReplicatedMap(this.getClass().getName());
        }
    }

    @PostConstruct
    @Override
    public void init() {

        // limits the size of the list
        Config config = ConfigProvider.getConfig();
        this.maxSize = Integer.parseInt(
                config.getOptionalValue("RECORDS_MAX_SIZE", String.class)
                        .orElse("20"));

        LOG.log(Level.WARNING, "{0} is now initialized", this.getClass().getSimpleName());

    }

    @PreDestroy
    @Override
    public void close() {
        LOG.log(Level.WARNING, "Destroying {0}", this.getClass().getSimpleName());
    }

    @Override
    public List<String> add(String value) {

        LinkedList<String> records = this.map.getOrDefault(key, new LinkedList<>());

        //pop old entries
        if (records.size() >= maxSize) {
            while (records.size() != maxSize - 1) {
                records.remove();
            }
        }
        records.add(value);

        this.map.put(key, records);

        return Collections.unmodifiableList(records);
    }
}
