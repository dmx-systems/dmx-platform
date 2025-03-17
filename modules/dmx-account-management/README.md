
# DMX Account-Management

This plugin deals with user account-related tasks like creating users or changing passwords.

## Plugin Configuration

The following options must be configured via dmx-platform's `config.properties`:

```
dmx.accountmanagement.manager = DMX
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

Copyright (c) 2024-2025 DMX Systems
