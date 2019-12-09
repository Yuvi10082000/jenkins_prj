/**
 * Provides a wrapper to interact with the security configuration
 */
import jenkins from '../util/jenkins';

/**
 * Calls a stapler post method to save the first user settings
 */
function saveFirstUser($form, success, error) {
	jenkins.staplerPost(
		'/setupWizard/createAdminUser',
		$form,
		function(response) {
			var crumbRequestField = response.data.crumbRequestField;
			if (crumbRequestField) {
				require('window-handle').getWindow().crumb.init(crumbRequestField, response.data.crumb);
			}
			success(response);
		}, {
			error: error
		});
};

function saveConfigureInstance($form, success, error){
	jenkins.staplerPost(
		'/setupWizard/configureInstance',
		$form,
		function(response) {
			var crumbRequestField = response.data.crumbRequestField;
			if (crumbRequestField) {
				require('window-handle').getWindow().crumb.init(crumbRequestField, response.data.crumb);
			}
			success(response);
		}, {
			error: error
		});
};

/**
 * Calls a stapler post method to save the first user settings
 */
function saveProxy($form, success, error) {
	jenkins.staplerPost(
		'/pluginManager/proxyConfigure',
		$form,
		success, {
			dataType: 'html',
			error: error
		});
};

export default {
	saveFirstUser: saveFirstUser,
	saveConfigureInstance: saveConfigureInstance,
	saveProxy: saveProxy
}
