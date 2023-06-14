package com.dimse.playground;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 * in this class the submitted payload is a fhir-observation-resource
 */
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.provarules.service.EPService;
import org.provarules.service.ProvaService;
import org.provarules.service.impl.ProvaServiceImpl;


import com.dimse.REST_API.WebSocketServer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.websocket.Session;

public class ProvaFHIRCallExtractValueTryManyRequests implements EPService, Runnable {

		static final String kAgent = "prova";
		static final String kPort = null;
		static String receiver_rulebase = "src/main/resources/provarules/FhirCallExtractValueManyRequests.prova";
		private ProvaService service;
		
		Observation fhirObservation = null;
		public int resulta = 0;
		public int resultb = 0;
		
		private boolean provaResponse = false;
		

		
	public ProvaFHIRCallExtractValueTryManyRequests() {
		//check if receiver_rulebase-file exists
		File f = new File(receiver_rulebase);
		if(!f.exists()) {
			try {
				receiver_rulebase = new java.io.File(".").getCanonicalPath()+"/../webapps/DistributedMedicalServiceEngine/WEB-INF/classes/provarules/FhirCallExtractValueManyRequests.prova";
				File f_other = new File(receiver_rulebase);
				if(!f_other.exists()) {
					System.out.println("other not existing");;
				}
			}catch(Exception e) {
				System.out.println("even the other file not existent");
				return;
			}
		}
			
		
		service = new ProvaServiceImpl();
	    service.init();
	     // Register the runner as a callback EPService
	    service.register("runner_1", this);
	    String receiver = service.instance("receiver", "");
		service.consult(receiver, receiver_rulebase, "receiver1");
	}
	public ProvaFHIRCallExtractValueTryManyRequests(Observation obs) {
		File f = new File(receiver_rulebase);
		if(!f.exists()) {
			try {
				receiver_rulebase = new java.io.File(".").getCanonicalPath()+"/../webapps/DistributedMedicalServiceEngine/WEB-INF/classes/provarules/FhirCallExtractValueManyRequests.prova";
				File f_other = new File(receiver_rulebase);
				if(!f_other.exists()) {
					System.out.println("other not existing");;
				}
			}catch(RuntimeException re) {
				System.out.println("runtimeexception happened: ");
				re.printStackTrace();
			}catch(Exception e) {
				System.out.println("even the other file not existent");
				return;
			}
		}
		service = new ProvaServiceImpl();
	    service.init();
	    service.register("runner_1", this);
	    String receiver = service.instance("receiver", "");
		service.consult(receiver, receiver_rulebase, "receiver1");
		fhirObservation = obs;
	}
	public void run() {
			
		Map<String,Observation> payload = new HashMap<String,Observation>();
		payload.put("a", fhirObservation);
		service.send("xid", "receiver","dimse", "inform", payload, this);
		
        try {
            synchronized (this) {
            	while(!provaResponse) {
            		wait();
            	}
//            	wait(100);
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
        System.out.println("Confirmed that the messages have been received");
        service.destroy();
	}
	
	@Override
	public void send(String xid, String dest, String agent, String verb, Object payload, EPService callback) {
		System.out.println("sent to this method");
        System.out.println("Received " + verb + " from " + agent + " :" + payload);
        System.out.println("From XID: " + xid);
        
        String forwardToWebsocket = "nothing";
       
        if(payload.toString().startsWith("{a")) {
        	System.out.println("\n\n\n received A!! \n");
        	this.resulta = +1;
        	forwardToWebsocket = "A";
        }
        if(payload.toString().startsWith("{b")) {
        	System.out.println("\n\n\n received B!! \n");
        	this.resultb = +1;
        	forwardToWebsocket = "B";
        }
        try {
        	synchronized (this) {
        		this.provaResponse = true;
        		System.out.println("Calling Websockets - for sending to connected clients...");
        		WebSocketServer websock = new WebSocketServer();
				websock.onMessage("{Decision: "+ forwardToWebsocket + "}");
        		notify();
        	}
        }catch (Exception e) {
        	e.printStackTrace();
        }
       
	}
	
	private Observation fetchObservationFromFhirStore(String serverBase, String queryParameters) throws IOException {
		FhirContext ctx = FhirContext.forR4();
		IGenericClient client = null;
		
		client = ctx.newRestfulGenericClient(serverBase);

		URL callingUrl = new URL(serverBase+queryParameters+"&_format=json");
		System.out.println(callingUrl.toString());
		HttpURLConnection httpConnection = (HttpURLConnection) callingUrl.openConnection();
    	System.out.println(httpConnection.getResponseMessage());
    	//reading the responseStream
    	BufferedReader responseStream = null;
    	
    	if(100<= httpConnection.getResponseCode() && httpConnection.getResponseCode()<=399) {
    		responseStream = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
    		String currentLine;
    		StringBuilder sb = new StringBuilder();
    		while((currentLine = responseStream.readLine()) != null) {
    			System.out.println(currentLine);
    			sb.append(currentLine + "\n");
    		}
    		//parsing the response to a string, for handling then as a FHIR-Bundle
    		String FhirBundleResult = sb.toString();
    		IParser parser = client.getFhirContext().newJsonParser();
    		Bundle bundle = (Bundle) parser.parseResource(FhirBundleResult);
    		Observation observation = (Observation) bundle.getEntryFirstRep().getResource();
    		return observation;
    	}
    	//this.service.destroy();
    	return null;
	}
}
