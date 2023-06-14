package com.dimse.playground;

/*
* @startuml
* skinparam  monochrome true
FHIR_Client -> FHIR_Server: Send FHIR-Resource (Observation)
FHIR_Server -> DiMSE: Forward FHIR-Resource (Observation)
DiMSE -> DiMSE: Forward internally to RuleEngine (Initiator)
DiMSE -> DiMSE: ExtractValue with specific Code (fhirExtractValueFromObservation)
DiMSE -> DiMSE: hold value internally for evaluation of comapare value
DiMSE -> DiMSE: forward internally to evaluation (fhirCompareBigDecimalValues)
DiMSE -> DiMSE: send internally to Initiator for further processing
DiMSE -> RemoteClients: forward taken decision to registered Websocketclients
RemoteClients -> RemoteClients: present decision to physician.

* @enduml
* 
* 
*/

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Resource;

public class ProvaFHIRCallExtractValueInitiator {
	
	static int resulta = 0;
	static int resultb = 0;

	public static void main(String[] args) {
		
		Observation badValueObservation = new Observation();
		badValueObservation.setStatus(Observation.ObservationStatus.FINAL);
		Coding badValueCoding = badValueObservation.getCode().addCoding();
		badValueCoding.setCode("29463-7").setSystem("http://loinc.org").setDisplay("Body Weight");
		Quantity value = new Quantity();
		value.setValue(5.98).setSystem("http://unitsofmeasure.org").setCode("kg");
		badValueObservation.setValue(value);
		
		Observation goodValueObservation = new Observation();
		badValueObservation.setStatus(Observation.ObservationStatus.FINAL);
		Coding goodValueCoding = goodValueObservation.getCode().addCoding();
		goodValueCoding.setCode("29463-7").setSystem("http://loinc.org").setDisplay("Body Weight");
		Quantity goodValue = new Quantity();
		goodValue.setValue(1.98).setSystem("http://unitsofmeasure.org").setCode("kg");
		goodValueObservation.setValue(goodValue);
		
		int tmp;
		long startTime = System.nanoTime();
		int i =0;
		for(i =0 ;i<1000; i++) {	
			System.out.println("iteration " + i);
			
			tmp = (int) ( Math.random() * 2 + 1);
			System.out.println("Random number: " + tmp);
			ProvaFHIRCallExtractValueTryManyRequests t1 = null;
			if(tmp <= 1) {
				System.out.println("tmp=" + tmp);
				t1 = new ProvaFHIRCallExtractValueTryManyRequests(goodValueObservation);
			}else {
				System.out.println("tmp=" + tmp);
				t1 = new ProvaFHIRCallExtractValueTryManyRequests(badValueObservation);
			}
			try {
				Thread thread1 = new Thread(t1);			
				thread1.start();			
				thread1.join();
				thread1 = null;
			}catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println("__________");
			if(t1.resulta == 0 && t1.resultb == 0) {
				System.out.println("t1- resulta" + t1.resulta);
				System.out.println("t1- resultb" + t1.resultb);
				System.exit(0);
			}
			System.out.println("t1- resulta" + t1.resulta);
			System.out.println("t1- resultb" + t1.resultb);
			resulta =  resulta+t1.resulta;
			resultb =  resultb+t1.resultb;
			t1 = null;
			
		

		}
		System.out.println("################");
		long endTime   = System.nanoTime();
		long totalTime = endTime - startTime;
		System.out.println("Duration: " + TimeUnit.NANOSECONDS.toSeconds(totalTime) + " Seconds");
		System.out.println("Runs: " + i);
		System.out.println("Decision A: " + resulta);
		System.out.println("Decision N: " + resultb);
		System.out.println(resulta+resultb + " count Decisions taken ()");
		System.out.println("Ending Initiator");
		
	}
}
