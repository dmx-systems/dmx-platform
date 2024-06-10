package systems.dmx.accountmanagement;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.impl.TopicModelImpl;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.ModelFactory;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;
import systems.dmx.core.util.JavaUtils;

import java.util.logging.Logger;

import static systems.dmx.accountmanagement.Constants.*;


final class DmxAccountManagementMethod implements AccountManagementMethod {

    private final static Logger logger = Logger.getLogger(DmxAccountManagementMethod.class.getName());

    static final String NAME = "DMX";

    private final CoreService dmx;

    private final ModelFactory mf;

    private final String siteSalt;

    DmxAccountManagementMethod(CoreService dmx, ModelFactory mf, String siteSalt) {
        this.dmx = dmx;
        this.mf = mf;
        this.siteSalt = siteSalt;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CheckCredentialsResult checkCredentials(Credentials credentials) {
        Topic usernameTopic = dmx.getPrivilegedAccess().checkCredentials(credentials);
        return (usernameTopic != null) ? CheckCredentialsResult.success(usernameTopic) : CheckCredentialsResult.failed();
    }

    @Override
    public void createAccount(Credentials cred) {
        PrivilegedAccess pa = dmx.getPrivilegedAccess();

        Topic usernameTopic = dmx.getPrivilegedAccess().getUsernameTopic(cred.username);

        // 1) create User Account
        // Note: a User Account topic (and its child topics) requires special workspace assignments (see next step 3).
        // So we suppress standard workspace assignment. (We can't set the actual workspace here as privileged
        // "assignToWorkspace" calls are required.)
        String salt = JavaUtils.random256();
        try {
            Topic userAccount = pa.runInWorkspaceContext(-1, () ->
                    dmx.createTopic(mf.newTopicModel(USER_ACCOUNT, mf.newChildTopicsModel()
                            .setRef(USERNAME, usernameTopic.getId())
                            .set(PASSWORD, JavaUtils.encodeSHA256(siteSalt + salt + cred.password))))
            );
            logger.info("### Salting password of user \"" + cred.username + "\"");
            RelatedTopic passwordTopic = userAccount.getChildTopics().getTopic(PASSWORD);
            passwordTopic.setProperty(SALT, salt, false);     // addToIndex=false
            // 2) assign user account and password to private workspace
            // Note: the current user has no READ access to the private workspace just created.
            // Privileged assignToWorkspace() calls are required (instead of using the Workspaces service).
            long privateWorkspaceId = pa.getPrivateWorkspace(cred.username).getId();
            pa.assignToWorkspace(userAccount, privateWorkspaceId);
            pa.assignToWorkspace(passwordTopic, privateWorkspaceId);
            pa.assignToWorkspace(passwordTopic.getRelatingAssoc(), privateWorkspaceId);
        } catch (Exception e) {
            throw new RuntimeException(String.format("DMX account creation for user %s failed", cred.username), e);
        }
    }

    @Override
    public void changePassword(Credentials currentCred, Credentials newCred) {
        // TODO: Everything related to internal DMX-based password handling is currently to be found in PrivilegedAccess
        dmx.getPrivilegedAccess().changePassword(newCred);
    }

    @Override
    public void onUsernameDeleted(String username) {
        // TODO: Shall remove user account, password and potentially email topic
    }

}
