/*
 * Title:        EdgeCloudSim - M/M/1 Queue model implementation
 * 
 * Description: 
 * MM1Queue implements M/M/1 Queue model for WLAN and WAN communication
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.network;

import org.cloudbus.cloudsim.core.CloudSim;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.utils.Location;

public class MM1Queue extends NetworkModel {
	private double WlanPoissonMean; //seconds
	private double WanPoissonMean; //seconds
	private double avgTaskInputSize; //bytes
	private double avgTaskOutputSize; //bytes
	private int maxNumOfClientsInPlace;
	
	public MM1Queue(int _numberOfMobileDevices, String _simScenario) {
		super(_numberOfMobileDevices, _simScenario);
	}


	@Override
	public void initialize() {
		WlanPoissonMean=0;
		WanPoissonMean=0;
		avgTaskInputSize=0;
		avgTaskOutputSize=0;
		maxNumOfClientsInPlace=0;
		
		//Calculate interarrival time and task sizes
		double numOfTaskType = 0;
		SimSettings SS = SimSettings.getInstance();
		for (int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
			double weight = SS.getTaskLookUpTable()[i][0]/(double)100;
			if(weight != 0) {
				WlanPoissonMean += (SS.getTaskLookUpTable()[i][2])*weight;
				
				double percentageOfCloudCommunication = SS.getTaskLookUpTable()[i][1];
				WanPoissonMean += (WlanPoissonMean)*((double)100/percentageOfCloudCommunication)*weight;
				
				avgTaskInputSize += SS.getTaskLookUpTable()[i][5]*weight;
				
				avgTaskOutputSize += SS.getTaskLookUpTable()[i][6]*weight;
				
				numOfTaskType++;
			}
		}
		
		WlanPoissonMean = WlanPoissonMean/numOfTaskType;
		avgTaskInputSize = avgTaskInputSize/numOfTaskType;
		avgTaskOutputSize = avgTaskOutputSize/numOfTaskType;
	}

    /**
    * source device is always mobile device in our simulation scenarios!
    */
	@Override
	public double getUploadDelay(int sourceDeviceId, int destDeviceId, Task task) {
		double delay = 0;
		Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(sourceDeviceId,CloudSim.clock());

		//mobile device to cloud server
		if(destDeviceId == SimSettings.CLOUD_DATACENTER_ID){
			double wlanDelay = getWlanUploadDelay(accessPointLocation, CloudSim.clock());
			double wanDelay = getWanUploadDelay(accessPointLocation, CloudSim.clock() + wlanDelay);
			if(wlanDelay > 0 && wanDelay >0)
				delay = wlanDelay + wanDelay;
		}
		//mobile device to edge orchestrator
		else if(destDeviceId == SimSettings.EDGE_ORCHESTRATOR_ID){
			delay = getWlanUploadDelay(accessPointLocation, CloudSim.clock()) +
					SimSettings.getInstance().getInternalLanDelay();
		}
		//mobile device to edge device (wifi access point)
		else if (destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
			delay = getWlanUploadDelay(accessPointLocation, CloudSim.clock());
		}
		
		return delay;
	}

    /**
    * destination device is always mobile device in our simulation scenarios!
    */
	@Override
	public double getDownloadDelay(int sourceDeviceId, int destDeviceId, Task task) {
		//Special Case -> edge orchestrator to edge device
		if(sourceDeviceId == SimSettings.EDGE_ORCHESTRATOR_ID &&
				destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
			return SimSettings.getInstance().getInternalLanDelay();
		}

		double delay = 0;
		Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(destDeviceId,CloudSim.clock());
		
		//cloud server to mobile device
		if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID){
			double wlanDelay = getWlanDownloadDelay(accessPointLocation, CloudSim.clock());
			double wanDelay = getWanDownloadDelay(accessPointLocation, CloudSim.clock() + wlanDelay);
			if(wlanDelay > 0 && wanDelay >0)
				delay = wlanDelay + wanDelay;
		}
		//edge device (wifi access point) to mobile device
		else{
			delay = getWlanDownloadDelay(accessPointLocation, CloudSim.clock());
			
			EdgeHost host = (EdgeHost)(SimManager.
					getInstance().
					getEdgeServerManager().
					getDatacenterList().get(sourceDeviceId).
					getHostList().get(0));
			
			//if source device id is the edge server which is located in another location, add internal lan delay
			//in our scenasrio, serving wlan ID is equal to the host id, because there is only one host in one place
			if(host.getLocation().getServingWlanId() != accessPointLocation.getServingWlanId())
				delay += (SimSettings.getInstance().getInternalLanDelay() * 2);
		}
		
		return delay;
	}
	
	@Override
	public NetworkStateSnapshot getNetworkState(
	        int sourceDeviceId,
	        int destDeviceId,
	        Task task) {

	    double currentTime = CloudSim.clock();

	    Location accessPointLocation =
	            SimManager.getInstance()
	                    .getMobilityModel()
	                    .getLocation(sourceDeviceId, currentTime);

	    int deviceCount =
	            getDeviceCount(accessPointLocation, currentTime);

	    int servingWlanId =
	            accessPointLocation.getServingWlanId();

	    double uploadDelaySeconds;
	    double propagationDelaySeconds = 0;

	    double configuredBandwidthMbps;
	    double availableBandwidthMbps;

	    String destinationType;

	    if (destDeviceId == SimSettings.CLOUD_DATACENTER_ID) {

	        destinationType = "CLOUD";

	        double wlanDelay =
	                getWlanUploadDelay(accessPointLocation, currentTime);

	        double wanDelay = -1;

	        if (wlanDelay > 0) {
	            wanDelay = getWanUploadDelay(
	                    accessPointLocation,
	                    currentTime + wlanDelay);
	        }

	        if (wlanDelay > 0 && wanDelay > 0) {
	            uploadDelaySeconds = wlanDelay + wanDelay;
	        }
	        else {
	            uploadDelaySeconds = -1;
	        }

	        propagationDelaySeconds =
	                SimSettings.getInstance()
	                        .getWanPropogationDelay();

	        double wlanConfiguredMbps =
	                SimSettings.getInstance()
	                        .getWlanBandwidth() / 1000.0;

	        double wanConfiguredMbps =
	                SimSettings.getInstance()
	                        .getWanBandwidth() / 1000.0;

	        configuredBandwidthMbps =
	                Math.min(wlanConfiguredMbps, wanConfiguredMbps);

	        double availableWlanMbps =
	                calculateAvailableBandwidthMbps(
	                        SimSettings.getInstance().getWlanBandwidth(),
	                        WlanPoissonMean,
	                        avgTaskInputSize,
	                        deviceCount);

	        double availableWanMbps =
	                calculateAvailableBandwidthMbps(
	                        SimSettings.getInstance().getWanBandwidth(),
	                        WanPoissonMean,
	                        avgTaskInputSize,
	                        deviceCount);

	        availableBandwidthMbps =
	                Math.min(availableWlanMbps, availableWanMbps);
	    }
	    else if (destDeviceId == SimSettings.EDGE_ORCHESTRATOR_ID) {

	        destinationType = "EDGE_ORCHESTRATOR";

	        double wlanDelay =
	                getWlanUploadDelay(accessPointLocation, currentTime);

	        if (wlanDelay > 0) {
	            uploadDelaySeconds =
	                    wlanDelay +
	                    SimSettings.getInstance()
	                            .getInternalLanDelay();
	        }
	        else {
	            uploadDelaySeconds = -1;
	        }

	        propagationDelaySeconds =
	                SimSettings.getInstance()
	                        .getInternalLanDelay();

	        configuredBandwidthMbps =
	                SimSettings.getInstance()
	                        .getWlanBandwidth() / 1000.0;

	        availableBandwidthMbps =
	                calculateAvailableBandwidthMbps(
	                        SimSettings.getInstance().getWlanBandwidth(),
	                        WlanPoissonMean,
	                        avgTaskInputSize,
	                        deviceCount);
	    }
	    else if (destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {

	        destinationType = "EDGE";

	        uploadDelaySeconds =
	                getWlanUploadDelay(
	                        accessPointLocation,
	                        currentTime);

	        configuredBandwidthMbps =
	                SimSettings.getInstance()
	                        .getWlanBandwidth() / 1000.0;

	        availableBandwidthMbps =
	                calculateAvailableBandwidthMbps(
	                        SimSettings.getInstance().getWlanBandwidth(),
	                        WlanPoissonMean,
	                        avgTaskInputSize,
	                        deviceCount);
	    }
	    else {

	        destinationType = "UNKNOWN";
	        uploadDelaySeconds = -1;
	        configuredBandwidthMbps = -1;
	        availableBandwidthMbps = -1;
	    }

	    boolean networkFeasible =
	            uploadDelaySeconds > 0 &&
	            availableBandwidthMbps > 0;

	    String infeasibilityReason;

	    if (networkFeasible) {
	        infeasibilityReason = "NONE";
	    }
	    else if (availableBandwidthMbps == 0) {
	        infeasibilityReason =
	                "INSUFFICIENT_BANDWIDTH";
	    }
	    else if (uploadDelaySeconds <= 0) {
	        infeasibilityReason =
	                "NETWORK_QUEUE_UNSTABLE";
	    }
	    else {
	        infeasibilityReason =
	                "NETWORK_STATE_UNAVAILABLE";
	    }

	    return new NetworkStateSnapshot(
	            destinationType,
	            servingWlanId,
	            deviceCount,
	            configuredBandwidthMbps,
	            availableBandwidthMbps,
	            propagationDelaySeconds,
	            uploadDelaySeconds,
	            networkFeasible,
	            infeasibilityReason);
	}
	
	public int getMaxNumOfClientsInPlace(){
		return maxNumOfClientsInPlace;
	}
	
	private int getDeviceCount(Location deviceLocation, double time){
		int deviceCount = 0;
		
		for(int i=0; i<numberOfMobileDevices; i++) {
			Location location = SimManager.getInstance().getMobilityModel().getLocation(i,time);
			if(location.equals(deviceLocation))
				deviceCount++;
		}
		
		//record max number of client just for debugging
		if(maxNumOfClientsInPlace<deviceCount)
			maxNumOfClientsInPlace = deviceCount;
		
		return deviceCount;
	}
	
	private double calculateMM1(double propogationDelay, int bandwidth /*Kbps*/, double PoissonMean, double avgTaskSize /*KB*/, int deviceCount){
		double Bps=0, mu=0, lamda=0;
		
		avgTaskSize = avgTaskSize * (double)1000; //convert from KB to Byte
		
		Bps = bandwidth * (double)1000 / (double)8; //convert from Kbps to Byte per seconds
                lamda = ((double)1/(double)PoissonMean); //task per seconds
		mu = Bps / avgTaskSize ; //task per seconds
		double result = (double)1 / (mu-lamda*(double)deviceCount);
		
		result += propogationDelay;
		
		return (result > 5) ? -1 : result;
	}
	
	/**
	 * Estimates the residual bandwidth after accounting for the traffic offered
	 * by mobile devices sharing the same network path.
	 *
	 * @param bandwidthKbps configured link bandwidth in Kbps
	 * @param poissonMean mean task interarrival time in seconds
	 * @param avgTaskSizeKB average task size in KB
	 * @param deviceCount number of devices sharing the network
	 * @return estimated residual bandwidth in Mbps
	 */
	private double calculateAvailableBandwidthMbps(
	        int bandwidthKbps,
	        double poissonMean,
	        double avgTaskSizeKB,
	        int deviceCount) {

	    if (bandwidthKbps <= 0 ||
	            poissonMean <= 0 ||
	            avgTaskSizeKB <= 0 ||
	            deviceCount < 0) {

	        return -1;
	    }

	    double linkCapacityBytesPerSecond =
	            bandwidthKbps * 1000.0 / 8.0;

	    double averageTaskSizeBytes =
	            avgTaskSizeKB * 1000.0;

	    double arrivalRatePerDevice =
	            1.0 / poissonMean;

	    double offeredTrafficBytesPerSecond =
	            arrivalRatePerDevice *
	            deviceCount *
	            averageTaskSizeBytes;

	    double residualBytesPerSecond =
	            linkCapacityBytesPerSecond -
	            offeredTrafficBytesPerSecond;

	    if (residualBytesPerSecond < 0) {
	        residualBytesPerSecond = 0;
	    }

	    return residualBytesPerSecond * 8.0 / 1_000_000.0;
	}
	
	private double getWlanDownloadDelay(Location accessPointLocation, double time) {
		return calculateMM1(0,
				SimSettings.getInstance().getWlanBandwidth(),
				WlanPoissonMean,
				avgTaskOutputSize,
				getDeviceCount(accessPointLocation, time));
	}
	
	private double getWlanUploadDelay(Location accessPointLocation, double time) {
		return calculateMM1(0,
				SimSettings.getInstance().getWlanBandwidth(),
				WlanPoissonMean,
				avgTaskInputSize,
				getDeviceCount(accessPointLocation, time));
	}
	
	private double getWanDownloadDelay(Location accessPointLocation, double time) {
		return calculateMM1(SimSettings.getInstance().getWanPropogationDelay(),
				SimSettings.getInstance().getWanBandwidth(),
				WanPoissonMean,
				avgTaskOutputSize,
				getDeviceCount(accessPointLocation, time));
	}
	
	private double getWanUploadDelay(Location accessPointLocation, double time) {
		return calculateMM1(SimSettings.getInstance().getWanPropogationDelay(),
				SimSettings.getInstance().getWanBandwidth(),
				WanPoissonMean,
				avgTaskInputSize,
				getDeviceCount(accessPointLocation, time));
	}

	@Override
	public void uploadStarted(Location accessPointLocation, int destDeviceId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void uploadFinished(Location accessPointLocation, int destDeviceId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void downloadStarted(Location accessPointLocation, int sourceDeviceId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void downloadFinished(Location accessPointLocation, int sourceDeviceId) {
		// TODO Auto-generated method stub
		
	}
}
