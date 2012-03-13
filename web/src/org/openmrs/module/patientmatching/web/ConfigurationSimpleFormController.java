package org.openmrs.module.patientmatching.web;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientmatching.MatchingConfigurationUtils;
import org.openmrs.module.patientmatching.MatchingConstants;
import org.openmrs.module.patientmatching.PatientMatchingConfiguration;
import org.openmrs.web.WebConstants;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class ConfigurationSimpleFormController extends SimpleFormController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

	@Override
	protected PatientMatchingConfiguration formBackingObject(HttpServletRequest request) throws Exception {
	    String name = request.getParameter(MatchingConstants.PARAM_NAME);
	    
        AdministrationService adminService = Context.getAdministrationService();
        String excludedProperties = adminService.getGlobalProperty(MatchingConstants.CONFIG_EXCLUDE_PROPERTIES);
        List<String> listExcludedProperties = Arrays.asList(excludedProperties.split(",", -1));
        log.info("Excluded Property: " + excludedProperties);
        
        PatientMatchingConfiguration configuration = null;
        
	    if (name != null) {
	    	configuration = MatchingConfigurationUtils.loadPatientMatchingConfig_db(name, listExcludedProperties);
	    } else {
	        configuration = MatchingConfigurationUtils.createPatientMatchingConfig(listExcludedProperties);
	    }
	    log.info("Config Name: " + configuration.toString());
		return configuration;
		
	}

    /**
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception {
    	
    	HttpSession httpSession = request.getSession();
        PatientMatchingConfiguration patientMatchingConfig = (PatientMatchingConfiguration) command;
        
        Map<String, String> model = new HashMap<String, String>();
        
        /*
        for(ConfigurationEntry configEntry: patientMatchingConfig.getConfigurationEntries()) {
            log.info("response -- " + patientMatchingConfig.getConfigurationName() + ": " + configEntry.getFieldName() + ": " + configEntry.getInclusion() + "(blocking=" + new Boolean(configEntry.getInclusion() == "BLOCKING").toString() + ")");
        }
        */
        
        if(request.getParameter("edit") != null){
        	MatchingConfigurationUtils.savePatientMatchingConfig(patientMatchingConfig);
        	httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "patientmatching.strategy.update");
        }
        else{
        List<String> pmcNames = MatchingConfigurationUtils.listAvailableMatchingConfigs_db();
        if(pmcNames.contains(patientMatchingConfig.getConfigurationName())){
        	log.error("Unable to save Strategy "+ patientMatchingConfig.getConfigurationName()+" as Strategy Name is a duplicate");
			httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "patientmatching.strategy.save");
			return showForm(request, response, errors);
        }
        MatchingConfigurationUtils.savePatientMatchingConfig(patientMatchingConfig);
        }
        return new ModelAndView(getSuccessView(), model);
    }


   
	
}
