package com.bloodlinecrates.storage;

import com.bloodlinecrates.model.PlayerProfile;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface StorageProvider {
    void initialize() throws Exception;

    Optional<PlayerProfile> loadProfile(UUID uniqueId) throws Exception;

    void saveProfile(PlayerProfile profile) throws Exception;

    Collection<PlayerProfile> loadAllProfiles() throws Exception;

    void shutdown() throws Exception;
}
