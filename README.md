# Protecode SC Jenkins Plugin

This plugin allows Jenkins builds to scan the resulting build artifacts
with Synopsys Protecode SC static analysis tool.

More information of Synopsys Protecode SC is available from
http://www.synopsys.com/software/protecode/Pages/default.aspx


## Build instructions

The plugin works with Jenkins 2.83 or newer and it is written to be Java 1.8
compatible.

The plugin is built with Maven 3.x. The command to create the package to be
deployed, simply issue

```
mvn package
```

This will compile, test and package the plugin into an Jenkins plugin installation package to `target/protecode-sc-plugin.hpi`.


## Installation

1. Install dependencies
    - [Credentials plugin](https://wiki.jenkins-ci.org/display/JENKINS/Credentials+Plugin) for storing Protecode SC credentials    
    - [Summary Display plugin](https://wiki.jenkins-ci.org/display/JENKINS/Summary+Display+Plugin) for rendering reports from Protecode SC scans
    (Copy Artifact Plugin IS NOT NEEDED anymore.)
2. Upload and install Protecode SC Plugin `protecode-sc-plugin.hpi`
3. Restart Jenkins


### Jenkins configuration

Configure system wide Protecode SC server address.

- Manage Jenkins > Configure System
    - **Protecode SC: Server address**
        - Specify the HTTP address of the Protecode SC installation here, such as https://protecode-sc.mydomain.com/.
    - **Protecode SC: Disable certificate validation**
        - Allow connections to Protecode SC server without certificate validation. It is not recommended to use this option. Instead you should consider getting a valid certificate for your server.

### Build configuration

Configure build with the following post build actions.
Order of the post build actions is important; first archive artifacts to be scanned, then scan artifacts with Protecode SC and finally generate a report using the returned results.

1. Archive the artifacts
    - *Files to archive*
        - Set to `*`
2. Protecode SC
    - *Credentials*
        - (new) Add
            - Select "Global" and "Username with password"
            - Enter your user Protecode SC user details
        - (existing) Select suitable credentials
    - *Group ID*
        - Specify the Protecode SC Group ID where the artifacts should be uploaded to. Group ID can be found from the Protecode SC service by looking at the URL when browsing an individual group: https://protecode-sc.mydomain.com/group/1234/ or with Groups API https://protecode-sc.mydomain.com/api/groups/.
    - *Fail build if vulnerabilities*
        - Trigger build failure if Protecode SC finds vulnerabilities from the artifacts.
    - *Additional artifact directory*
        - You can use Copy Artifact Plugin to copy artifacts from another job. The directory specified here must match the directory specifed in Copy artifact plugin. If this job generates artifacts, they are always included in the scan.
    - *Keep copied artifacts after build*
        - Check this if you want to keep artifacts that are copied using Copy Artifact plugin. Note that if artifacts are not overwritten during copy phase, they accumulate and the same artifacts are scanned again in subsequent runs.
    - *Convert results to Summary plugin format*
        - Set to `true`
        - The summary can be shown using Summary Display Plugin of Jenkins.
      The report file name to publish is protecodesc.xml.
3. Publish XML Summary Reports
    - *Files to parse*
        - Set to `**/protecodesc.xml`
    - *Show on Project page*
        - Set to `true`


## License

All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/epl-v10.html
