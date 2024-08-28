package com.ericsson.de.scenariorx.api;

import com.ericsson.de.scenariorx.impl.Flow;
import com.google.common.base.Predicate;

/**
 * Determines the order in which Flow builder methods can be called.
 */
@SuppressWarnings("WeakerAccess")
public final class RxFlowBuilderInterfaces {

    private RxFlowBuilderInterfaces() {
    }

    public interface FlowBuilderStates<T extends Flow>
            extends FlowStart<T>, Before<T>, Steps<T>, AlwaysRun<T>, After<T>, Options<T> {

    }

    /*---------------- STATE INTERFACES ----------------*/

    public interface FlowStart<T extends Flow> extends ToBefore<T>, ToSteps<T> {

    }

    public interface Before<T extends Flow> extends ToSteps<T> {

    }

    public interface Steps<T extends Flow> extends ToSteps<T>, ToAfter<T>, ToOptions<T>, Builder<T> {

        AlwaysRun<T> alwaysRun();

    }

    public interface AlwaysRun<T extends Flow> extends ToSteps<T>, ToAfter<T>, ToOptions<T>, Builder<T> {

    }

    public interface After<T extends Flow> extends ToOptions<T>, Builder<T> {

    }

    public interface Options<T extends Flow> extends ToOptions<T>, Builder<T> {

    }

    /*---------------- DESTINATION INTERFACES ----------------*/

    private interface ToBefore<T extends Flow> {

        Before<T> beforeFlow(Runnable... runnables);

        Before<T> withBefore(RxTestStep... testStep);

    }

    private interface ToSteps<T extends Flow> {

        Steps<T> addTestStep(RxTestStep testStep);

        Steps<T> addSubFlow(Builder<T> subFlow);

        Steps<T> addSubFlow(T flow);

        Steps<T> split(Builder<T>... builders);

        Steps<T> split(T... flows);

    }

    private interface ToAfter<T extends Flow> {

        After<T> afterFlow(Runnable... runnables);

        After<T> withAfter(RxTestStep... testStep);

    }

    private interface ToOptions<T extends Flow> {

        Options<T> withVUsers(int vUsers);

        Options<T> withVUsersAuto();

        Options<T> withDataSources(RxDataSource... dataSources);

        Options<T> withExceptionHandler(RxExceptionHandler exceptionHandler);

        Options<T> runWhile(Predicate<RxDataRecordWrapper> predicate);
    }
}
