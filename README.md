# Protecode SC Jenkins Plugin

This plugin allows Jenkins builds to scan the resulting build artifacts
with Synopsys Protecode SC static analysis tool.

More information of Synopsys Protecode SC is available from
http://www.synopsys.com/software/protecode/Pages/default.aspx


## Build instructions

The plugin works with Jenkins 2.60.1 or newer and it is written to be Java 1.8
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
    - Optional: [Summary Display plugin](https://wiki.jenkins-ci.org/display/JENKINS/Summary+Display+Plugin) for rendering reports from Protecode SC scans    
2. Upload and install Protecode SC Plugin `protecode-sc-plugin.hpi`
3. Restart Jenkins


### Jenkins configuration

See https://plugins.jenkins.io/protecode-sc

## Possible plugins to use with Protecode SC Jenkins plugin

* https://plugins.jenkins.io/confluence-publisher
* https://plugins.jenkins.io/mock-slave
* https://plugins.jenkins.io/http-post
* https://plugins.jenkins.io/http_request
* https://plugins.jenkins.io/publish-over-ssh
* https://plugins.jenkins.io/hudson-pview-plugin
* https://plugins.jenkins.io/device-watcher
* https://plugins.jenkins.io/hipchat
* https://plugins.jenkins.io/coverity
* https://plugins.jenkins.io/disk-usage
* https://plugins.jenkins.io/findbugs
* https://plugins.jenkins.io/robot
* https://plugins.jenkins.io/copyartifact
* https://plugins.jenkins.io/text-file-operations
* https://plugins.jenkins.io/build-timeout
* https://plugins.jenkins.io/exclusive-execution
* https://plugins.jenkins.io/versionnumber
* https://plugins.jenkins.io/files-found-trigger
* https://plugins.jenkins.io/artifact-diff-plugin
* https://plugins.jenkins.io/blackduck-detect
* https://plugins.jenkins.io/blackduck-hub
* https://plugins.jenkins.io/downstream-buildview
* https://plugins.jenkins.io/project-inheritance
* https://plugins.jenkins.io/build-monitor-plugin
* https://plugins.jenkins.io/htmlresource	

## License

All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/epl-v10.html
