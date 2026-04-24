package com.bloodline.crates.loadout;

import java.io.File;
import java.io.IOException;

public interface LoadoutSerializer {
    LoadoutData read(File file) throws IOException, LoadoutValidationException;

    void write(File file, LoadoutData data) throws IOException;

    String getFileExtension();
}
