package org.provarules.reference2.builtins;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.query.Binding;
import org.hl7.fhir.r4.model.Observation;
import org.provarules.agent2.ProvaReagent;
import org.provarules.kernel2.ProvaConstant;
import org.provarules.kernel2.ProvaDerivationNode;
import org.provarules.kernel2.ProvaGoal;
import org.provarules.kernel2.ProvaKnowledgeBase;
import org.provarules.kernel2.ProvaList;
import org.provarules.kernel2.ProvaLiteral;
import org.provarules.kernel2.ProvaObject;
import org.provarules.kernel2.ProvaPredicate;
import org.provarules.kernel2.ProvaRule;
import org.provarules.kernel2.ProvaVariable;
import org.provarules.kernel2.ProvaVariablePtr;
import org.provarules.reference2.ProvaConstantImpl;
import org.provarules.reference2.ProvaListImpl;
import org.provarules.reference2.ProvaLiteralImpl;
import org.provarules.reference2.ProvaRuleImpl;

import ca.uhn.fhir.context.FhirContext;

public class FhirExtractValueFromObservationImpl extends ProvaBuiltinImpl {

	private static AtomicInteger nqid = new AtomicInteger(0);

	public FhirExtractValueFromObservationImpl(ProvaKnowledgeBase kb) {
		super(kb, "fhirExtractValueFromObservation");
	}

	@Override
	public boolean process(ProvaReagent prova, ProvaDerivationNode node, ProvaGoal goal, List<ProvaLiteral> newLiterals,
			ProvaRule query) {
		System.out.println("processing the FhirExtractValueFromObservation");

		List<ProvaVariable> variables = query.getVariables();
		ProvaLiteral literal = goal.getGoal();
		ProvaList terms = literal.getTerms();
		ProvaObject[] data = terms.getFixed();

		if (data.length != 3) {
			System.out.println("Syntax error - need three terms");
			System.out.println("I found entries: " +  data.length);
			return false;
		}
		// assigning the data to a FHIRObservationObject
		Observation obsToPrint = (Observation) extractValue(data, variables, 0);
		BigDecimal obsValueBigDecimal = obsToPrint.getValueQuantity().getValue();
		ProvaObject data2 = resolve(data[1], variables);
		String extractedCoding = (String) extractValue(data,variables,2);
		//Exit the process if configured coding and extractedCoding from observation does not match
		boolean foundCoding = false;
		for(int i = 0;i<obsToPrint.getCode().getCoding().size();i++) {
			System.out.println("Code from Observation: " + obsToPrint.getCode().getCoding().get(0).getCode());
			System.out.println("Code from Prova: " + extractedCoding);
			if(obsToPrint.getCode().getCoding().get(i).getCode().equalsIgnoreCase(extractedCoding)) {
				System.out.println("Found a matching entry; continue");
				foundCoding = true;
				continue;
			}else {
				System.out.println("Found NO matching entry; loop next round");
				foundCoding = false;
			}
		}
		if(foundCoding == false) {
			System.out.println("Found no matching code in the FHIR Observation - returning False - nothing to do for me here...");
			return false;
		}
		
		//Continue by adding the entry to the knowledgebase
		String qid = null;

		qid = Integer.toString(nqid.incrementAndGet());
		((ProvaVariable) data2).setAssigned(ProvaConstantImpl.create(qid));

		List<ProvaObject> newEntry = new ArrayList<>();
		newEntry.add(ProvaConstantImpl.create(obsValueBigDecimal));

		ProvaConstant cqid = ProvaConstantImpl.create(qid);
		try {
			ProvaPredicate pred = kb.getOrGeneratePredicate("fhir_results", newEntry.size() + 1);
			addFact(pred, cqid, newEntry);
		} catch (RuntimeException e) {
		} catch (Exception e) {
		}
		return true;
	}

	// helper-methods for extracting data from the prova-files
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
			System.out.println("Exception: " + cce);
			return null;
		}
		return retval;
	}

	static private ProvaObject resolve(ProvaObject o, List<ProvaVariable> variables) {
		if (o instanceof ProvaVariablePtr) {
			ProvaVariablePtr varPtr = (ProvaVariablePtr) o;
			o = variables.get(varPtr.getIndex()).getRecursivelyAssigned();
		}
		System.out.println("Resolved from PROVA: " + o.toString());
		return o;
	}

	// Adding something to the Knowledgebase
	static void addFact(ProvaPredicate pred, ProvaConstant cqid, List<ProvaObject> terms) {
		terms.add(0, cqid);
		ProvaList ls = ProvaListImpl.create(terms);
		ProvaLiteral lit = new ProvaLiteralImpl(pred, ls);
		ProvaRule clause = ProvaRuleImpl.createVirtualRule(1, lit, null);
		pred.addClause(clause);
	}
}
