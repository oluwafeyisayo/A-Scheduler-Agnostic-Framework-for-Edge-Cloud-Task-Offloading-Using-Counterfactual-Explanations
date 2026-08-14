package edu.boun.edgecloudsim.research.counterfactual.features;

public final class CounterfactualFeatureNames {

    public static final String REQUIRED_CPU_PERCENT =
            "required_cpu_percent";

    public static final String SELECTED_AVAILABLE_CPU_PERCENT =
            "selected_available_cpu_percent";

    public static final String BEST_AVAILABLE_CPU_PERCENT =
            "best_available_cpu_percent";

    public static final String AVAILABLE_BANDWIDTH_MBPS =
            "available_bandwidth_mbps";

    public static final String NETWORK_LATENCY_MS =
            "network_latency_ms";

    public static final String TASK_LENGTH_MI =
            "task_length_mi";

    public static final String UPLOAD_SIZE_KB =
            "upload_size_kb";

    public static final String DOWNLOAD_SIZE_KB =
            "download_size_kb";
    
    public static final String REQUIRED_CORES =
            "required_cores";

    private CounterfactualFeatureNames() {
        throw new AssertionError(
                "CounterfactualFeatureNames cannot be instantiated.");
    }
}