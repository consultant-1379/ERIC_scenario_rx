package com.ericsson.de.scenariorx.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.configuration.Configuration;
import com.google.common.io.Resources;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class RxConfiguration implements Configuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(RxConfiguration.class);
    public static final String LOCATION = "properties/datadriven.properties";
    Properties properties;

    public RxConfiguration() {
        URL resource = Resources.getResource(LOCATION);
        try (InputStream stream = resource.openStream()) {
            properties = new Properties();
            properties.load(stream);
            properties.putAll(System.getProperties());
        } catch (IOException e) {
            LOGGER.error("IOException:", e);
        }
    }

    @Override
    public Properties getProperties() {
        throw new NotImplementedException();
    }

    @Override
    public Object getProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public <T> T getProperty(String s, T t) {
        throw new NotImplementedException();
    }

    @Override
    public <T> T getProperty(String s, Class<T> aClass) {
        throw new NotImplementedException();
    }

    @Override
    public <T> T getProperty(String s, T t, Class<T> aClass) {
        throw new NotImplementedException();
    }

    @Override
    public void setProperty(String s, Object o) {
        throw new NotImplementedException();
    }

    @Override
    public void clearProperty(String s) {
        throw new NotImplementedException();
    }

    @Override
    public void clear() {
        throw new NotImplementedException();
    }

    @Override
    public String getString(String s) {
        return String.valueOf(properties.getProperty(s));
    }

    @Override
    public String getString(String s, String s1) {
        if (properties.containsKey(s)) {
            return String.valueOf(properties.getProperty(s));
        } else {
            return s1;
        }
    }

    @Override
    public double getDouble(String s) {
        throw new NotImplementedException();
    }

    @Override
    public double getDouble(String s, double v) {
        throw new NotImplementedException();
    }

    @Override
    public int getInt(String s) {
        throw new NotImplementedException();
    }

    @Override
    public int getInt(String s, int i) {
        throw new NotImplementedException();
    }

    @Override
    public boolean getBoolean(String s) {
        throw new NotImplementedException();
    }

    @Override
    public boolean getBoolean(String s, boolean b) {
        throw new NotImplementedException();
    }

    @Override
    public String[] getStringArray(String s) {
        throw new NotImplementedException();
    }

    @Override
    public boolean containsKey(String s) {
        throw new NotImplementedException();
    }

    @Override
    public String getSource(String s) {
        throw new NotImplementedException();
    }

    @Override
    public void reload() {
        throw new NotImplementedException();
    }
}
