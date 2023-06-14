package org.provarules.reference2.builtins;

import java.math.BigDecimal;
import java.util.List;

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

public class FhirCompareBigDecimalValues extends ProvaBuiltinImpl {

	public FhirCompareBigDecimalValues(ProvaKnowledgeBase kb) {
		super(kb, "fhirCompareBigDecimalValues");
	}

	@Override
	public boolean process(ProvaReagent prova, ProvaDerivationNode node, ProvaGoal goal, List<ProvaLiteral> newLiterals,
			ProvaRule query) {
		System.out.println("hitting the comparison of values");
		
		List<ProvaVariable> variables = query.getVariables();
        ProvaLiteral literal = goal.getGoal();
        ProvaList terms = literal.getTerms();
        ProvaObject[] data = terms.getFixed();
        
        
		//read the values from the prova-file
        //FirstValue is the observation
        BigDecimal firstValue = (BigDecimal) extractValue(data, variables,0);
        System.out.println(firstValue);
        //SecondValue is the compareTo
        Double secondValueString = (Double) extractValue(data, variables,1);
        BigDecimal secondValue = new BigDecimal(secondValueString);
        System.out.println(secondValue);
        //3rd value is the direction <=>
        String thirdValueString = (String) extractValue(data, variables,2);
        System.out.println(thirdValueString);

		//compare and return
        if(thirdValueString.equals("<")) {
        	if(firstValue.compareTo(secondValue)<0) {
        		return true;
        	}
        }
        if(thirdValueString.equals(">")) {
        	if(firstValue.compareTo(secondValue)>0) {
        		return true;
        	}
        }
        if(thirdValueString.equals("=")) {
        	if(firstValue.compareTo(secondValue) == 0) {
        		return true;
        	}
        }
        if(thirdValueString.equals("<=")) {
        	if(firstValue.compareTo(secondValue) <= 0) {
        		return true;
        	}
        }
        if(thirdValueString.equals(">=")) {
        	if(firstValue.compareTo(secondValue) >= 1) {
        		return true;
        	}
        }
        if(thirdValueString.equals("!=")) {
        	if(firstValue.compareTo(secondValue) != 0) {
        		return true;
        	}
        }
		System.out.println("Something went wrong ... maybe a wrong compare-value");
		return false;
	}
	//helper-methods for extracting data from the prova-files
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
