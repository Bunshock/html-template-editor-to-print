package com.bunshock.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.bunshock.model.AppProfile;
import com.google.gson.Gson;

public class ProfileService {
    private static final Gson gson = new Gson();

    public static AppProfile loadProfile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, AppProfile.class);
        }
    }
}
