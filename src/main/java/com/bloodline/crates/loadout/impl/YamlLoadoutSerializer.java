package com.bloodline.crates.loadout.impl;

import com.bloodline.crates.loadout.LoadoutData;
import com.bloodline.crates.loadout.LoadoutMapUtil;
import com.bloodline.crates.loadout.LoadoutSerializer;
import com.bloodline.crates.loadout.LoadoutValidationException;
import com.bloodline.crates.loadout.LoadoutValidator;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class YamlLoadoutSerializer implements LoadoutSerializer {
    @Override
    public LoadoutData read(File file) throws IOException, LoadoutValidationException {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        LoadoutData data = LoadoutMapUtil.fromMap(configuration.getValues(true));
        LoadoutValidator.validate(data);
        return data;
    }

    @Override
    public void write(File file, LoadoutData data) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        for (var entry : LoadoutMapUtil.toMap(data).entrySet()) {
            configuration.set(entry.getKey(), entry.getValue());
        }
        configuration.save(file);
    }

    @Override
    public String getFileExtension() {
        return ".yml";
    }
}
