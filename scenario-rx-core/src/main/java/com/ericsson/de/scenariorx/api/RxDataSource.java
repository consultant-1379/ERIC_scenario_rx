/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 */

package com.ericsson.de.scenariorx.api;

import static java.lang.String.format;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.ericsson.de.scenariorx.impl.DefaultDataRecordTransformer;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;

/**
 * Definition of the Data source with base functionality. Contains detailed actions to describe and operate on Data Source.
 */
public abstract class RxDataSource<T> implements Iterable {

    private static final String SUMMARY_TEMPLATE = "%s Data Source '%s'";

    protected String name;
    private final Class<?> type;
    private boolean shared;
    private boolean cyclic;
    final List<Predicate<? super RxDataRecord>> filters = newArrayList();

    protected RxDataSource(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Determines whether Data Source is shared.
     * @return true if shared, else false.
     */
    public boolean isShared() {
        return shared;
    }

    /**
     * Determines whether Data Source is cyclic.
     * @return true if cyclic, else false.
     */
    public boolean isCyclic() {
        return cyclic;
    }

    /**
     * @return name of Data Source.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Iterator of {@link RxDataRecord} (Rx Scenario Data Records)
     */
    @Override
    public final Iterator<? extends RxDataRecord> iterator() {
        return Iterators.filter(getIterator(), Predicates.and(filters));
    }

    /**
     * @return Count of {@link RxDataRecord} in Data Source
     */
    public int getSize() {
        return Iterators.size(iterator());
    }

    /**
     * Method for overrides
     * @return Transformer, that should be able to create object of given type
     * from {@link RxDataRecord} (Rx Scenario Data Records)
     * @see DefaultDataRecordTransformer
     */
    public RxDataRecordTransformer<T> getDataRecordTransformer() {
        return new DefaultDataRecordTransformer<>();
    }

    /**
     * Method for overrides
     */
    protected abstract Iterator<? extends RxDataRecord> getIterator();

    /**
     * Method for overrides
     */
    protected abstract RxDataSource<T> newDefinition();

    /**
     * Each record will be processed only by one of vUsers
     * I.e. Data Records will be "shared" between vUsers
     */
    public RxDataSource<T> shared() {
        RxDataSource<T> copy = copy();
        copy.shared = true;
        return copy;
    }

    /**
     * Data Source will be repeated until other Data Sources defined on same flow will provide records
     * Note: At least one Data Source defined on flow should be not cyclic
     */
    public RxDataSource<T> cyclic() {
        RxDataSource<T> copy = copy();
        copy.cyclic = true;
        return copy;
    }

    private RxDataSource<T> copy() {
        RxDataSource<T> definition = newDefinition();
        definition.shared = shared;
        definition.cyclic = cyclic;
        definition.filters.addAll(filters);

        return definition;
    }

    /**
     * Rename Data Source in scope of Flow
     * @param newName New DataSource name.
     * @return Newly named RxDataSource
     */
    public RxDataSource rename(String newName) {
        RxDataSource<T> copy = copy();
        copy.name = newName;
        return copy;
    }

    protected Class<?> getType() {
        return type;
    }

    /**
     * Filter Data Source by {@code field}
     *
     * @return FilterBuilder
     */
    public FilterBuilder filterField(String field) {
        return new FilterBuilder(field);
    }

    public class FilterBuilder {
        private final String field;

        FilterBuilder(String field) {
            this.field = field;
        }

        /**
         * Data Record value of given Data Source should be equal to {@code value}
         */
        public RxDataSource<T> equalTo(final Object value) {
            checkArgument(value != null);
            RxDataSource<T> copy = copy();

            copy.filters.add(new Predicate<RxDataRecord>() {
                                 @Override
                                 public boolean apply(RxDataRecord dataRecord) {
                                     Object fromDataRecord = getFieldValue(dataRecord, value.getClass());
                                     return value.equals(fromDataRecord);
                                 }
                             }
            );

            return copy;
        }

        /**
         * Data Record value of given Data Source should be equal to String {@code value} ignoring case
         * Note: Data Record value will be transformed to String
         */
        public RxDataSource<T> equalToIgnoreCase(final String value) {
            checkArgument(value != null);
            RxDataSource<T> copy = copy();

            copy.filters.add(new Predicate<RxDataRecord>() {
                                 @Override
                                 public boolean apply(RxDataRecord dataRecord) {
                                     Object fromDataRecord = getFieldValue(dataRecord, Object.class);
                                     return value.equalsIgnoreCase(valueToString(fromDataRecord));
                                 }
                             }
            );

            return copy;
        }

        /**
         * Data Record value of given Data Source should contain {@code value}
         * Note: Data Record value will be transformed to String
         */
        public RxDataSource<T> contains(final String value) {
            checkArgument(value != null);
            RxDataSource<T> copy = copy();

            copy.filters.add(new Predicate<RxDataRecord>() {
                @Override
                public boolean apply(RxDataRecord dataRecord) {
                    Object fromDataRecord = getFieldValue(dataRecord, Object.class);
                    return valueToString(fromDataRecord).contains(value);
                }
            });

            return copy;
        }

        private String valueToString(Object fromDataRecord) {
            checkNotNull(fromDataRecord);
            if (fromDataRecord.getClass().isArray()) {
                return Arrays.toString((Object[]) fromDataRecord);
            }
            return fromDataRecord.toString();
        }

        //todo public DataSourceDefinition matchParent(String value) {return DataSourceDefinition.this;}

        private <U> U getFieldValue(RxDataRecord dataRecord, Class<U> type) {
            Object fieldValue = dataRecord.getFieldValue(field);
            checkArgument(fieldValue != null, "Trying to filter by not existing field: " + field);
            return getDataRecordTransformer().convert(field, fieldValue, type);
        }
    }

    @Override
    public String toString() {
        String dataSourceType = Joiner.on(", ").skipNulls().join(new String[]{
                shared ? "shared" : null,
                cyclic ? "cyclic" : null
        });
        return format(SUMMARY_TEMPLATE, dataSourceType, name).trim();
    }
}
