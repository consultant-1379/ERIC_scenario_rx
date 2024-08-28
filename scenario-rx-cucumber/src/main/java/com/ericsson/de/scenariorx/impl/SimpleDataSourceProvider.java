package com.ericsson.de.scenariorx.impl;

import static java.lang.String.format;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import com.ericsson.cifwk.taf.ServiceRegistry;
import com.ericsson.cifwk.taf.configuration.Configuration;
import com.ericsson.cifwk.taf.datasource.DataRecord;
import com.ericsson.cifwk.taf.datasource.TestDataSource;
import com.ericsson.de.scenariorx.api.RxApi;
import com.ericsson.de.scenariorx.api.RxBasicDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.configuration.RxConfiguration;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

public class SimpleDataSourceProvider {
    public static RxDataSource<RxDataRecord> provide(String name, CucumberListener cucumberListener, String defaultLocation) {
        if (propertyOrNull(name, "type") == null) {
            cucumberListener.write(format("%nLoading Data Source `%s` not defined in properties" +
                    " loading from default csv at location: %s%n", name, defaultLocation));
            return RxApi.fromCsv(name, defaultLocation, RxDataRecord.class);
        }

        return provide(name, cucumberListener);
    }

    public static RxDataSource<RxDataRecord> provide(String name, CucumberListener cucumberListener) {
        String type = getProperty(name, "type");

        if ("csv".equals(type)) {
            String location = getProperty(name, "location");
            cucumberListener.write(format("%nLoading Data Source `%s` from csv at location: %s%n", name, location));
            return RxApi.fromCsv(name, location, RxDataRecord.class);
        }
        throw new IllegalArgumentException("Unable to find Data Source with type " + type);
    }

    private static String getProperty(String name, String propName) {
        String prop = propertyOrNull(name, propName);
        checkNotNull(prop, "Unable to find `%s` of Data Source `%s` in `%s` " +
                "or system properties", propName, name, RxConfiguration.LOCATION);
        return prop;
    }

    private static String propertyOrNull(String name, String propName) {
        Configuration configuration = ServiceRegistry.getConfigurationProvider().get();
        return configuration.getString("dataprovider." + name + "." + propName, null);
    }
}
