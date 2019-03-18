package org.ironrhino.core.security.action;

import static org.ironrhino.core.security.action.LoginAction.DEFAULT_VALUE_LOGIN_DEFAULT_TARGET_URL;
import static org.ironrhino.core.security.action.LoginAction.KEY_LOGIN_DEFAULT_TARGET_URL;

import java.util.Collections;
import java.util.List;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.security.event.PasswordChangedEvent;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.session.HttpSessionManager;
import org.ironrhino.core.spring.security.VerificationCodeChecker;
import org.ironrhino.core.spring.security.WrongVerificationCodeException;
import org.ironrhino.core.spring.security.password.PasswordMutator;
import org.ironrhino.core.spring.security.password.PasswordStrengthChecker;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.validator.annotations.ExpressionValidator;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Authorize(ifNotGranted = UserRole.ROLE_BUILTIN_ANONYMOUS)
public class PasswordAction extends BaseAction {

	private static final long serialVersionUID = 5706895776974935953L;

	@Getter
	@Setter
	private String password;

	@Getter
	@Setter
	private String currentPassword;

	@Getter
	@Setter
	private String confirmPassword;

	@Getter
	@Setter
	private String verificationCode;

	@Getter
	@Value("${user.password.readonly:false}")
	private boolean userPasswordReadonly;

	@Getter
	@Value("${user.password.currentPasswordNeeded:true}")
	private boolean userCurrentPasswordNeeded;

	@Value("${" + KEY_LOGIN_DEFAULT_TARGET_URL + ":" + DEFAULT_VALUE_LOGIN_DEFAULT_TARGET_URL + "}")
	protected String defaultTargetUrl;

	@Autowired(required = false)
	private PasswordStrengthChecker passwordStrengthChecker;

	@Autowired(required = false)
	private List<PasswordMutator<?>> passwordMutators;

	@Autowired(required = false)
	@Getter
	protected VerificationManager verificationManager;

	@Autowired(required = false)
	private List<VerificationCodeChecker> verificationCodeCheckers = Collections.emptyList();

	@Autowired
	protected EventPublisher eventPublisher;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@InputConfig(resultName = SUCCESS)
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, trim = true, fieldName = "password", key = "validation.required") }, expressions = {
					@ExpressionValidator(expression = "password == confirmPassword", key = "validation.repeat.not.matched") })
	public String execute() {
		if (passwordMutators == null || userPasswordReadonly) {
			addActionError(getText("access.denied"));
			return ACCESSDENIED;
		}
		UserDetails user = AuthzUtils.getUserDetails();
		if (!verificationCodeCheckers.isEmpty()) {
			WrongVerificationCodeException ex = null;
			for (VerificationCodeChecker checker : verificationCodeCheckers) {
				try {
					checker.verify(user, null, verificationCode);
					ex = null;
					break;
				} catch (WrongVerificationCodeException e) {
					ex = e;
				}
			}
			if (ex != null) {
				addFieldError("verificationCode", getText(WrongVerificationCodeException.class.getName()));
				return SUCCESS;
			}
		}
		if (passwordStrengthChecker != null) {
			try {
				passwordStrengthChecker.check(user, password);
			} catch (Exception e) {
				addFieldError("password", getText(e.getLocalizedMessage()));
				return SUCCESS;
			}
		}
		if (isUserCurrentPasswordNeeded()) {
			boolean valid = currentPassword != null && AuthzUtils.isPasswordValid(currentPassword);
			if (!valid) {
				addFieldError("currentPassword", getText("currentPassword.error"));
				return SUCCESS;
			}
		}
		boolean passwordExpired = !user.isCredentialsNonExpired();
		PasswordMutator passwordMutator = null;
		for (PasswordMutator<?> pm : passwordMutators) {
			if (pm.accepts(user.getUsername())) {
				passwordMutator = pm;
				break;
			}
		}
		if (passwordMutator == null) {
			addActionError(getText("access.denied"));
			return ACCESSDENIED;
		}
		passwordMutator.changePassword(user, password);
		notify("save.success");
		eventPublisher.publish(
				new PasswordChangedEvent(user.getUsername(), ServletActionContext.getRequest().getRemoteAddr()),
				Scope.LOCAL);
		ServletActionContext.getRequest().setAttribute(HttpSessionManager.REQUEST_ATTRIBUTE_SESSION_MARK_AS_DIRTY,
				true);
		if (passwordExpired) {
			targetUrl = defaultTargetUrl;
			return REDIRECT;
		}
		return SUCCESS;
	}

}
