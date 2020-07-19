package jp.openstandia.keycloak.authenticator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

import jp.openstandia.keycloak.authenticator.api.AuthyUserParams;
import jp.openstandia.keycloak.authenticator.api.AuthyUserService;
import jp.openstandia.keycloak.authenticator.api.SMSSendVerify;
import com.authy.AuthyApiClient;
import com.authy.AuthyException;
import com.authy.api.*;

public class SMSAuthenticator implements Authenticator {

	private static final Logger logger = Logger.getLogger(SMSAuthenticator.class.getPackage().getName());

	public void authenticate(AuthenticationFlowContext context) {
		logger.debug("Method [authenticate]");

		// Start Authy client & get users
		AuthyApiClient client = new AuthyApiClient(SMSAuthContstants.AUTHY_API_KEY);
		Users authyUsers = client.getUsers();

		AuthenticatorConfigModel config = context.getAuthenticatorConfig();

		UserModel user = context.getUser();
		String phoneNumber = getPhoneNumber(user);
		int authyId = -1;	// if -1 then it's not initialized
		String email = getEmail(user);
		logger.debugv("phoneNumber : {0}", phoneNumber);
		logger.debugv("email : {0}", email);


		if (phoneNumber != null) {

			// Create Authy user
			User authyUser;
			try {
				authyUser = authyUsers.createUser(email, phoneNumber, "966");
				if(authyUser.isOk()){
					// store authy id
					authyId = authyUser.getId();
				} else {
					System.out.println(authyUser.getError());
				}
			} catch (AuthyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// SendSMS
			try {
		        Map<String, String> options = new HashMap<String, String>();
		        options.put("force", "true");

		        Hash response = authyUsers.requestSms(authyId, options);

				System.out.println(response.getMessage());
				System.out.println(response.getToken());
				System.out.println(response.isSuccess());				
				if (response.isOk()) {
					Response challenge = context.form().createForm("sms-validation.ftl");
					context.challenge(challenge);
					System.out.println(response.getMessage());
				} else {
					Response challenge = context.form().addError(new FormMessage("sendSMSCodeErrorMessage"))
							.createForm("sms-validation-error.ftl");
					context.challenge(challenge);
					System.out.println(response.getError());
				}
			} catch (AuthyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// SMSSendVerify sendVerify = new SMSSendVerify(getConfigString(config, SMSAuthContstants.CONFIG_SMS_API_KEY),
			// 		getConfigString(config, SMSAuthContstants.CONFIG_PROXY_FLAG),
			// 		getConfigString(config, SMSAuthContstants.CONFIG_PROXY_URL),
			// 		getConfigString(config, SMSAuthContstants.CONFIG_PROXY_PORT),
			// 		getConfigString(config, SMSAuthContstants.CONFIG_CODE_LENGTH));

			// if (sendVerify.sendSMS(phoneNumber, authyId)) {
			// 	Response challenge = context.form().createForm("sms-validation.ftl");
			// 	context.challenge(challenge);

			// } else {
			// 	Response challenge = context.form().addError(new FormMessage("sendSMSCodeErrorMessage"))
			// 			.createForm("sms-validation-error.ftl");
			// 	context.challenge(challenge);
			// }

		} else {
			Response challenge = context.form().addError(new FormMessage("missingTelNumberMessage"))
					.createForm("sms-validation-error.ftl");
			context.challenge(challenge);
		}

	}

	public void action(AuthenticationFlowContext context) {
		logger.debug("Method [action]");

		MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
		String enteredCode = inputData.getFirst("smsCode");

		UserModel user = context.getUser();
		String phoneNumber = getPhoneNumber(user);
		logger.debugv("phoneNumber : {0}", phoneNumber);

		// SendSMS
		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		SMSSendVerify sendVerify = new SMSSendVerify(getConfigString(config, SMSAuthContstants.CONFIG_SMS_API_KEY),
				getConfigString(config, SMSAuthContstants.CONFIG_PROXY_FLAG),
				getConfigString(config, SMSAuthContstants.CONFIG_PROXY_URL),
				getConfigString(config, SMSAuthContstants.CONFIG_PROXY_PORT),
				getConfigString(config, SMSAuthContstants.CONFIG_CODE_LENGTH));

		if (sendVerify.verifySMS(phoneNumber, enteredCode)) {
			logger.info("verify code check : OK");
			context.success();

		} else {
			Response challenge = context.form()
					.setAttribute("username", context.getAuthenticationSession().getAuthenticatedUser().getUsername())
					.addError(new FormMessage("invalidSMSCodeMessage")).createForm("sms-validation-error.ftl");
			context.challenge(challenge);
		}

	}

	public boolean requiresUser() {
		logger.debug("Method [requiresUser]");
		return false;
	}

	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		logger.debug("Method [configuredFor]");
		return false;
	}

	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

	}

	public void close() {
		logger.debug("<<<<<<<<<<<<<<< SMSAuthenticator close");
	}

	private String getPhoneNumber(UserModel user) {
		List<String> phoneNumberList = user.getAttribute(SMSAuthContstants.ATTR_PHONE_NUMBER);
		if (phoneNumberList != null && !phoneNumberList.isEmpty()) {
			return phoneNumberList.get(0);
		}
		return null;
	}

	private String getEmail(UserModel user){
		List<String> emailList = user.getAttribute(SMSAuthContstants.ATTR_EMAIL);
		if (emailList != null && !emailList.isEmpty()) {
			return emailList.get(0);
		}
		return "user_email@email.com";
	}

	private String getConfigString(AuthenticatorConfigModel config, String configName) {
		String value = null;
		if (config.getConfig() != null) {
			// Get value
			value = config.getConfig().get(configName);
		}
		return value;
	}
}