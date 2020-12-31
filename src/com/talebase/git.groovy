#!/usr/bin/env groovy
package com.talebase

void checkoutBranch(project,branch) {
    color.green('>>>>>>>>>>>> Git Pull Code <<<<<<<<<<<<')

    try {      
        checkout([$class: 'GitSCM',
                branches: [[name: "${branch}"]],
                userRemoteConfigs: [[url: "http://gitlab.cepin.com:9999/root/${project}.git", credentialsId: "8f34b1a1-a614-416a-9290-a25fad4e863f"]]])
    } catch (e) {
        color.red('>>>>>>>>>>>> Git Pull fail <<<<<<<<<<<<')
        throw e
    }
}