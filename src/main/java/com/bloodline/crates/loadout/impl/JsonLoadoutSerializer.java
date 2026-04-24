package com.bloodline.crates.loadout.impl;

import com.bloodline.crates.loadout.LoadoutData;
import com.bloodline.crates.loadout.LoadoutMapUtil;
import com.bloodline.crates.loadout.LoadoutSerializer;
import com.bloodline.crates.loadout.LoadoutValidationException;
import com.bloodline.crates.loadout.LoadoutValidator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.Map;

public class JsonLoadoutSerializer implements LoadoutSerializer {
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public LoadoutData read(File file) throws IOException, LoadoutValidationException {
        Map<String, Object> map = gson.fromJson(Files.readString(file.toPath()), MAP_TYPE);
        LoadoutData data = LoadoutMapUtil.fromMap(map);
        LoadoutValidator.validate(data);
        return data;
    }

    @Override
    public void write(File file, LoadoutData data) throws IOException {
        Files.writeString(file.toPath(), gson.toJson(LoadoutMapUtil.toMap(data)));
    }

    @Override
    public String getFileExtension() {
        return ".json";
    }
}
