package edu.boun.edgecloudsim.research.counterfactual.units;

public final class EngineeringUnits {

    private static final double BITS_PER_BYTE = 8.0;
    private static final double BYTES_PER_KILOBYTE = 1024.0;
    private static final double MILLISECONDS_PER_SECOND = 1000.0;

    private EngineeringUnits() {
        throw new AssertionError(
                "EngineeringUnits cannot be instantiated.");
    }

    public static double secondsToMilliseconds(
            double seconds) {

        validateNonNegative(
                seconds,
                "seconds");

        return seconds * MILLISECONDS_PER_SECOND;
    }

    public static double millisecondsToSeconds(
            double milliseconds) {

        validateNonNegative(
                milliseconds,
                "milliseconds");

        return milliseconds
                / MILLISECONDS_PER_SECOND;
    }

    public static double bytesToKilobytes(
            double bytes) {

        validateNonNegative(
                bytes,
                "bytes");

        return bytes
                / BYTES_PER_KILOBYTE;
    }

    public static double kilobytesToBytes(
            double kilobytes) {

        validateNonNegative(
                kilobytes,
                "kilobytes");

        return kilobytes
                * BYTES_PER_KILOBYTE;
    }

    /*
     * Transmission time in milliseconds.
     *
     * Upload size: KB
     * Bandwidth: Mbps
     */
    public static double calculateTransmissionTimeMs(
            double uploadSizeKb,
            double bandwidthMbps) {

        validateNonNegative(
                uploadSizeKb,
                "uploadSizeKb");

        if(bandwidthMbps <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }

        double uploadBits =
                uploadSizeKb
                * BYTES_PER_KILOBYTE
                * BITS_PER_BYTE;

        double bandwidthBitsPerSecond =
                bandwidthMbps
                * 1_000_000.0;

        double transmissionSeconds =
                uploadBits
                / bandwidthBitsPerSecond;

        return secondsToMilliseconds(
                transmissionSeconds);
    }

    private static void validateNonNegative(
            double value,
            String name) {

        if(Double.isNaN(value)
                || Double.isInfinite(value)
                || value < 0.0) {

            throw new IllegalArgumentException(
                    name
                    + " must be finite and non-negative.");
        }
    }
}