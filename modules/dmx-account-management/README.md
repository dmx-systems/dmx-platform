
# DMX Account-Management

This plugin deals with user account-related tasks like creating users or changing passwords.

## Plugin Configuration

The following options must be configured via dmx-platform's `config.properties`:

```
dmx.accountmanagement.manager = DMX
dmx.accountmanagement.expected_password_complexity = complex
dmx.accountmanagement.expected_min_password_length = 8
dmx.accountmanagement.expected_max_password_length = 64
```

### Configure account manager
An account manager is an extension to the account management DMX plugin which usually adds support for storing and
checking user credentials in some 3rd party system (eg. a dedicated database). Each account manager has an identifying
name attached. This name can be used in the property `dmx.accountmanagement.manager` to select an account manager which
is responsible for creating new user by default.

This is only necessary when there is more than one account manager present. If no external account managers have been
provided there is always an account manager with the name "DMX". This is also the default value for this property.

Keep in mind that another mechanism in the account creation process optionally allows the developer to select an
account manager beforehand. If that mechanism is used, the configured account manager has no effect. In other word,
the configured account manager is only used when during the account creation, no account manager was selected
explicitly.

### Configuring expected password complexity
It is possible to select the expected password complexity by setting the property:
```
dmx.accountmanagement.expected_password_complexity = complex
```

The possible values are "complex", "simple" or "none" with "complex" being the default if no value was set or the value
having been mistyped.

The complex password rules are:
* minimum and maximum characters according to separate settings (see next section)
* at least one lower-case letter
* at least on upper-case letter
* at least one digit
* at least one special character
* no whitespace
* no simple sequences (eg. 123456, qwertz, ABCDEF etc.)

The simple password rules are:
* minimum and maximum characters according to separate settings (see next section)
* no whitespace

The password rules are enforced on account creation and password change.

### Configuring expected password length
When the expected password complexity is "complex" you can configure the expected password lengths as follows:

```
dmx.accountmanagement.expected_min_password_length = 8
dmx.accountmanagement.expected_max_password_length = 64
```

The values should be positive and the max value higher or equal than the min value. If no value is specified the
defaults are a minimum of 8 characters and a maximum of 64 characters.

Copyright (c) 2024-2024 DMX Systems
