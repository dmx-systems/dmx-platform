package systems.dmx.accountmanagement;

import systems.dmx.accountmanagement.configuration.ExpectedPasswordComplexity;

/**
 * Static configuration values from DMX' config.properties.
 */
final class DmxAccountManagementConfigOptions {

    private DmxAccountManagementConfigOptions() {
    }

    // --- Global config options
    static final ExpectedPasswordComplexity CONFIG_EXPECTED_PASSWORD_COMPLEXITY = ExpectedPasswordComplexity.fromStringOrComplex(System.getProperty(Constants.EXPECTED_PASSWORD_COMPLEXITY));

    static final int CONFIG_EXPECTED_MIN_PASSWORD_LENGTH = Integer.parseInt(System.getProperty(Constants.EXPECTED_MIN_PASSWORD_LENGTH, "8"));

    static final int CONFIG_EXPECTED_MAX_PASSWORD_LENGTH = Integer.parseInt(System.getProperty(Constants.EXPECTED_MAX_PASSWORD_LENGTH, "64"));
    static final String ACCOUNT_MANAGER_NAME = System.getProperty(Constants.ACCOUNT_MANAGER_NAME,
            DmxAccountManager.NAME);

    static final boolean NEW_ACCOUNTS_ARE_ENABLED = Boolean.parseBoolean(
            System.getProperty("dmx.security.new_accounts_are_enabled", "true")
    );

}
