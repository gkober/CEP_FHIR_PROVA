package org.provarules.reference2.builtins;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.provarules.agent2.ProvaReagent;
import org.provarules.kernel2.ProvaConstant;
import org.provarules.kernel2.ProvaDerivationNode;
import org.provarules.kernel2.ProvaGoal;
import org.provarules.kernel2.ProvaKnowledgeBase;
import org.provarules.kernel2.ProvaLiteral;
import org.provarules.kernel2.ProvaObject;
import org.provarules.kernel2.ProvaRule;
import org.provarules.kernel2.ProvaVariable;
import org.provarules.kernel2.ProvaVariablePtr;
import org.provarules.reference2.ProvaConstantImpl;
import org.provarules.kernel2.ProvaList;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class FhirConnectImpl extends ProvaBuiltinImpl {
	FhirContext ctx = FhirContext.forR4();
	IGenericClient client = null;

	public FhirConnectImpl(ProvaKnowledgeBase kb) {
		super(kb, "fhir_connect");
	}
	@Override
	public int getArity() {
		return 2;
	}

	@Override
	public boolean process(ProvaReagent prova, ProvaDerivationNode node, ProvaGoal goal, List<ProvaLiteral> newLiterals,
			ProvaRule query) {
		System.out.println("in FhirConnectImpl");
		
		List<ProvaVariable> variables = query.getVariables();
        ProvaLiteral literal = goal.getGoal();
        ProvaList terms = literal.getTerms();
        ProvaObject[] data = terms.getFixed();

        if (data.length != 2) {
            System.out.println("Syntax error - need two terms");
            return false;
        }else {
        	System.out.println("two terms are included in the call, proceed");
        }
        //quickly checking the terms in prova
        System.out.println(data[0].toString());
        System.out.println(data[1].toString());
        //the url is in data1; so the 2nd entry
        ProvaObject data1 = resolve(data[1], variables);
        String url = (String) ((ProvaConstant) data1).getObject();
        this.client = this.ctx.newRestfulGenericClient(url);
        
        ProvaObject data0 = resolve(data[0], variables);
        
        //check if FHIR-Server is available
        try {
        	URL callingUrl = new URL(url);
        	HttpURLConnection httpConnection = (HttpURLConnection) callingUrl.openConnection();
        	//httpConnection.setRequestProperty("Accept", "application/json");
        	
        	System.out.println(httpConnection.getResponseCode() + httpConnection.getResponseMessage());
        	httpConnection.disconnect();
        	
        	if(httpConnection.getResponseCode() == 400) {
        		ctx = null;
        		return false;
        	}
        
        }catch(Exception e) {
        	e.printStackTrace();
        	ctx = null;
        	return false;
        }
 
        try {
        	//assign connection to variable
        	 ProvaVariable cvar = (ProvaVariable) data0;
        	 cvar.setAssigned(ProvaConstantImpl.create(this.client));
        }catch(Exception e) {
        	e.printStackTrace();
        	ctx = null;
        }
        System.out.println("returning true");
		return true;
	}
	private ProvaObject resolve(ProvaObject o, List<ProvaVariable> variables) {
        if (o instanceof ProvaVariablePtr) {
            ProvaVariablePtr varPtr = (ProvaVariablePtr) o;
            o = variables.get(varPtr.getIndex()).getRecursivelyAssigned();
        }
        return o;
    }

}
