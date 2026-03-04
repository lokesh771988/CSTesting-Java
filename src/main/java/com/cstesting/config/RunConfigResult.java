package com.cstesting.config;

/**
 * Result of running a config file. success, index of failed step (if any), error message, and steps count.
 */
public final class RunConfigResult {

    private final boolean success;
    private final int totalSteps;
    private final int failedStepIndex; // 0-based; -1 if no failure
    private final String errorMessage;

    public RunConfigResult(boolean success, int totalSteps, int failedStepIndex, String errorMessage) {
        this.success = success;
        this.totalSteps = totalSteps;
        this.failedStepIndex = failedStepIndex;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    /** 0-based index of the step that failed; -1 if no failure. */
    public int getFailedStepIndex() {
        return failedStepIndex;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
