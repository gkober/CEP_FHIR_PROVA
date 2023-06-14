package org.provarules.reference2.builtins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.provarules.agent2.ProvaReagent;
import org.provarules.kernel2.ProvaConstant;
import org.provarules.kernel2.ProvaDerivationNode;
import org.provarules.kernel2.ProvaGoal;
import org.provarules.kernel2.ProvaKnowledgeBase;
import org.provarules.kernel2.ProvaList;
import org.provarules.kernel2.ProvaLiteral;
import org.provarules.kernel2.ProvaObject;
import org.provarules.kernel2.ProvaRule;
import org.provarules.kernel2.ProvaVariable;
import org.provarules.kernel2.ProvaVariablePtr;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.impl.GenericClient;

public class FhirNativeQueryImpl extends ProvaBuiltinImpl {

	public FhirNativeQueryImpl(ProvaKnowledgeBase kb) {
		super(kb, "fhir_native_query");
	}

	@Override
	public boolean process(ProvaReagent prova, ProvaDerivationNode node, ProvaGoal goal, List<ProvaLiteral> newLiterals,
			ProvaRule query) {
		System.out.println("Processing the fhir-native-query");
		
		List<ProvaVariable> variables = query.getVariables();
        ProvaLiteral literal = goal.getGoal();
        ProvaList terms = literal.getTerms();
        ProvaObject[] data = terms.getFixed();
		//Extracting fhir-server-url from connection
		GenericClient connection = extractValue(data, variables,0);
		String queryParameters = extractValue(data,variables,1).toString();
		String compareParameters = extractValue(data, variables,2).toString();
		
		BigDecimal fhirExtractedValue = null;
		
		System.out.println("This is the FHIR-Server-base configured " + connection.getServerBase().toString());
		System.out.println(compareParameters);
		
		//calling the FHIR-Server
		URL callingUrl;
		try {
			System.out.println("\n\ncalling the fhir-server with parameters");
			//Perform FHIR-Query to the server and print the response		
			callingUrl = new URL(connection.getServerBase()+queryParameters+"&_format=json");
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
	    		IParser parser = connection.getFhirContext().newJsonParser();
	    		Bundle bundle = (Bundle) parser.parseResource(FhirBundleResult);
	    		Observation observation = (Observation) bundle.getEntryFirstRep().getResource();
	    		fhirExtractedValue = observation.getValueQuantity().getValue();
	    		System.out.println(fhirExtractedValue);
	    		
	    	}else {
	    		System.out.println("An error happened... returning false");
	    		return false;
	    	}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//extracting the compareParameter
		char compareSign = compareParameters.charAt(0);
		BigDecimal compareValue = new BigDecimal(compareParameters.substring(1));
		if(compareSign == '<') {
			if(fhirExtractedValue.compareTo(compareValue) == -1) {
				System.out.println("first is less then second");
				return true;
			}else {
				return false;
			}
		}
		if(compareSign == '>') {
			if(fhirExtractedValue.compareTo(compareValue) == 1) {
				System.out.println("first is greater then second");
				return true;
			}else {
				return false;
			}
			
		}
		if(compareSign == '=') {
			if(compareValue.compareTo(fhirExtractedValue) == 0) {
				System.out.println("first is equal to second");
				return true;
			}else {
				return false;
			}
		}
		return true;
	}

	
	
	//Helpermethods to extract
	 static private <T> T extractValue(ProvaObject[] data, List<ProvaVariable> variables, int idx) {
	    T retval;
	    ProvaObject o = resolve(data[idx], variables);
	    if (!(o instanceof ProvaConstant)) {
	        System.out.println("Binding error. Term " + (idx + 1) + " must be constant.");
	        return null;
	    }
	    try {
	        retval = (T) ((ProvaConstant) o).getObject();
	    } catch (ClassCastException cce) {
	    	System.out.println("Binding error. Term " + (idx + 1) + " has wrong type.");
	    	System.out.println("Exception: " +  cce);
	        return null;
	    }
	    return retval;
	}
	static private ProvaObject resolve(ProvaObject o, List<ProvaVariable> variables) {
	    if (o instanceof ProvaVariablePtr) {
	        ProvaVariablePtr varPtr = (ProvaVariablePtr) o;
	        o = variables.get(varPtr.getIndex()).getRecursivelyAssigned();
	    }
	    return o;
	}
}
